package org.apache.olingo.jpa.client.example.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
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
    private static final String METHOD_CREATE_CONVERTER = "createConverter";

    private final ODataClient odataClient;
    private final Object customConverter;

    public HandlerClassInvocationHandler(final Class<?> customConverterClass) {
      odataClient = new LocalTestODataClient(persistenceAdapter);
      if (customConverterClass != null) {
        try {
          final Constructor<?> constructor = customConverterClass.getDeclaredConstructor(ClientObjectFactory.class);
          if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
          }
          this.customConverter = constructor.newInstance(
              odataClient
              .getObjectFactory());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
            | NoSuchMethodException | SecurityException e) {
          throw new IllegalArgumentException(e);
        }
      } else {
        this.customConverter = null;
      }
    }

    @Override
    public boolean isHandled(final Method thisMethod) {
      switch (thisMethod.getName()) {
      case METHOD_CREATE_CLIENT:
      case METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE:
      case METHOD_GET_SERVICE_ROOT_URL:
        return true;
      case METHOD_CREATE_CONVERTER:
        return customConverter != null;
      default:
        return false;
      }
    }

    @Override
    public Object invoke(final Object self, final Method thisMethod, final Method proceed, final Object[] args)
        throws Throwable {
      switch (thisMethod.getName()) {
      case METHOD_CREATE_CLIENT:
        return odataClient;
      case METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE:
        return null;
      case METHOD_GET_SERVICE_ROOT_URL:
        return URI;
      case METHOD_CREATE_CONVERTER:
        if (customConverter != null) {
          // use already created instance
          return customConverter;
        }
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
   * @see #createLocalEntityAccess(Class, Object) without custom converter
   */
  protected <C> C createLocalEntityAccess(final Class<C> abstractHandlerClass) throws Exception {
    return createLocalEntityAccess(abstractHandlerClass, null);
  }

  /**
   * Create a concrete facade for given <i>abstractHandlerClass</i> to work on it without implementing the abstract
   * methods.
   * @param customConverterClass If not <code>null</code> a class instance is used as converter for the handler
   * overwriting the
   * default from {@link HandlerClassInvocationHandler#METHOD_CREATE_CONVERTER createConverter()}.
   */
  protected <H, C> H createLocalEntityAccess(final Class<H> abstractHandlerClass, final Class<C> customConverterClass)
      throws Exception {
    checkAccessMethodPresence(abstractHandlerClass, HandlerClassInvocationHandler.METHOD_CREATE_CLIENT);
    checkAccessMethodPresence(abstractHandlerClass,
        HandlerClassInvocationHandler.METHOD_DETERMINE_AUTHORIZATION_HEADER_VALUE);
    checkAccessMethodPresence(abstractHandlerClass, HandlerClassInvocationHandler.METHOD_GET_SERVICE_ROOT_URL);
    checkAccessMethodPresence(abstractHandlerClass, HandlerClassInvocationHandler.METHOD_CREATE_CONVERTER);
    if (customConverterClass != null) {
      if (customConverterClass.isMemberClass() && !Modifier.isStatic(customConverterClass.getModifiers())) {
        throw new IllegalArgumentException(customConverterClass.getSimpleName()+" must be declared as 'static'");
      }
      checkAccessMethodPresence(customConverterClass, "toDto");
      checkAccessMethodPresence(customConverterClass, "toEntity");
    }

    final ProxyFactory factory = new ProxyFactory();
    final HandlerClassInvocationHandler handler = new HandlerClassInvocationHandler(customConverterClass);
    factory.setSuperclass(abstractHandlerClass);
    factory.setFilter(handler);
    @SuppressWarnings("unchecked")
    final H access = (H) factory.create(new Class[0], new Object[0], handler);
    //    if (handler.customConverter != null) {
    //      // workaround for javassist, because 'createConverter()' method, called from initializer is not covered by the
    //      // proxy instance
    //      final Field field = determineField(abstractHandlerClass, "converter");
    //      field.setAccessible(true);
    //      field.set(access, handler.customConverter);
    //      field.setAccessible(false);
    //    }
    return access;
  }

  @SuppressWarnings("unused")
  private Field determineField(final Class<?> abstractAccessClass, final String fieldName) {
    for (final Field field : abstractAccessClass.getDeclaredFields()) {
      if (field.getName().equals(fieldName)) {
        return field;
      }
    }
    if (abstractAccessClass.getSuperclass() != null) {
      return determineField(abstractAccessClass.getSuperclass(), fieldName);
    } else {
      throw new IllegalStateException("Class " + abstractAccessClass
          + " is not of expected type, because needs to have the field " + fieldName);
    }

  }

  private void checkAccessMethodPresence(final Class<?> abstractAccessClass, final String methodToFind)
      throws UnsupportedOperationException {
    for (final Method m : abstractAccessClass.getDeclaredMethods()) {
      if (m.getName().equals(methodToFind)) {
        // ok, found
        return;
      }
    }
    if (abstractAccessClass.getSuperclass() != null) {
      checkAccessMethodPresence(abstractAccessClass.getSuperclass(), methodToFind);
    } else {
      throw new UnsupportedOperationException("Class " + abstractAccessClass
          + " is not of expected type, because needs to have the abstract method " + methodToFind);
    }
  }

}
