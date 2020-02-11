package org.apache.olingo.jpa.client.example;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
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
class AccessTestBase extends TestBase {

  private class AccessClassInvocationHandler implements MethodHandler, MethodFilter {

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

  private static URI URI = null;

  static {
    try {
      URI = new URI("LocalFakeCall" + TestBase.SERVLET_PATH);
    } catch (final URISyntaxException e) {
      new RuntimeException(e);
    }
  }

  protected PersonAccess createLocalPersonAccess() throws Exception {
    return new PersonAccess(URI) {
      @Override
      protected ODataClient createClient() {
        return new LocalTestODataClient(persistenceAdapter);
      }
    };
  }

  protected <C> C createLocalEntityAccess(final Class<C> abstractAccessClass) throws Exception {
    checkAccessMethodPresence(abstractAccessClass, AccessClassInvocationHandler.METHOD_CREATE_CLIENT);
    checkAccessMethodPresence(abstractAccessClass,
        AccessClassInvocationHandler.METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE);
    checkAccessMethodPresence(abstractAccessClass, AccessClassInvocationHandler.METHOD_GET_SERVICE_ROOT_URL);

    final ProxyFactory factory = new ProxyFactory();
    final AccessClassInvocationHandler handler = new AccessClassInvocationHandler();
    factory.setSuperclass(abstractAccessClass);
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
