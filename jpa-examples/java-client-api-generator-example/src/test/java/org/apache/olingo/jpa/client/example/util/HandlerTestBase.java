package org.apache.olingo.jpa.client.example.util;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.jpa.processor.core.testmodel.PersonHandler;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

/**
 * Base for simple unit tests for generated code. For real world integration test see
 * <i>olingo-generic-servlet-example</i>.
 * @author Ralf Zozmann
 *
 */
public class HandlerTestBase extends TestBase {

  private class HandlerClassInvocationHandler implements MethodHandler, MethodFilter {

    private static final String METHOD_CREATE_CLIENT = "createClient";
    private static final String METHOD_GET_SERVICE_ROOT_URL = "getServiceRootUrl";
    private static final String METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE = "determineAuthorizationHeaderValue";

    @Override
    public boolean isHandled(final Method thisMethod) {
      switch (thisMethod.getName()) {
      case METHOD_CREATE_CLIENT:
      case METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE:
      case METHOD_GET_SERVICE_ROOT_URL:
        return true;
      default:
        return false;
      }
    }

    @Override
    public Object invoke(final Object self, final Method thisMethod, final Method proceed, final Object[] args)
        throws Throwable {
      switch (thisMethod.getName()) {
      case METHOD_CREATE_CLIENT:
        return new LocalTestODataClient(persistenceAdapter);
      case METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE:
        return null;
      case METHOD_GET_SERVICE_ROOT_URL:
        return URI;
      default:
        throw new UnsupportedOperationException("Method " + thisMethod.getName() + "() not supported");
      }
    }
  }

  /**
   * Dummy URI to have an address to call...
   */
  protected static URI URI = null;

  static {
    try {
      URI = new URI("LocalFakeCall" + ServerCallSimulator.SERVLET_PATH);
    } catch (final URISyntaxException e) {
      new RuntimeException(e);
    }
  }

  protected PersonHandler createLocalPersonAccess() throws Exception {
    return new PersonHandler(URI) {
      @Override
      protected ODataClient createClient() {
        return new LocalTestODataClient(persistenceAdapter);
      }
    };
  }

  /**
   * Create a concrete facade for given <i>abstractHandlerClass</i> to work on it without implementing the abstract
   * methods.
   */
  protected <C> C createLocalEntityAccess(final Class<C> abstractHandlerClass) throws Exception {
    checkAccessMethodPresence(abstractHandlerClass, HandlerClassInvocationHandler.METHOD_CREATE_CLIENT);
    checkAccessMethodPresence(abstractHandlerClass,
        HandlerClassInvocationHandler.METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE);
    checkAccessMethodPresence(abstractHandlerClass, HandlerClassInvocationHandler.METHOD_GET_SERVICE_ROOT_URL);

    final ProxyFactory factory = new ProxyFactory();
    final HandlerClassInvocationHandler handler = new HandlerClassInvocationHandler();
    factory.setSuperclass(abstractHandlerClass);
    factory.setFilter(handler);
    @SuppressWarnings("unchecked")
    final C access = (C) factory.create(new Class[0], new Object[0], handler);
    return access;
  }

  private void checkAccessMethodPresence(final Class<?> abstractAccessClass, final String methodToFind)
      throws UnsupportedOperationException {
    for (final Method m : abstractAccessClass.getDeclaredMethods()) {
      if (m.getName().equals(methodToFind)) {
        // ok, found
        return;
      }
    }
    throw new UnsupportedOperationException("Class " + abstractAccessClass
        + " is not of expected type, because does not need the abstract method " + methodToFind);
  }

}
