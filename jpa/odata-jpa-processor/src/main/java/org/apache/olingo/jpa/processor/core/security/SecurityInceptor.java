package org.apache.olingo.jpa.processor.core.security;

import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.OlingoExtension;
import org.apache.olingo.server.api.uri.UriInfo;

/**
 * API defining interface to implement a custom logic limiting access to
 * resources and actions per request type.
 *
 * @author Ralf Zozmann
 *
 */
public interface SecurityInceptor extends OlingoExtension {

	/**
	 * @throws SecurityException
	 *             Thrown if call is forbidden
	 */
	public void authorize(ODataRequest odRequest, UriInfo uriInfo) throws SecurityException;
}
