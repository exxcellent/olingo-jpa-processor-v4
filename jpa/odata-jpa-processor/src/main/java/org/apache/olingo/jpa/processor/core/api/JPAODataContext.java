package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.server.api.debug.DebugSupport;

/**
 * Represents the request context.
 *
 */
public interface JPAODataContext extends JPAODataSessionContextAccess {

	public void setDebugSupport(final DebugSupport jpaDebugSupport);

	/**
	 *
	 * @see javax.servlet.http.HttpServletRequest#getHeader(String)
	 * @return The (HTTP) request (header) value of given <i>name</i> or <code>null</code>.
	 */
	public String getParameter(String name);

}
