package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.olingo.commons.api.http.HttpMethod;

class HttpServletRequestDouble implements HttpServletRequest {
	private final HttpRequestHeaderDouble reqHeader;
	private final String queryString;
	private final StringBuffer url;
	private final StringBuffer input;
	private HttpMethod requestMethod = null;

	public HttpServletRequestDouble(final String uri) throws IOException {
		this(uri, null);
	}

	public HttpServletRequestDouble(final String uri, final StringBuffer body) throws IOException {
		super();
		this.reqHeader = new HttpRequestHeaderDouble();
		final String[] uriParts = uri.split("\\?");
		this.url = new StringBuffer(uriParts[0]);
		if (uriParts.length == 2) {
			queryString = uriParts[1];
		} else {
			queryString = null;
		}
		if (uri.contains("$batch")) {
			reqHeader.setBatchRequest();
		}
		this.input = body;
	}

	@Override
	public Object getAttribute(final String name) {
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		fail();
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		fail();
		return null;
	}

	@Override
	public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
		fail();

	}

	@Override
	public int getContentLength() {
		fail();
		return 0;
	}

	@Override
	public String getContentType() {
		fail();
		return null;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ServletInputStreamDouble(input);
	}

	@Override
	public String getParameter(final String name) {
		return null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		fail();
		return null;
	}

	@Override
	public String[] getParameterValues(final String name) {
		fail();
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		fail();
		return null;
	}

	@Override
	public String getProtocol() {
		return "HTTP/1.1";
	}

	@Override
	public String getScheme() {
		fail();
		return null;
	}

	@Override
	public String getServerName() {
		fail();
		return null;
	}

	@Override
	public int getServerPort() {
		fail();
		return 0;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		fail();
		return null;
	}

	@Override
	public String getRemoteAddr() {
		fail();
		return null;
	}

	@Override
	public String getRemoteHost() {
		fail();
		return null;
	}

	@Override
	public void setAttribute(final String name, final Object o) {
		fail();

	}

	@Override
	public void removeAttribute(final String name) {
		fail();

	}

	@Override
	public Locale getLocale() {
		fail();
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.emptyEnumeration();
	}

	@Override
	public boolean isSecure() {
		fail();
		return false;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(final String path) {
		fail();
		return null;
	}

	@Override
	public String getRealPath(final String path) {
		fail();
		return null;
	}

	@Override
	public int getRemotePort() {
		fail();
		return 0;
	}

	@Override
	public String getLocalName() {
		fail();
		return null;
	}

	@Override
	public String getLocalAddr() {
		fail();
		return null;
	}

	@Override
	public int getLocalPort() {
		fail();
		return 0;
	}

	@Override
	public String getAuthType() {
		fail();
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		fail();
		return null;
	}

	@Override
	public long getDateHeader(final String name) {
		fail();
		return 0;
	}

	@Override
	public String getHeader(final String name) {
		return null;
	}

	@Override
	public Enumeration<String> getHeaders(final String name) {
		/*
		 * TODO org.apache.tomcat.util.http.ValuesEnumerator
		 * host : localhost:8090
		 * connection : keep-alive
		 * cache-control : max-age=0
		 * accept : text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,\*\*;q=0.8;
		 * upgrade-insecure-requests : 1
		 * user-agent : Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111
		 * Safari/537.36
		 * dnt : 1
		 * accept-encoding : gzip, deflate, sdch
		 * accept-language : de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4
		 * cookie : JSESSIONID=6155DEA85E65B9842E8474C0EF5330A6
		 *
		 */
		return reqHeader.get(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return reqHeader.getEnumerator();
	}

	@Override
	public int getIntHeader(final String name) {
		fail();
		return 0;
	}

	/**
	 * Set explicit HTTP method value instead of GET in {@link #getMethod()}.
	 */
	public void setMethod(final HttpMethod requestMethod) {
		this.requestMethod = requestMethod;
		if (requestMethod == null) {
			return;
		}
		switch (requestMethod) {
		case POST:
			// fall through
		case PUT:
			reqHeader.addHeader("Content-Type", "application/json");
			break;
		default:
			// do nothing
		}
	}

	@Override
	public String getMethod() {
		// prefer custom method before our auto detection
		if (requestMethod != null) {
			return requestMethod.toString();
		}
		// if (url.toString().contains("$batch")) {
		// return HttpMethod.POST.toString();
		// } else {
		return HttpMethod.GET.toString();
		// }
	}

	@Override
	public String getPathInfo() {
		fail();
		return null;
	}

	@Override
	public String getPathTranslated() {
		fail();
		return null;
	}

	@Override
	public String getContextPath() {
		fail();
		return null;
	}

	@Override
	public String getQueryString() {
		// $orderby=Roles/$count%20desc,Address/Region%20asc&$select=ID,Name1
		return queryString;
	}

	@Override
	public String getRemoteUser() {
		fail();
		return null;
	}

	@Override
	public boolean isUserInRole(final String role) {
		fail();
		return false;
	}

	@Override
	public Principal getUserPrincipal() {
		fail();
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		fail();
		return null;
	}

	@Override
	public String getRequestURI() {
		fail();
		return null;
	}

	@Override
	public StringBuffer getRequestURL() {
		// http://localhost:8090/BuPa/BuPa.svc/AdministrativeDivisions(DivisionCode='BE252',CodeID='3',CodePublisher='NUTS')/Parent/Parent
		// http://localhost:8090/BuPa/BuPa.svc/Organizations
		return url;
	}

	@Override
	public String getServletPath() {
		return "/Olingo.svc";
	}

	@Override
	public HttpSession getSession(final boolean create) {
		return null;
	}

	@Override
	public HttpSession getSession() {
		fail();
		return null;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		fail();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		fail();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		fail();
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		fail();
		return false;
	}

	@Override
	public long getContentLengthLong() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse)
			throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(final HttpServletResponse response) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void login(final String username, final String password) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Part getPart(final String name) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(final Class<T> handlerClass) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

}
