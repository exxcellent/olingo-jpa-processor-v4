package org.apache.olingo.server.core;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;

public abstract class ODataHttpHandlerImplAccessor {

	public static void convertToHttp(final HttpServletResponse response, final ODataResponse odResponse) {
		ODataHttpHandlerImpl.convertToHttp(response, odResponse);
	}

	public static void copyHeaders(final ODataRequest odRequest, final HttpServletRequest req) {
		ODataHttpHandlerImpl.copyHeaders(odRequest, req);
	}

	public static HttpMethod extractMethod(final HttpServletRequest httpRequest) throws ODataLibraryException {
		return ODataHttpHandlerImpl.extractMethod(httpRequest);
	}

	public static void fillUriInformation(final ODataRequest odRequest,
	        final HttpServletRequest httpRequest, final int split) {
		ODataHttpHandlerImpl.fillUriInformation(odRequest, httpRequest, split);
	}
}
