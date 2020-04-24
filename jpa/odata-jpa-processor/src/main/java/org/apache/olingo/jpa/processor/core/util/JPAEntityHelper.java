package org.apache.olingo.jpa.processor.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter.ParameterKind;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.DependencyInjector;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.EntityConverter;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriInfoResource;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAEntityHelper {

  private final Logger log = Logger.getLogger(JPAEntityHelper.class.getName());
  private final EntityManager em;
  private final IntermediateServiceDocument sd;
  private final JPAODataRequestContext context;
  private final EntityConverter entityConverter;

  public JPAEntityHelper(final EntityManager em, final IntermediateServiceDocument sd, final UriInfoResource uriInfo,
      final UriHelper uriHelper, final JPAODataRequestContext context) throws ODataJPAModelException {
    this.em = em;
    this.sd = sd;
    this.context = context;
    entityConverter = new EntityConverter(uriHelper, sd, context.getServiceMetaData());
  }

  @SuppressWarnings("unchecked")
  public final <R> R invokeUnboundActionMethod(final JPAAction jpaAction, final Map<String, Parameter> parameters)
      throws ODataJPAModelException, ODataApplicationException {
    final Object[] args = buildArguments(jpaAction, parameters);
    final IntermediateAction iA = (IntermediateAction) jpaAction;
    final Method javaMethod = iA.getJavaMethod();
    try {
      final Object result = javaMethod.invoke(null, args);
      if (result == null || iA.getResultParameter() == null) {
        return null;
      }
      return (R) result;
    } catch (final InvocationTargetException e) {
      if (ODataApplicationException.class.isInstance(e.getTargetException())) {
        log.log(Level.FINE, "Action call throws " + ODataApplicationException.class.getSimpleName()
            + "... unrwap to send custom error status", e);
        throw ODataApplicationException.class.cast(e.getTargetException());
      }
      // otherwise
      throw new ODataJPAModelException(e);
    } catch (final Exception e) {
      throw new ODataJPAModelException(e);
    }
  }

  /**
   * Invoke the corresponding JAVA method on the instance loaded based on given
   * <i>jpaType</i>.
   *
   * @return The result or <code>null</code> if no result parameter is defined or
   *         value self is <code>null</code>
   * @see #lookupJPAEntity(JPAEntityType, Entity)
   */
  @SuppressWarnings("unchecked")
  public final <R> R invokeBoundActionMethod(final JPAStructuredType jpaType, final Entity oDataEntity,
      final JPAAction jpaAction,
      final Map<String, Parameter> parameters) throws ODataJPAModelException, ODataJPAConversionException, ODataApplicationException {
    final Object jpaEntity = lookupJPAEntity(jpaType, oDataEntity);
    if (jpaEntity == null) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL);
    }
    final Object[] args = buildArguments(jpaAction, parameters);
    final IntermediateAction iA = (IntermediateAction) jpaAction;
    final Method javaMethod = iA.getJavaMethod();
    try {
      final Object result = javaMethod.invoke(jpaEntity, args);
      if (result == null || iA.getResultParameter() == null) {
        return null;
      }
      return (R) result;
    } catch (final InvocationTargetException e) {
      log.log(Level.FINE, "Invocation of bound action '" + jpaAction.getExternalName()
      + "' failed (will rethrow target exception): " + e.getMessage());
      if (ODataApplicationException.class.isInstance(e.getTargetException())) {
        throw ODataApplicationException.class.cast(e.getTargetException());
      } else {
        throw new ODataJPAModelException(e);
      }
    } catch (IllegalAccessException | IllegalArgumentException e) {
      throw new ODataJPAModelException(e);
    }
  }

  private Object[] buildArguments(final JPAAction jpaAction, final Map<String, Parameter> odataParameterValues)
      throws ODataJPAModelException, ODataJPAConversionException {

    final DependencyInjector dependencyInjector = context.getDependencyInjector();
    final List<JPAOperationParameter> actionParameters = jpaAction.getParameters();
    final Object[] args = new Object[actionParameters.size()];
    for (int i = 0; i < actionParameters.size(); i++) {
      args[i] = null;
      // fill Backend (inject) parameters
      final JPAOperationParameter jpaParameter = actionParameters.get(i);
      if (jpaParameter.getParameterKind() == ParameterKind.Inject) {
        final Object value = dependencyInjector.getDependencyValue(jpaParameter.getType());
        if (value == null) {
          log.warning(
              "Cannot inject value for method parameter " + jpaParameter.getName() + " of type " + jpaParameter.getType());
        }
        args[i] = value;
        continue;
      }
      // fill OData parameters
      final Parameter p = odataParameterValues.get(jpaParameter.getName());
      if (p == null) {
        if (!jpaParameter.isNullable()) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER, jpaParameter.getName());
        }
        continue;
      }
      switch (p.getValueType()) {
      case PRIMITIVE:
        args[i] = p.getValue();
        break;
      case ENUM:
        args[i] = entityConverter.convertOData2JPAPropertyValue(jpaParameter, p);
        break;
      case COLLECTION_PRIMITIVE:
        args[i] = p.asCollection();
        break;
      case COLLECTION_ENUM:
        args[i] = p.asCollection();
        break;
      case ENTITY:
        final JPAEntityType jpaTypeS = sd.getEntityType(jpaParameter.getTypeFQN());
        final Entity odataEntityS = p.asEntity();
        args[i] = entityConverter.convertOData2JPAEntity(odataEntityS, jpaTypeS);
        break;
      case COLLECTION_ENTITY:
        final JPAEntityType jpaTypeCE = sd.getEntityType(jpaParameter.getTypeFQN());
        final EntityCollection odataEntities = (EntityCollection) p.getValue();
        final List<Object> jpaEntities = new ArrayList<>(odataEntities.getEntities().size());
        for (final Entity odataEntityCE : odataEntities) {
          final Object jpaEntity = entityConverter.convertOData2JPAEntity(odataEntityCE, jpaTypeCE);
          jpaEntities.add(jpaEntity);
        }
        args[i] = jpaEntities;
        break;
      default:
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED, p.getValueType().toString(),
            p.getName());
      }
    }
    return args;
  }

  /**
   * Lookup (load/search) for a JPA entity based on the identifier taken from the given OData
   * entity. The JPA entity will assigned to the current {@link EntityManager
   * entity manager}.
   *
   * @param oDataEntity The OData entity used to identify the corresponding JPA entity.
   * @return A instance of one of the {@link Metamodel#getEntities() managed types}, loaded based on the given OData
   * entity.
   * @throws ODataJPAModelException For any nested exception
   * @see javax.persistence.EntityManager#find(Class, Object)
   */
  @SuppressWarnings("unchecked")
  public final <O> O lookupJPAEntity(final JPAStructuredType jpaType, final Entity oDataEntity)
      throws ODataJPAModelException {
    final List<Object> listPrimaryKeyValues = new LinkedList<>();
    try {
      for (final JPAAttribute<?> jpaAttribute : jpaType.getKeyAttributes(false)) {
        if (jpaAttribute.isComplex() && !listPrimaryKeyValues.isEmpty()) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
        }
        if (jpaAttribute.isAssociation()) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ASSOCIATION);
        }
        final Object value = entityConverter.transferOData2JPAProperty(null, jpaAttribute, oDataEntity.getProperties());
        if (value == null) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER);
        }
        listPrimaryKeyValues.add(value);
      }
    } catch (final IllegalAccessException | NoSuchFieldException | ODataApplicationException ex) {
      throw new ODataJPAModelException(ex);
    }
    if (listPrimaryKeyValues.isEmpty()) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
    }
    if (listPrimaryKeyValues.size() == 1) {
      return em.find((Class<O>) jpaType.getTypeClass(), listPrimaryKeyValues.get(0), LockModeType.NONE);
    } else {
      log.warning(jpaType.getInternalName()
          + " has multiple id properties, this is supported only by a few JPA providers and not JPA compliant! Use @EmbeddedId or @IdClass instead.");
      return em.find((Class<O>) jpaType.getTypeClass(), listPrimaryKeyValues, LockModeType.NONE);
    }
  }

}
