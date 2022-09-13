package org.apache.olingo.server.core;

import org.apache.olingo.JakartaJavaxAdapterFactory;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class ODataHttpHandlerImplAccessor {

  public static void convertToHttp(final HttpServletResponse response, final ODataResponse odResponse) {
    final javax.servlet.http.HttpServletResponse javaxResp = JakartaJavaxAdapterFactory.adapt(response,
        javax.servlet.http.HttpServletResponse.class);
    ODataHttpHandlerImpl.convertToHttp(javaxResp, odResponse);
  }

  public static void copyHeaders(final ODataRequest odRequest, final HttpServletRequest req) {
    final javax.servlet.http.HttpServletRequest javaxReq = JakartaJavaxAdapterFactory.adapt(req,
        javax.servlet.http.HttpServletRequest.class);
    ODataHttpHandlerImpl.copyHeaders(odRequest, javaxReq);
  }

  public static HttpMethod extractMethod(final HttpServletRequest httpRequest) throws ODataLibraryException {
    final javax.servlet.http.HttpServletRequest javaxReq = JakartaJavaxAdapterFactory.adapt(httpRequest,
        javax.servlet.http.HttpServletRequest.class);
    return ODataHttpHandlerImpl.extractMethod(javaxReq);
  }

  public static void fillUriInformation(final ODataRequest odRequest,
      final HttpServletRequest httpRequest, final int split) {
    final javax.servlet.http.HttpServletRequest javaxReq = JakartaJavaxAdapterFactory.adapt(httpRequest,
        javax.servlet.http.HttpServletRequest.class);
    ODataHttpHandlerImpl.fillUriInformation(odRequest, javaxReq, split);
  }
}
