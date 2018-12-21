package org.apache.olingo.jpa.processor.core.security;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateAction;
import org.apache.olingo.jpa.security.ODataOperationAccess;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;

/**
 * Generic interceptor configured via annotations on entity classes. This
 * interceptor is using annotations from the
 * <i>org.apache.olingo.jpa.security</i> package.
 *
 * @author Ralf Zozmann
 *
 */
public class AnnotationBasedSecurityInceptor implements SecurityInceptor {

	private static final Logger LOG = Logger.getLogger(SecurityInceptor.class.getName());

	@Inject
	private JPAEdmProvider jpaProvider;

	@Inject
	private HttpServletRequest httpRequest;

	@Inject
	private HttpServletResponse httpResponse;

	@Override
	public void authorize(final ODataRequest odRequest, final UriInfo uriInfo) throws ODataApplicationException {

		if (jpaProvider == null) {
			throw new IllegalStateException("DepdencyInjection not working, missing "+JPAEdmProvider.class.getSimpleName());
		}
		if (httpRequest == null) {
			throw new IllegalStateException(
					"DepdencyInjection not working, missing " + HttpServletRequest.class.getSimpleName());
		}
		if (httpResponse == null) {
			throw new IllegalStateException(
					"DepdencyInjection not working, missing " + HttpServletResponse.class.getSimpleName());
		}

		// see org.apache.olingo.server.core.ODataDispatcher#dispatch(...)
		switch (uriInfo.getKind()) {
		case entityId:
			// handle as resource
		case resource:
			authorizeResource(odRequest, uriInfo);
			break;
		case metadata:
			authorizeMetadata(odRequest, uriInfo);
			break;
		case service:
			authorizeService(odRequest, uriInfo);
			break;
		case batch:
			authorizeBatch(odRequest, uriInfo);
			break;
		case all:
			authorizeAll(odRequest, uriInfo);
			break;
		case crossjoin:
			authorizeCrossjoin(odRequest, uriInfo);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	protected void authorizeAll(final ODataRequest odRequest, final UriInfo uriInfo) throws ODataApplicationException {
		// accept every time (no implemented yet)
	}

	protected void authorizeCrossjoin(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		// accept every time (no implemented yet)
	}

	protected void authorizeBatch(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		// accept every time
		LOG.warning(uriInfo.getKind() + " call not covered by security inceptor");
	}

	protected void authorizeMetadata(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		// accept every time
	}

	protected void authorizeService(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		// accept every time
	}

	protected void authorizeResource(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		final int lastPathSegmentIndex = uriInfo.getUriResourceParts().size() - 1;
		final UriResource lastPathSegment = uriInfo.getUriResourceParts().get(lastPathSegmentIndex);

		switch (lastPathSegment.getKind()) {
		case action:
			checkMethodAccess((UriResourceAction) lastPathSegment);
			break;
		case function:
		case entitySet:
		case navigationProperty:
		case singleton:
		case count:
		case primitiveProperty:
		case complexProperty:
		case value:
		case ref:
			LOG.warning(lastPathSegment + " call not covered by security inceptor");
			break;
		default:
			throw new UnsupportedOperationException(lastPathSegment.getKind() + " unknown to security inceptor");
		}

	}

	private void checkMethodAccess(final UriResourceAction uriAction) throws ODataApplicationException {
		if (uriAction.getAction() == null) {
			return;
		}
		final IntermediateAction jpaAction = (IntermediateAction) jpaProvider.getServiceDocument()
				.getAction(uriAction.getAction());
		final ODataOperationAccess annoAccess = jpaAction.getJavaMethod().getAnnotation(ODataOperationAccess.class);
		if (annoAccess == null) {
			return;
		}
		if (annoAccess.authenticationRequired() || annoAccess.rolesAllowed().length > 0) {
			if (httpRequest.getUserPrincipal() == null) {
				try {
					if (!httpRequest.authenticate(httpResponse)) {
						throw new ODataApplicationException("Authentication required",
								HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ENGLISH, "AuthenticationRequired");
					}
				} catch (ServletException | IOException e) {
					throw new ODataApplicationException("Authentication request failed",
							HttpStatusCode.UNAUTHORIZED.getStatusCode(), Locale.ENGLISH, "AuthenticationFailed");
				}
			}
		}
		// permit all?
		if (annoAccess.rolesAllowed().length == 0) {
			return;
		}
		if (!isInRole(annoAccess.rolesAllowed())) {
			throw new ODataApplicationException("Authorization not given", HttpStatusCode.FORBIDDEN.getStatusCode(),
					Locale.ENGLISH, "NotAuthorized");
		}
	}

	private boolean isInRole(final String[] roles) {
		for (final String role : roles) {
			if (httpRequest.isUserInRole(role)) {
				return true;
			}
		}
		return false;

	}
}
