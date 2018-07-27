package org.apache.olingo.jpa.processor.core.security;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.UriInfo;

/**
 * Generic interceptor configured via annotations on entity classes. This
 * interceptor is using annotations from the <i>javax.servlet</i> to duplicate
 * the behaviour described via {@link javax.servlet.annotation.ServletSecurity}
 * and {@link javax.servlet.annotation.HttpMethodConstraint}.
 *
 * @author Ralf Zozmann
 *
 */
public class ServletSecurityAnnotationBasedSecurityInceptor implements SecurityInceptor {

	@Inject
	EntityManager em;

	@Override
	public void authorize(final ODataRequest odRequest, final UriInfo uriInfo) throws SecurityException {
		if (em == null)
		{
			throw new SecurityException("Invalid state of inceptor");
			// TODO Auto-generated method stub
		}

	}


}
