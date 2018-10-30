package org.apache.olingo.jpa.processor.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter.ParameterKind;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.query.AbstractObjectConverter;
import org.apache.olingo.jpa.processor.core.query.JPAEntityConverter;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAEntityHelper {

	private final Logger log = Logger.getLogger(JPAEntityHelper.class.getName());
	private final EntityManager em;
	private final IntermediateServiceDocument sd;
	private final ServiceMetadata serviceMetadata;
	private final UriHelper uriHelper;
	private final DependencyInjector dependencyInjector;

	public JPAEntityHelper(final EntityManager em, final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata,
			final UriHelper uriHelper, final DependencyInjector dependencyInjector) {
		this.em = em;
		this.sd = sd;
		this.serviceMetadata = serviceMetadata;
		this.uriHelper = uriHelper;
		this.dependencyInjector = dependencyInjector;
	}

	@SuppressWarnings("unchecked")
	public final <R> R invokeUnboundActionMethod(final JPAAction jpaAction, final Map<String, Parameter> parameters) throws ODataException {
		final Object[] args = buildArguments(jpaAction, parameters);
		final IntermediateAction iA = (IntermediateAction) jpaAction;
		final Method javaMethod = iA.getJavaMethod();
		try {
			final Object result = javaMethod.invoke(null, args);
			if (result == null || iA.getResultParameter() == null) {
				return null;
			}
			return (R) result;
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
	 * @see #loadJPAEntity(JPAEntityType, Entity)
	 */
	@SuppressWarnings("unchecked")
	public final <R> R invokeBoundActionMethod(final JPAEntityType jpaType, final Entity oDataEntity, final JPAAction jpaAction,
			final Map<String, Parameter> parameters) throws ODataException {
		final Object jpaEntity = loadJPAEntity(jpaType, oDataEntity);
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
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ODataJPAModelException(e);
		}
	}

	private Object[] buildArguments(final JPAAction jpaAction, final Map<String, Parameter> odataParameterValues) throws ODataException {

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
				continue;
			}
			switch (p.getValueType()) {
			case PRIMITIVE:
				args[i] = p.getValue();
				break;
			case ENUM:
				final AbstractObjectConverter converter = new AbstractObjectConverter(null, uriHelper, sd,
						serviceMetadata) {
					@Override
					protected Collection<? extends Link> createExpand(final Tuple row, final URI uri)
							throws ODataApplicationException {
						return null;
					}
				};
				args[i] = converter.convertOData2JPAPropertyValue(jpaParameter, p);
				break;
			case COLLECTION_PRIMITIVE:
				args[i] = p.asCollection();
				break;
			case COLLECTION_ENUM:
				args[i] = p.asCollection();
				break;
			case ENTITY:
				final Entity entity = p.asEntity();
				final EntityType<?> persistenceType = em.getMetamodel().entity(jpaParameter.getType());

				final JPAEntityConverter entityConverter = new JPAEntityConverter(persistenceType, uriHelper, sd, serviceMetadata,
						em.getMetamodel());
				final Object jpaEntity = entityConverter.convertOData2JPAEntity(entity);
				args[i] = jpaEntity;
				break;
			default:
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED, p.getValueType().toString(),
						p.getName());
			}
		}
		return args;
	}

	/**
	 * Load/search a JPA entity based on the identifier taken from the given OData
	 * entity. The JPA entity will assigned to the current {@link EntityManager
	 * entity manager}.
	 *
	 * @param oDataEntity
	 *            The OData entity used to identify the corresponding JPA
	 *            entity.
	 * @return A instance of one of the {@link Metamodel#getEntities() managed
	 *         types}, loaded based on the given OData entity.
	 * @throws ODataJPAModelException
	 */
	@SuppressWarnings("unchecked")
	public final <O> O loadJPAEntity(final JPAEntityType jpaType, final Entity oDataEntity) throws ODataJPAModelException {
		final List<Object> listPrimaryKeyValues = new LinkedList<>();
		// TODO risk: the order of (primary) key/id attributes must be the same as in descriptor; that is not ensured
		for (final JPAAttribute jpaAttribute : jpaType.getKeyAttributes()) {
			if (jpaAttribute.isComplex() || jpaAttribute.isAssociation()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
			}
			final Object value = oDataEntity.getProperty(jpaAttribute.getExternalName()).getValue();
			listPrimaryKeyValues.add(value);
		}
		if (listPrimaryKeyValues.isEmpty()) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
		}
		return em.find((Class<O>) jpaType.getTypeClass(), listPrimaryKeyValues, LockModeType.NONE);
	}

}
