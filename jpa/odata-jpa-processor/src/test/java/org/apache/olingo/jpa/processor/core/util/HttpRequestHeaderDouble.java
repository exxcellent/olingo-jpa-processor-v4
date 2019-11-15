package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class HttpRequestHeaderDouble {

	static final String HEADER_CONTENT_TYPE = "Content-Type";

	private final HashMap<String, List<String>> headers;

	public HttpRequestHeaderDouble() {
		super();
		headers = new HashMap<String, List<String>>();
		List<String> headerValue;
		headerValue = new ArrayList<String>();
		headerValue.add("localhost:8090");
		headers.put("host", headerValue);

		headerValue = new ArrayList<String>();
		headerValue.add("keep-alive");
		headers.put("connection", headerValue);

		headerValue = new ArrayList<String>();
		headerValue.add("max-age=0");
		headers.put("cache-control", headerValue);

		setHeader("accept", "text/html,application/json,application/xml;q=0.9,image/webp,*/*;q=0.8");
		setHeader("accept-encoding", "gzip, deflate, sdch");
		setHeader("accept-language", "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");

	}

	/**
	 * Set new value or replace existing value(s) for same key.
	 *
	 */
	public void setHeader(final String headerKey, final String headerValue) {
		final List<String> headerValues = new ArrayList<String>(1);
		headerValues.add(headerValue);
		headers.put(headerKey, headerValues);
	}

	public Enumeration<String> getHeaderNamesEnumeration() {
		return Collections.enumeration(headers.keySet());
	}

	public Enumeration<String> getHeaderValues(final String headerName) {
		return Collections.enumeration(headers.get(headerName));
	}

	public boolean hasHeader(final String name) {
		return headers.containsKey(name);
	}

	public void setBatchRequest() {
		final List<String> headerValue = new ArrayList<String>();
		headerValue.add("multipart/mixed;boundary=abc123");
		headers.put("content-type", headerValue);
	}
}
