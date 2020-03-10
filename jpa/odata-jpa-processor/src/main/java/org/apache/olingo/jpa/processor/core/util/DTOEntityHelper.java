package org.apache.olingo.jpa.processor.core.util;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTOHandler;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.api.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.EntityConverter;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class DTOEntityHelper {

  private final Logger log = Logger.getLogger(DTOEntityHelper.class.getName());

  private final JPAEdmProvider provider;
  private final JPAODataGlobalContext context;
  private final UriInfoResource uriInfo;

  public DTOEntityHelper(final JPAODataGlobalContext context, final UriInfoResource uriInfo) {
    this.context = context;
    this.provider = context.getEdmProvider();
    this.uriInfo = uriInfo;
  }

  private Class<? extends ODataDTOHandler<?>> determineDTOHandlerClass(final EdmEntitySet targetEdmEntitySet)
      throws ODataJPAModelException {
    final ODataDTO dtoAnnotation = determineODataDTOAnnotation(targetEdmEntitySet);
    if (dtoAnnotation == null) {
      return null;
    }
    final Class<? extends ODataDTOHandler<?>> handler = dtoAnnotation.handler();
    if (handler == null || ODataDTO.DEFAULT.class.equals(handler)) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
          "ODataDTOHandler not defined");
    }
    return handler;
  }

  private ODataDTO determineODataDTOAnnotation(final EdmEntitySet targetEdmEntitySet) throws ODataJPAModelException {
    final JPAEntityType jpaEntityType = provider.getServiceDocument()
        .getEntityType(targetEdmEntitySet.getName());
    if (jpaEntityType == null) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE);
    }
    final Class<?> javaType = jpaEntityType.getTypeClass();
    if (javaType == null) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
          "Java type not available");
    }
    return javaType.getAnnotation(ODataDTO.class);
  }

  public boolean isTargetingDTO(final Class<?> clazzPossibleDTO) {
    return clazzPossibleDTO.getAnnotation(ODataDTO.class) != null;
  }

  public boolean isTargetingDTO(final EdmEntitySet targetEdmEntitySet) {
    try {
      return (determineODataDTOAnnotation(targetEdmEntitySet) != null);
    } catch (final ODataJPAModelException e) {
      log.log(Level.SEVERE, "Couldn't get informations about DTO state of " + targetEdmEntitySet.getName(), e);
      return false;
    }
  }

  /**
   *
   * @return TRUE if <i>targetEdmEntitySet</i> is {@link ODataDTO DTO} and has a handler taken from {@link ODataDTO#handler() @ODataDTO}.
   */
  public boolean isTargetingDTOWithHandler(final EdmEntitySet targetEdmEntitySet) {
    try {
      return (determineDTOHandlerClass(targetEdmEntitySet) != null);
    } catch (final ODataJPAModelException e) {
      log.log(Level.SEVERE, "Couldn't get informations about DTO state of " + targetEdmEntitySet.getName(), e);
      return false;
    }
  }

  public EntityCollection loadEntities(final EdmEntitySet targetEdmEntitySet) throws ODataApplicationException {
    try {
      final EntityCollection odataEntityCollection = new EntityCollection();
      final ODataDTOHandler<?> handler = buildHandlerInstance(targetEdmEntitySet);
      final Collection<?> result = handler.read(uriInfo);
      if (result == null) {
        return odataEntityCollection;
      }
      final JPAEntityType jpaEntityType = provider.getServiceDocument()
          .getEntityType(targetEdmEntitySet.getName());

      final EntityConverter converter = new EntityConverter(context.getOdata().createUriHelper(),
          provider.getServiceDocument(), context.getServiceMetaData());
      for (final Object o : result) {
        final Entity entity = converter.convertJPA2ODataEntity(jpaEntityType, o);
        odataEntityCollection.getEntities().add(entity);
      }
      return odataEntityCollection;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  public void updateEntity(final EdmEntitySet targetEdmEntitySet, final Entity odataEntity)
      throws ODataApplicationException {
    try {
      @SuppressWarnings("unchecked")
      final ODataDTOHandler<Object> handler = (ODataDTOHandler<Object>) buildHandlerInstance(targetEdmEntitySet);
      final JPAEntityType jpaEntityType = provider.getServiceDocument()
          .getEntityType(targetEdmEntitySet.getName());
      final EntityConverter converter = new EntityConverter(context.getOdata().createUriHelper(),
          provider.getServiceDocument(), context.getServiceMetaData());
      final Object dto = converter.convertOData2JPAEntity(odataEntity, jpaEntityType);
      handler.write(uriInfo, dto);
    } catch (InstantiationException | IllegalAccessException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }

  }

  private ODataDTOHandler<?> buildHandlerInstance(final EdmEntitySet targetEdmEntitySet)
      throws ODataJPAModelException, InstantiationException, IllegalAccessException, ODataApplicationException {
    final Class<? extends ODataDTOHandler<?>> classHandler = determineDTOHandlerClass(targetEdmEntitySet);
    final ODataDTOHandler<?> handler = classHandler.newInstance();
    context.getDependencyInjector().injectFields(handler);
    return handler;
  }
}
