package org.apache.olingo.jpa.processor.core.api;

import java.util.Enumeration;
import java.util.Locale;

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

	/**
	 *
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
	 * @return The (HTTP) request (header) values of given <i>name</i> or <code>null</code>.
	 */
	public Enumeration<String> getParameters(String name);

	/**
	 *
	 * @return The preferred locale, never <code>null</code>.
	 */
	public Locale getLocale();
}
