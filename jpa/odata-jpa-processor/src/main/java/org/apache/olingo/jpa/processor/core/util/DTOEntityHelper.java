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
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.DTOConverter;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;

public class DTOEntityHelper {

	private final Logger log = Logger.getLogger(DTOEntityHelper.class.getName());

	private final OData odata;
	private final JPAEdmProvider provider;
	private final JPAODataSessionContextAccess context;
	private final ServiceMetadata serviceMetadata;
	private final UriInfo uriInfo;

	public DTOEntityHelper(final JPAODataSessionContextAccess context, final ServiceMetadata serviceMetadata,
			final UriInfo uriInfo) {
		this.context = context;
		this.odata = context.getOdata();
		this.provider = context.getEdmProvider();
		this.serviceMetadata = serviceMetadata;
		this.uriInfo = uriInfo;
	}

	private Class<? extends ODataDTOHandler<?>> determineDTOHandlerClass(final EdmEntitySet targetEdmEntitySet)
			throws ODataJPAModelException {
		final JPAEntityType jpaEntityType = provider.getServiceDocument()
				.getEntitySetType(targetEdmEntitySet.getName());
		if(jpaEntityType == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE);
		}
		final Class<?> javaType = jpaEntityType.getTypeClass();
		if (javaType == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
					"Java type not available");
		}
		final ODataDTO dtoAnnotation = javaType.getAnnotation(ODataDTO.class);
		if (dtoAnnotation == null) {
			return null;
		}
		final Class<? extends ODataDTOHandler<?>> handler = dtoAnnotation.handler();
		if (handler == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
					"ODataDTOHandler not defined");
		}
		return handler;
	}

	public boolean isTargetingDTO(final EdmEntitySet targetEdmEntitySet) {
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
			final Collection<?> result=  handler.read(uriInfo);
			if(result == null) {
				return odataEntityCollection;
			}
			final JPAEntityType jpaEntityType = provider.getServiceDocument()
					.getEntitySetType(targetEdmEntitySet.getName());

			final DTOConverter converter = new DTOConverter(jpaEntityType, odata.createUriHelper(),
					provider.getServiceDocument(), serviceMetadata);
			for(final Object o: result) {
				final Entity entity = converter.convertDTO2ODataEntity(o);
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
					.getEntitySetType(targetEdmEntitySet.getName());
			final DTOConverter converter = new DTOConverter(jpaEntityType, odata.createUriHelper(),
					provider.getServiceDocument(), serviceMetadata);
			final Object dto = converter.convertODataEntity2DTO(odataEntity);
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
