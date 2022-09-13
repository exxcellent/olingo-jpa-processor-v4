package org.apache.olingo.javaxadapters;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.olingo.JakartaJavaxAdapterFactory;

import jakarta.servlet.http.HttpServletResponse;

public class JavaxServletHttpHttpServletResponse implements InvocationHandler {

  private final HttpServletResponse jakartaResponse;

  public JavaxServletHttpHttpServletResponse(final HttpServletResponse jakartaResponse) {
    super();
    this.jakartaResponse = jakartaResponse;
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    switch (method.getName()) {
    case "getOutputStream":
      return JakartaJavaxAdapterFactory.adapt(jakartaResponse.getOutputStream(),
          javax.servlet.ServletOutputStream.class);
    default:
      break;
    }
    // default
    return method.invoke(jakartaResponse, args);
  }

}
