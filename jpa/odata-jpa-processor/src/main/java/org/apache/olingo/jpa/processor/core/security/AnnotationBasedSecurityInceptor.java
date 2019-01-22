package org.apache.olingo.jpa.processor.core.security;

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateAction;
import org.apache.olingo.jpa.security.AccessDefinition;
import org.apache.olingo.jpa.security.ODataEntityAccess;
import org.apache.olingo.jpa.security.ODataOperationAccess;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceAction;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Generic inceptor configured via annotations on entity classes. This inceptor
 * is using annotations from the <i>org.apache.olingo.jpa.security</i> package.
 * Behaviour:
 * <ul>
 * <li>An operation (action) or resource without annotation is not secured, so
 * public, anonymous access will be allowed. To change this behaviour the
 * inceptor can be created via an alternative
 * {@link AnnotationBasedSecurityInceptor#AnnotationBasedSecurityInceptor(SecurityConfiguration, SecurityConfiguration)
 * constructor}. Using that constructor means that ALL operations/entitites
 * without annotation are treated as having a annotation defining the values as
 * defined in the global security configuration for operation or entity.</li>
 * <li>The presence of an annotation on method (action) or class (entity) level
 * will enable the security checks for that entity/action. Depending on further
 * settings the default or the specific configuration will come into
 * effect.</li>
 * <li>Bound actions are threaded as related to an specific resource (entity)
 * and so also with a missing method annotation the entity level security is
 * respected. Unbound actions are only secured with explicit presence of
 * {@link ODataOperationAccess @ODataOperationAccess}. A global security
 * configuration for (bound) actions will have priority over a possible resource
 * security configuration.</li>
 * </ul>
 *
 * @author Ralf Zozmann
 *
 */
public class AnnotationBasedSecurityInceptor implements SecurityInceptor {

	public final class SecurityConfiguration {
		private final String[] rolesAllowed;

		private final boolean authenticationRequired;

		public SecurityConfiguration(final boolean authenticationRequired, final String[] rolesAllowed) {
			this.authenticationRequired = authenticationRequired;
			this.rolesAllowed = rolesAllowed == null ? new String[0] : rolesAllowed;
		}

		/**
		 *
		 * @return Non-<code>null</code> array of defined roles, maybe empty.
		 */
		public String[] getRolesAllowed() {
			return rolesAllowed;
		}

		public boolean isAuthenticationRequired() {
			return authenticationRequired || (rolesAllowed != null && rolesAllowed.length > 0);
		}
	}

	private static final Logger LOG = Logger.getLogger(SecurityInceptor.class.getName());

	@Inject
	private JPAEdmProvider jpaProvider;

	@Inject
	private HttpServletRequest httpRequest;

	@Inject
	private HttpServletResponse httpResponse;

	private final SecurityConfiguration globalDefaultOperationSecurityConfiguration;
	private final SecurityConfiguration globalDefaultEntitySecurityConfiguration;

	public AnnotationBasedSecurityInceptor() {
		this(null, null);
	}

	public AnnotationBasedSecurityInceptor(final SecurityConfiguration globalDefaultEntitySecurityConfiguration,
			final SecurityConfiguration globalDefaultOperationSecurityConfiguration) {
		this.globalDefaultEntitySecurityConfiguration = globalDefaultEntitySecurityConfiguration;
		this.globalDefaultOperationSecurityConfiguration = globalDefaultOperationSecurityConfiguration;
	}

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
		LOG.warning(uriInfo.getKind() + " call not covered by security inceptor");
	}

	protected void authorizeCrossjoin(final ODataRequest odRequest, final UriInfo uriInfo)
			throws ODataApplicationException {
		// accept every time (no implemented yet)
		LOG.warning(uriInfo.getKind() + " call not covered by security inceptor");
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
		final HttpMethod method = odRequest.getMethod();

		switch (lastPathSegment.getKind()) {
		case action:
			checkMethodAccess(method, (UriResourceAction) lastPathSegment);
			break;
		case complexProperty:
		case entitySet:
		case navigationProperty:
		case primitiveProperty:
		case count:
			final UriResourcePartTyped affectedResource = findParentResource(uriInfo);
			checkResourceAccess(method, affectedResource);
			break;
		case function:
		case singleton:
		case value:
		case ref:
			authorizeUncoveredCall(odRequest, uriInfo);
			break;
		default:
			throw new UnsupportedOperationException(lastPathSegment.getKind() + " unknown to security inceptor");
		}

	}

	/**
	 * Hook method for sub classes to override the behaviour for unsupported kinds
	 * of calls.
	 */
	protected void authorizeUncoveredCall(final ODataRequest odRequest, final UriInfo uriInfo) {
		final int lastPathSegmentIndex = uriInfo.getUriResourceParts().size() - 1;
		final UriResource lastPathSegment = uriInfo.getUriResourceParts().get(lastPathSegmentIndex);
		LOG.warning(lastPathSegment + " call not covered by security inceptor");
	}

	/**
	 *
	 * @return The most tailing resource uri part or <code>null</code>.
	 */
	private UriResourcePartTyped findParentResource(final UriInfo uriInfo) {
		UriResource part;
		for (int i = uriInfo.getUriResourceParts().size(); i > 0; i--) {
			part = uriInfo.getUriResourceParts().get(i - 1);
			if (UriResourceEntitySet.class.isInstance(part)) {
				return (UriResourceEntitySet) part;
			}
			if (UriResourceNavigation.class.isInstance(part)) {
				// navigation is also targeting a entity
				return (UriResourceNavigation) part;
			}
		}
		return null;
	}

	private void checkResourceAccess(final HttpMethod method, final UriResourcePartTyped uriResource)
			throws ODataApplicationException {
		if (uriResource == null) {
			return;
		}
		final EdmType type = uriResource.getType();
		final JPAEntityType jpaEntity = jpaProvider.getServiceDocument().getEntityType(type);
		final SecurityConfiguration securityConfiguration = determineEffectiveEntitySecurityConfiguration(method,
				jpaEntity);
		checkSecurityConfiguration(securityConfiguration);
	}

	private void checkMethodAccess(final HttpMethod method, final UriResourceAction uriAction)
			throws ODataApplicationException {
		if (uriAction.getAction() == null) {
			return;
		}
		final IntermediateAction jpaAction = (IntermediateAction) jpaProvider.getServiceDocument()
				.getAction(uriAction.getAction());
		final SecurityConfiguration securityConfiguration = determineEffectiveOperationSecurityConfiguration(method,
				jpaAction);
		checkSecurityConfiguration(securityConfiguration);
	}

	private void checkSecurityConfiguration(final SecurityConfiguration effectiveSecurityConfiguration)
			throws ODataApplicationException {
		if (effectiveSecurityConfiguration == null) {
			return;
		}

		if (effectiveSecurityConfiguration.isAuthenticationRequired()) {
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

		final String[] roles = effectiveSecurityConfiguration.getRolesAllowed();

		// permit all?
		if (roles.length == 0) {
			return;
		}

		// check role
		if (!isInRole(roles)) {
			throw new ODataApplicationException("Authorization not given", HttpStatusCode.FORBIDDEN.getStatusCode(),
					Locale.ENGLISH, "NotAuthorized");
		}
	}

	private SecurityConfiguration determineEffectiveEntitySecurityConfiguration(final HttpMethod method,
			final JPAEntityType jpaEntity) throws ODataApplicationException {
		// ups?
		if (jpaEntity == null) {
			return globalDefaultEntitySecurityConfiguration;
		}

		return determineEffectiveResourceSecurityConfiguration(method, jpaEntity.getTypeClass());
	}

	private SecurityConfiguration determineEffectiveResourceSecurityConfiguration(final HttpMethod method,
			final Class<?> classResource) throws ODataApplicationException {

		final ODataEntityAccess annoAccess = classResource.getAnnotation(ODataEntityAccess.class);
		if (annoAccess == null) {
			return globalDefaultEntitySecurityConfiguration;
		}

		final AccessDefinition[] httpMethodMappings = annoAccess.value();
		if (httpMethodMappings == null) {
			throw new ODataApplicationException("Authorization mapping not given",
					HttpStatusCode.FORBIDDEN.getStatusCode(),
					Locale.ENGLISH, "NotAuthorized");
		}
		AccessDefinition effectiveAccessDefinition = null;
		for (final AccessDefinition ad : httpMethodMappings) {
			if (method != ad.method()) {
				continue;
			}
			effectiveAccessDefinition = ad;
			break;
		}
		if (effectiveAccessDefinition == null) {
			throw new ODataApplicationException("Authorization mapping not defined for " + method.name(),
					HttpStatusCode.FORBIDDEN.getStatusCode(), Locale.ENGLISH, "NotAuthorized");
		}
		return new SecurityConfiguration(effectiveAccessDefinition.authenticationRequired(),
				effectiveAccessDefinition.rolesAllowed());
	}

	private SecurityConfiguration determineEffectiveOperationSecurityConfiguration(final HttpMethod method,
			final IntermediateAction jpaAction) throws ODataApplicationException {
		//ups?
		if (jpaAction == null) {
			return globalDefaultOperationSecurityConfiguration;
		}

		final ODataOperationAccess annoAccess = jpaAction.getJavaMethod().getAnnotation(ODataOperationAccess.class);

		// use found annotation
		if(annoAccess != null) {
			return new SecurityConfiguration(annoAccess.authenticationRequired(), annoAccess.rolesAllowed());
		}

		// global configuration for operation?
		if (globalDefaultOperationSecurityConfiguration != null) {
			return globalDefaultOperationSecurityConfiguration;
		}

		if (jpaAction.isBound()) {
			// the owning class is treated as resource (entity) for bound actions
			return determineEffectiveResourceSecurityConfiguration(method,
					jpaAction.getJavaMethod().getDeclaringClass());

		}
		return null;
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
