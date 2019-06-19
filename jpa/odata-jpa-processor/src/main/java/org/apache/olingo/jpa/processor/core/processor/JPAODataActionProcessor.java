package org.apache.olingo.jpa.processor.core.processor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.api.JPAServiceDebugger;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.EntityConverter;
import org.apache.olingo.jpa.processor.core.query.JPAEntityQuery;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.jpa.processor.core.util.JPAEntityHelper;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.ActionEntityCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionEntityProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveCollectionProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.processor.ActionVoidProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.core.uri.queryoption.ExpandItemImpl;
import org.apache.olingo.server.core.uri.queryoption.ExpandOptionImpl;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAODataActionProcessor extends AbstractProcessor
        implements ActionVoidProcessor, ActionPrimitiveProcessor, ActionPrimitiveCollectionProcessor,
        ActionEntityProcessor, ActionEntityCollectionProcessor {

	private static class ActionCallResult {

		/**
		 * Maybe <code>null</code>
		 */
		private final JPAEntityType resultType;
		private final List<Object> resultValues;

		private ActionCallResult(final JPAEntityType resultType, final List<Object> resultValues) {
			this.resultType = resultType;
			this.resultValues = resultValues;
		}

	}

	private final Logger log = Logger.getLogger(AbstractProcessor.class.getName());
	private final JPAServiceDebugger debugger;

	public JPAODataActionProcessor(final JPAODataSessionContextAccess context, final EntityManager em) {
		super(context, em);
		this.debugger = context.getDebugger();
	}

	@Override
	public void processActionEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
	        final ContentType requestFormat, final ContentType responseFormat)
	        throws ODataApplicationException, ODataLibraryException {
		processActionEntityWithResult(request, response, uriInfo, requestFormat, responseFormat, false);
	}

	@Override
	public void processActionEntityCollection(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
	        final ContentType requestFormat,
	        final ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {
		processActionEntityWithResult(request, response, uriInfo, requestFormat, responseFormat, true);
	}

	private void processActionEntityWithResult(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
	        final ContentType requestFormat, final ContentType responseFormat, final boolean resultIsCollection)
	        throws ODataApplicationException, ODataLibraryException {
		final ActionCallResult acr = processActionsCall(request, uriInfo, requestFormat);
		if (!resultIsCollection && acr.resultValues.isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else {
			final OData odata = getOData();
			// we know that the return type is 'Entity', so we doesn't need additional
			// checks...
			final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
			// the action must (currently) be the last
			final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(resourceParts.size() - 1);
			final EntityCollection entityCollection = convert2Entities(resultIsCollection, acr);
			final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
			final UriHelper uriHelper = odata.createUriHelper();
			// assuming the complete action result should be exapnded and serialized back to client
			ExpandOption resultExpand;
			if (uriInfo.getExpandOption() == null) {
				resultExpand = new ExpandOptionImpl();
				final ExpandItemImpl expand = new ExpandItemImpl();
				expand.setIsStar(true);
				((ExpandOptionImpl) resultExpand).addExpandItem(expand);
			} else
				resultExpand = uriInfo.getExpandOption();
			final String selectList = uriHelper.buildContextURLSelectList(targetEdmEntitySet.getEntityType(),
			        resultExpand, uriInfo.getSelectOption());
			final ContextURL contextUrl = ContextURL.with().entitySet(targetEdmEntitySet).suffix(Suffix.ENTITY)
			        .selectList(selectList).build();
			final ODataSerializer serializer = odata.createSerializer(responseFormat);
			final EdmEntityType type = (EdmEntityType) uriResourceAction.getAction().getReturnType().getType();

			final SerializerResult serializerResult;
			if (resultIsCollection) {
				final EntityCollectionSerializerOptions options = EntityCollectionSerializerOptions.with().contextURL(contextUrl)
				        .select(uriInfo.getSelectOption()).expand(resultExpand).build();
				serializerResult = serializer.entityCollection(getServiceMetadata(), type, entityCollection,
				        options);
			} else if (entityCollection.getEntities().isEmpty()) {
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
				        HttpStatusCode.INTERNAL_SERVER_ERROR);
			} else {
				// we have at least one entity
				final EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl)
				        .select(uriInfo.getSelectOption()).expand(uriInfo.getExpandOption()).build();
				serializerResult = serializer.entity(getServiceMetadata(), type, entityCollection.getEntities().get(0), options);
			}

			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}

	}

	@Override
	public void processActionPrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
	        final ContentType requestFormat, final ContentType responseFormat)
	        throws ODataApplicationException, ODataLibraryException {
		processActionPrimitiveWithResult(request, response, uriInfo, requestFormat, responseFormat, false);
	}

	@Override
	public void processActionPrimitiveCollection(final ODataRequest request, final ODataResponse response,
	        final UriInfo uriInfo, final ContentType requestFormat, final ContentType responseFormat)
	        throws ODataApplicationException, ODataLibraryException {
		processActionPrimitiveWithResult(request, response, uriInfo, requestFormat, responseFormat, true);
	}

	private void processActionPrimitiveWithResult(final ODataRequest request, final ODataResponse response,
	        final UriInfo uriInfo, final ContentType requestFormat, final ContentType responseFormat,
	        final boolean resultIsCollection) throws ODataApplicationException, ODataLibraryException {
		final ActionCallResult acr = processActionsCall(request, uriInfo, requestFormat);
		if (!resultIsCollection && acr.resultValues.isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else {
			// we know that the return type is primitive, so we doesn't need additional
			// checks...
			final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
			// the action must (currently) be the last
			final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(resourceParts.size() - 1);
			final EdmPrimitiveType type = (EdmPrimitiveType) uriResourceAction.getAction().getReturnType().getType();
			final Property property = convert2Primitive(type, resultIsCollection, acr);
			final ContextURL contextUrl = ContextURL.with().type(type).build();
			final PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
			final ODataSerializer serializer = getOData().createSerializer(responseFormat);

			final SerializerResult serializerResult;
			if (resultIsCollection) {
				serializerResult = serializer.primitiveCollection(getServiceMetadata(), type, property, options);
			} else {
				serializerResult = serializer.primitive(getServiceMetadata(), type, property, options);
			}

			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}
	}

	@Override
	public void processActionVoid(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
	        final ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {
		processActionsCall(request, uriInfo, requestFormat);
		// ignore results, return type MUST be 'void'
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	/**
	 *
	 * @return List with optional result values. Every entity (for bound actions) is
	 *         called for the action and may have a result. So the result may be a list of collections.
	 */
	private ActionCallResult processActionsCall(final ODataRequest request, final UriInfo uriInfo, final ContentType requestFormat)
	        throws ODataApplicationException, DeserializerException {
		final int handle = debugger.startRuntimeMeasurement("JPAODataActionProcessor", "processActionCall");

		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final int noOfResourceParts = resourceParts.size();

		// the action must (currently) be the last
		final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(noOfResourceParts - 1);
		final JPAAction jpaAction = sd.getAction(uriResourceAction.getAction());
		if (jpaAction == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
			        HttpStatusCode.INTERNAL_SERVER_ERROR);
		}

		// extract request parameters as arguments for action call
		final OData odata = getOData();
		final ServiceMetadata serviceMetadata = getServiceMetadata();
		Map<String, Parameter> parameters = Collections.emptyMap();
		try {
			if (!jpaAction.getParameters().isEmpty() && request.getBody().available() > 0) {

				InputStream is = request.getBody();
				if (log.isLoggable(Level.FINER)) {
					// wrap input stream for debugging
					final int bufferSize = 1048576;// 1MB
					is = new BufferedInputStream(is, bufferSize);
					is.mark(bufferSize - 1);
					final String bodyContent = new BufferedReader(new InputStreamReader(is)).lines().parallel()
					        .collect(Collectors.joining("\n"));
					log.log(Level.FINER, "Request body for action call: " + bodyContent);
					is.reset();
				}
				final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
				final DeserializerResult deserializerResult = deserializer.actionParameters(is,
				        uriResourceAction.getAction());
				parameters = deserializerResult.getActionParameters();
				log.log(Level.FINER, "Request parameters for action call: " + parameters);
			} else {
				log.log(Level.FINER, "Request parameters for action call: <none>");
			}
		} catch (final IOException ex) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
			        HttpStatusCode.INTERNAL_SERVER_ERROR, ex);
		}

		final List<Object> results = new LinkedList<>();
		if (jpaAction.isBound()) {
			// determine entity context
			JPAEntityQuery query = null;
			final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
			try {
				query = new JPAEntityQuery(odata, targetEdmEntitySet, context, uriInfo, em, request.getAllHeaders(),
				        getServiceMetadata());
			} catch (final ODataJPAModelException e) {
				debugger.stopRuntimeMeasurement(handle);
				throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
				        HttpStatusCode.INTERNAL_SERVER_ERROR, e);
			}
			// we do not expand the entities
			final EntityCollection entityCollection = query.execute(false);

			final JPAEntityType jpaType = query.getQueryResultType();
			if (entityCollection.getEntities() != null && entityCollection.getEntities().size() > 0) {
				try {
					final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, getServiceMetadata(),
					        getOData().createUriHelper(), context.getDependencyInjector());
					for (final Entity entity : entityCollection.getEntities()) {
						// call action in context of entity
						final Object resultAction = invoker.invokeBoundActionMethod(jpaType, entity, jpaAction,
						        parameters);
						if (resultAction != null) {
							results.add(resultAction);
						}
					}
				} catch (final ODataException e) {
					throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
			}
		} else {
			try {
				final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, getServiceMetadata(),
				        getOData().createUriHelper(), context.getDependencyInjector());
				final Object resultAction = invoker.invokeUnboundActionMethod(jpaAction, parameters);
				if (resultAction != null) {
					results.add(resultAction);
				}
			} catch (final ODataApplicationException e) {
				throw e;
			} catch (final ODataException e) {
				throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}
		debugger.stopRuntimeMeasurement(handle);
		JPAEntityType returnType = null;
		if (jpaAction.getResultParameter() != null) {
			final FullQualifiedName fqn = jpaAction.getResultParameter().getTypeFQN();
			// will be null for primitive type (like Edm.Int32)
			returnType = sd.getEntityType(fqn);
		}
		return new ActionCallResult(returnType, results);
	}

	/**
	 * Helper method to convert a list containing multiple JPA instances or collection of entities into a similar number of OData entities
	 */
	@SuppressWarnings("unchecked")
	private EntityCollection convert2Entities(final boolean resultContainsCollections, final ActionCallResult acr)
	        throws ODataApplicationException {
		if (!resultContainsCollections && acr.resultValues.size() != 1) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
			        HttpStatusCode.INTERNAL_SERVER_ERROR);
		}

		final EntityCollection odataEntityCollection = new EntityCollection();
		final UriHelper uriHelper = getOData().createUriHelper();

		// the given type may be a super class of the real object type, so we have to derive the entity type from the object (instance)
		final EntityConverter entityConverter = new EntityConverter(acr.resultType, uriHelper, sd,
		        getServiceMetadata());

		Collection<Object> jpaEntities;
		for (final Object resultEntry : acr.resultValues) {
			// build a temporary list, also for single result entities
			if (resultContainsCollections)
				jpaEntities = (Collection<Object>) resultEntry;
			else
				jpaEntities = Collections.singletonList(resultEntry);
			for (final Object japEntity : jpaEntities) {
				final Entity entity = entityConverter.convertJPA2ODataEntity(japEntity);
				odataEntityCollection.getEntities().add(entity);
			}
		}
		return odataEntityCollection;
	}

	/**
	 * Helper method to convert a list containing one instance of primitive value
	 * into a single OData property
	 */
	private Property convert2Primitive(final EdmPrimitiveType type, final boolean resultContainsCollections,
	        final ActionCallResult acr)
	        throws ODataJPAProcessorException {
		if (acr.resultValues.isEmpty()) {
			// should never happen, because we have checked that before
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
			        HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		if (resultContainsCollections) {
			// one or more actions with collections as result
			final LinkedList<Object> transformed = new LinkedList<Object>();
			for (final Object v : acr.resultValues) {
				transformed.addAll((List<?>) v);
			}
			return new Property(type.getName(), null, ValueType.COLLECTION_PRIMITIVE, transformed);
		} else if (acr.resultValues.size() > 1) {
			// more than one primitive (multiple action results with single result)
			return new Property(type.getName(), null, ValueType.COLLECTION_PRIMITIVE, acr.resultValues);
		} else {
			// single action call with single result
			return new Property(type.getName(), null, ValueType.PRIMITIVE, acr.resultValues.get(0));
		}
	}

}
