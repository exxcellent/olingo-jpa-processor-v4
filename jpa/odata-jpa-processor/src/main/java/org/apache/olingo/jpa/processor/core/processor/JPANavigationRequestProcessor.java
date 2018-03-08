package org.apache.olingo.jpa.processor.core.processor;

import java.util.List;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataRequestContextAccess;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.JPAQuery;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriResource;
/**
 * @deprecated Only for legacy reasons existing
 *
 */
@Deprecated
class JPANavigationRequestProcessor extends JPAAbstractRequestProcessor implements JPARequestProcessor {
	private final ServiceMetadata serviceMetadata;

	public JPANavigationRequestProcessor(final OData odata, final ServiceMetadata serviceMetadata,
			final JPAODataSessionContextAccess context, final JPAODataRequestContextAccess requestContext) {
		super(odata, context, requestContext);
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void retrieveData(final ODataRequest request, final ODataResponse response, final ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {

		final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
		final EdmEntitySet targetEdmEntitySet = Util.determineTargetEntitySet(resourceParts);

		// Create a JPQL Query and execute it
		JPAQuery query = null;
		try {
			query = new JPAQuery(odata, targetEdmEntitySet, context, uriInfo, em, request.getAllHeaders(),
					serviceMetadata);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_PREPARATION_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

		final EntityCollection entityCollection = query.execute(true);

		if (entityCollection.getEntities() != null && entityCollection.getEntities().size() > 0) {
			final SerializerResult serializerResult = serializer.serialize(request, entityCollection);
			createSuccessResonce(response, responseFormat, serializerResult);
		} else {
			// 404 Not Found indicates that the resource specified by the request URL does not exist. The response body MAY
			// provide additional information.
			// A request returns 204 No Content if the requested resource has the null value, or if the service applies a
			// return=minimal preference. In this case, the response body MUST be empty.
			// Assumption 404 is handled by Olingo during URL parsing
			response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
		}
	}
}
