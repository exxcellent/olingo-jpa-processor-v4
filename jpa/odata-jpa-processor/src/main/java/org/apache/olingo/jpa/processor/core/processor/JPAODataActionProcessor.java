package org.apache.olingo.jpa.processor.core.processor;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
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
import org.apache.olingo.jpa.processor.core.query.JPAQuery;
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
import org.apache.olingo.server.api.processor.ActionEntityProcessor;
import org.apache.olingo.server.api.processor.ActionPrimitiveProcessor;
import org.apache.olingo.server.api.processor.ActionVoidProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAODataActionProcessor extends AbstractProcessor
implements ActionVoidProcessor, ActionPrimitiveProcessor, ActionEntityProcessor {

	private final JPAServiceDebugger debugger;

	public JPAODataActionProcessor(final JPAODataSessionContextAccess context, final EntityManager em) {
		super(context, em);
		this.debugger = context.getDebugger();
	}

	@Override
	public void processActionEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType requestFormat, final ContentType responseFormat)
					throws ODataApplicationException, ODataLibraryException {
		final List<Object> results = processActionCall(request, uriInfo, requestFormat);
		if (results.isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else {
			final OData odata = getOData();
			// we know that the return type is 'Entity', so we doesn't need additional
			// checks...
			final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
			// the action must (currently) be the last
			final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(resourceParts.size() - 1);
			final Entity entity = convert2Entity(results);
			final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
			final UriHelper uriHelper = odata.createUriHelper();
			final String selectList = uriHelper.buildContextURLSelectList(targetEdmEntitySet.getEntityType(),
					uriInfo.getExpandOption(), uriInfo.getSelectOption());
			final ContextURL contextUrl = ContextURL.with().entitySet(targetEdmEntitySet).suffix(Suffix.ENTITY)
					.selectList(selectList).build();
			final EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl)
					.select(uriInfo.getSelectOption()).expand(uriInfo.getExpandOption()).build();
			final ODataSerializer serializer = odata.createSerializer(responseFormat);
			final EdmEntityType type = (EdmEntityType) uriResourceAction.getAction().getReturnType().getType();
			final SerializerResult serializerResult = serializer.entity(getServiceMetadata(), type, entity, options);
			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}
	}

	@Override
	public void processActionPrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType requestFormat, final ContentType responseFormat)
					throws ODataApplicationException, ODataLibraryException {
		final List<Object> results = processActionCall(request, uriInfo, requestFormat);
		if (results.isEmpty()) {
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		} else {
			// we know that the return type is primitive, so we doesn't need additional
			// checks...
			final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
			// the action must (currently) be the last
			final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(resourceParts.size() - 1);
			final EdmPrimitiveType type = (EdmPrimitiveType) uriResourceAction.getAction().getReturnType().getType();
			final Property property = convert2Primitive(type, results);
			final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
			final ContextURL contextUrl = ContextURL.with().entitySet(targetEdmEntitySet).build();
			final PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
			final ODataSerializer serializer = getOData().createSerializer(responseFormat);
			final SerializerResult serializerResult = serializer.primitive(getServiceMetadata(), type, property, options);
			response.setContent(serializerResult.getContent());
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
		}
	}

	@Override
	public void processActionVoid(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
			final ContentType requestFormat) throws ODataApplicationException, ODataLibraryException {
		processActionCall(request, uriInfo, requestFormat);
		// ignore results, return type MUST be 'void'
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	private List<Object> processActionCall(final ODataRequest request, final UriInfo uriInfo, final ContentType requestFormat)
			throws ODataApplicationException, DeserializerException {
		final int handle = debugger.startRuntimeMeasurement("JPAODataActionProcessor", "processActionVoid");

		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);
		final int noOfResourceParts = resourceParts.size();
		// the action must (currently) be the last
		final UriResourceAction uriResourceAction = (UriResourceAction) resourceParts.get(noOfResourceParts - 1);
		final OData odata = getOData();
		final ServiceMetadata serviceMetadata = getServiceMetadata();

		// determine entity context
		JPAQuery query = null;
		try {
			query = new JPAQuery(odata, targetEdmEntitySet, context, uriInfo, em, request.getAllHeaders(),
					getServiceMetadata());
		} catch (final ODataJPAModelException e) {
			debugger.stopRuntimeMeasurement(handle);
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

		// we do not expand the entities
		final EntityCollection entityCollection = query.execute(false);

		// extract request parameters as arguments for action call
		final JPAAction jpaAction = sd.getAction(uriResourceAction.getAction());
		if (jpaAction == null) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}

		Map<String, Parameter> parameters = Collections.emptyMap();
		try {
			if (!jpaAction.getParameters().isEmpty() && request.getBody().available() > 0) {
				final ODataDeserializer deserializer = odata.createDeserializer(requestFormat, serviceMetadata);
				final DeserializerResult deserializerResult = deserializer.actionParameters(request.getBody(),
						uriResourceAction.getAction());
				parameters = deserializerResult.getActionParameters();
			}
		} catch (final IOException ex) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, ex);
		}

		final List<Object> results = new LinkedList<>();
		final JPAEntityType jpaType = query.getJPAEntityType();
		if (entityCollection.getEntities() != null && entityCollection.getEntities().size() > 0) {
			for (final Entity entity : entityCollection.getEntities()) {
				// call action in context of entity
				final Object resultAction = callAction(jpaAction, parameters, jpaType, entity);
				if (resultAction != null) {
					results.add(resultAction);
				}
			}
		}

		debugger.stopRuntimeMeasurement(handle);
		return results;
	}

	private Object callAction(final JPAAction jpaAction, final Map<String, Parameter> parameters, final JPAEntityType jpaType,
			final Entity entity) throws ODataApplicationException {
		final JPAEntityHelper invoker = new JPAEntityHelper(em, sd, getServiceMetadata(), getOData().createUriHelper());
		try {
			return invoker.invokeActionMethod(jpaType, entity, jpaAction, parameters);
		} catch (final ODataException e) {
			throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
	}

}
