package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HttpRequestHeaderDouble {
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

		headerValue = new ArrayList<String>();
		headerValue.add("text/html,application/json,application/xml;q=0.9,image/webp,*/*;q=0.8");
		headers.put("accept", headerValue);

		headerValue = new ArrayList<String>();
		headerValue.add("gzip, deflate, sdch");
		headers.put("accept-encoding", headerValue);

		addHeader("accept-language", "de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");

	}

	public void addHeader(final String headerKey, final String headerValue) {
		final List<String> headerValues = new ArrayList<String>(1);
		headerValues.add(headerValue);
		headers.put(headerKey, headerValues);
	}

	public Enumeration<String> getEnumerator() {
		return new HeaderEnumerator(headers.keySet());
	}

	public Enumeration<String> get(final String headerName) {
		return new headerItem(headers.get(headerName));
	}

	public void setBatchRequest() {
		final List<String> headerValue = new ArrayList<String>();
		headerValue.add("multipart/mixed;boundary=abc123");
		headers.put("content-type", headerValue);
	}

	class HeaderEnumerator implements Enumeration<String> {

		private final Iterator<String> keys;

		HeaderEnumerator(final Set<String> keySet) {
			keys = keySet.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return keys.hasNext();
		}

		@Override
		public String nextElement() {
			return keys.next();
		}
	}

	class headerItem implements Enumeration<String> {
		private final Iterator<String> keys;

		public headerItem(final List<String> header) {
			keys = header.iterator();
		}

		@Override
		public boolean hasMoreElements() {
			return keys.hasNext();
		}

		@Override
		public String nextElement() {
			return keys.next();
		}

	}
}
