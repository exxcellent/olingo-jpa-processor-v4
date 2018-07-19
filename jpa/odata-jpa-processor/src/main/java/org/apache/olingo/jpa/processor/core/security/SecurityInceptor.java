package org.apache.olingo.jpa.processor.core.security;

import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.OlingoExtension;

/**
 * API defining interface to implement a custom logic limiting access to
 * resources, actions per request type.
 *
 * @author Ralf Zozmann
 *
 */
public interface SecurityInceptor extends OlingoExtension {

	/**
	 * For every incoming request an own {@link RequestSecurityHandler security
	 * handler instance} is created.
	 *
	 * @param odRequest
	 *            The ongoing request to process.
	 *
	 * @return A handler managing security aspects for the given request or
	 *         <code>null</code> if request doesn't have any security constraints.
	 */
	public RequestSecurityHandler createRequestSecurityHandler(ODataRequest odRequest);
}
