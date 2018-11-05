package org.apache.olingo.jpa.processor.core.processor;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.JPAEntityConverter;
import org.apache.olingo.jpa.processor.core.query.JPAInstanceResultConverter;
import org.apache.olingo.jpa.processor.core.query.JPAQuery;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeCollection;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeCount;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeEntity;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializer;
import org.apache.olingo.jpa.processor.core.util.DTOEntityHelper;
import org.apache.olingo.jpa.processor.core.util.JPAEntityHelper;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.CountEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.queryoption.CountOptionImpl;

public class JPAEntityProcessor extends AbstractProcessor implements EntityProcessor, CountEntityCollectionProcessor {

	public JPAEntityProcessor(final JPAODataSessionContextAccess context, final EntityManager em) {
		super(context, em);
	}

	@Override
	public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		final EntityCollection entityCollection = retrieveEntityData(request, uriInfo);
		if (entityCollection.getEntities() == null || entityCollection.getEntities().isEmpty()) {
			// 404 Not Found indicates that the resource specified by the request URL does
			// not exist. The response body MAY
			// provide additional information.
			// A request returns 204 No Content if the requested resource has the null
			// value, or if the service applies a
			// return=minimal preference. In this case, the response body MUST be empty.
			// Assumption 404 is handled by Olingo during URL parsing
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else if (entityCollection.getEntities().size() > 1) {
			throw new ODataApplicationException("More than one entity found for request",
					HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
		} else {
			final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(), responseFormat,
					uriInfo);
			// serialize the first (and only) entry
			final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}
	}

	@Override
	public void createEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
			final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
		try {
			final JPAEntityType jpaEntityType = context.getEdmProvider().getServiceDocument().getEntitySetType(targetEdmEntitySet.getName());
			final EntityType<?> persistenceType = em.getMetamodel().entity(jpaEntityType.getTypeClass());
			final OData odata = getOData();
			final ServiceMetadata serviceMetadata = getServiceMetadata();
			final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());

			final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
			final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
			Entity odataEntity = deserializerResult.getEntity();

			final JPAEntityConverter entityConverter = new JPAEntityConverter(persistenceType, odata.createUriHelper(), sd, serviceMetadata, em.getMetamodel());
			final Object persistenceJPAEntity = entityConverter.convertOData2JPAEntity(odataEntity);
			em.persist(persistenceJPAEntity);

			//convert reverse to get also generated fields
			odataEntity = entityConverter.convertJPA2ODataEntity(persistenceJPAEntity);

			response.setHeader("Location", request.getRawBaseUri() + "/" + odataEntity.getId().toASCIIString()); // set always
			if(hasPreference(request, "return", "minimal")) {
				response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
				request.setHeader(HttpHeader.ODATA_ENTITY_ID, odataEntity.getId().toASCIIString());
			} else {
				// full response containing complete entity content
				final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(),
						responseFormat, uriInfo);
				final EntityCollection entityCollection = new EntityCollection();
				entityCollection.getEntities().add(odataEntity);
				// serialize the first (and only) entry
				final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
				response.setContent(serializerResult.getContent());
				response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
				response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
			}
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

	}

	@Override
	public void updateEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
			final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
		final OData odata = getOData();
		final ServiceMetadata serviceMetadata = getServiceMetadata();

		// DTO?
		final DTOEntityHelper helper = new DTOEntityHelper(context, serviceMetadata, uriInfo);
		if (helper.isTargetingDTO(targetEdmEntitySet)) {
			try {
				final JPAEntityType jpaEntityType = context.getEdmProvider().getServiceDocument()
						.getEntitySetType(targetEdmEntitySet.getName());
				final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());
				final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
				final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
				final Entity odataEntity = deserializerResult.getEntity();

				helper.updateEntity(targetEdmEntitySet, odataEntity);

				// full response containing complete entity content
				final EntityCollection entityCollectionResult = new EntityCollection();
				entityCollectionResult.getEntities().add(odataEntity);
				final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(), responseFormat,
						uriInfo);
				// serialize the first (and only) entry
				final SerializerResult serializerResult = serializer.serialize(request, entityCollectionResult);
				response.setContent(serializerResult.getContent());
				response.setStatusCode(HttpStatusCode.OK.getStatusCode());
				response.setHeader(HttpHeader.CONTENT_TYPE, requestFormat.toContentTypeString());
				return;
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}

		// normal JPA entity handling
		final EntityCollection entityCollectionCompleteEntities = retrieveEntityData(request, uriInfo);

		if (entityCollectionCompleteEntities.getEntities() == null || entityCollectionCompleteEntities.getEntities().isEmpty()) {
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
		} else if(entityCollectionCompleteEntities.getEntities().size() > 1) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		} else {
			try {
				final JPAEntityType jpaEntityType = context.getEdmProvider().getServiceDocument().getEntitySetType(targetEdmEntitySet.getName());
				final EntityType<?> persistenceType = em.getMetamodel().entity(jpaEntityType.getTypeClass());
				final EdmEntityType edmType = serviceMetadata.getEdm().getEntityType(jpaEntityType.getExternalFQN());

				final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
				final DeserializerResult deserializerResult = deserializer.entity(request.getBody(), edmType);
				// if PATCH method, then only a few properties are set (and no ID...)
				final Entity odataEntityPatchData = deserializerResult.getEntity();
				final Entity odataEntityMerged = mergeEntities(odataEntityPatchData,
						entityCollectionCompleteEntities.getEntities().get(0));

				final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, getServiceMetadata(),
						odata.createUriHelper(), context.getDependencyInjector());
				// load the entity as JPA instance from DB, using the ID from resource path
				final Object persistenceEntity = invoker.loadJPAEntity(jpaEntityType, odataEntityMerged);
				if(persistenceEntity == null) {
					throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
							HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
				final JPAEntityConverter entityConverter = new JPAEntityConverter(persistenceType, odata.createUriHelper(), sd, serviceMetadata, em.getMetamodel());
				final Object persistenceModifiedEntity = entityConverter.convertOData2JPAEntity(odataEntityMerged);
				// FIXME we cannot use em.merge(), because relationships are removed...
				final Object persistenceMergedEntity = em.merge(persistenceModifiedEntity);

				//convert reverse to get also generated fields
				final Entity odataEntityUpdated = entityConverter.convertJPA2ODataEntity(persistenceMergedEntity);

				// full response containing complete entity content
				final EntityCollection entityCollectionResult = new EntityCollection();
				entityCollectionResult.getEntities().add(odataEntityUpdated);
				final JPASerializer serializer = new JPASerializeEntity(getServiceMetadata(), getOData(),
						responseFormat, uriInfo);
				// serialize the first (and only) entry
				final SerializerResult serializerResult = serializer.serialize(request, entityCollectionResult);
				response.setContent(serializerResult.getContent());
				response.setStatusCode(HttpStatusCode.OK.getStatusCode());
				response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}
	}

	@Override
	public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		final EntityCollection entityCollection = retrieveEntityData(request, uriInfo);

		if (entityCollection.getEntities() == null || entityCollection.getEntities().isEmpty()) {
			// a 'dummy' message content will prevent the OData client reponse parser from
			// exceptions because empty body
			response.setContent(new ByteArrayInputStream("{}".getBytes()));
			response.setStatusCode(HttpStatusCode.NOT_FOUND.getStatusCode());
		} else {
			final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, getServiceMetadata(),
					getOData().createUriHelper(), context.getDependencyInjector());
			final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
			final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
			try {
				final JPAEntityType jpaType = sd.getEntitySetType(targetEdmEntitySet.getName());
				for(final Entity entity: entityCollection.getEntities()) {
					final Object persistenceEntity = invoker.loadJPAEntity(jpaType, entity);
					em.remove(persistenceEntity);
				}
				// ok
				response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
		}

	}

	private EntityCollection retrieveFunctionData(final ODataRequest request, final UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		final UriResourceFunction uriResourceFunction = (UriResourceFunction) uriInfo.getUriResourceParts().get(0);
		final JPAFunction jpaFunction = sd.getFunction(uriResourceFunction.getFunction());
		final JPAEntityType returnType = sd.getEntityType(jpaFunction.getResultParameter().getTypeFQN());

		// dbProcessor.query
		final JPAODataDatabaseProcessor dbProcessor = context.getDatabaseProcessor();
		final List<?> nr = dbProcessor.executeFunctionQuery(uriResourceFunction, jpaFunction, returnType, em);

		final EdmEntitySet returnEntitySet = uriResourceFunction.getFunctionImport().getReturnedEntitySet();
		try {
			final JPAInstanceResultConverter converter = new JPAInstanceResultConverter(getOData().createUriHelper(),
					sd, nr, returnEntitySet, returnType.getTypeClass());
			return converter.getResult();
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		} catch (final URISyntaxException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_URI_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

	}

	/**
	 * Central method to load (entity/dto) data from a source.
	 */
	private EntityCollection retrieveEntityData(final ODataRequest request, final UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final int lastPathSegmentIndex = resourceParts.size() - 1;
		final UriResource lastPathSegment = resourceParts.get(lastPathSegmentIndex);
		final OData odata = getOData();
		if (lastPathSegment.getKind() == UriResourceKind.function) {
			// entity dispatching is also called for functions (but not actions)
			return retrieveFunctionData(request, uriInfo);
		}

		// continue with normal entity (collection) query
		final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);

		if (targetEdmEntitySet == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.BAD_REQUEST, new IllegalArgumentException("EntitySet not found"));
		}

		final ServiceMetadata serviceMetadata = getServiceMetadata();
		final DTOEntityHelper helper = new DTOEntityHelper(context, serviceMetadata, uriInfo);
		if (helper.isTargetingDTO(targetEdmEntitySet)) {
			return helper.loadEntities(targetEdmEntitySet);
		} else {
			// Create a JPQL Query and execute it
			JPAQuery query = null;
			try {
				query = new JPAQuery(odata, targetEdmEntitySet, context, uriInfo, em, request.getAllHeaders(),
						serviceMetadata);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
						HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}

			return query.execute(true);
		}
	}

	@Override
	public void readEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		final EntityCollection entityCollection = retrieveEntityData(request, uriInfo);
		if (entityCollection.getEntities() == null) {
			// 404 Not Found indicates that the resource specified by the request URL does
			// not exist. The response body MAY
			// provide additional information.
			// A request returns 204 No Content if the requested resource has the null
			// value, or if the service applies a
			// return=minimal preference. In this case, the response body MUST be empty.
			// Assumption 404 is handled by Olingo during URL parsing
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else {
			final JPASerializer serializer = new JPASerializeCollection(getServiceMetadata(), getOData(),
					responseFormat, uriInfo);
			// serialize all entries
			final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}
	}

	@Override
	public void countEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
		// enforce $count option as given, because OLingo parser doesn't respect the
		// last resource path as system query option (a bug in Olingo?!)
		if (uriInfo.getCountOption() == null) {
			final CountOptionImpl countOption = new CountOptionImpl();
			countOption.setValue(true);
			((UriInfoImpl) uriInfo).setSystemQueryOption(countOption);
		}

		final EntityCollection entityCollection = retrieveEntityData(request, uriInfo);
		final JPASerializer serializer = new JPASerializeCount(getOData());
		// serialize all entries
		final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
		response.setContent(serializerResult.getContent());
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, ContentType.TEXT_PLAIN.toContentTypeString());
	}

}
