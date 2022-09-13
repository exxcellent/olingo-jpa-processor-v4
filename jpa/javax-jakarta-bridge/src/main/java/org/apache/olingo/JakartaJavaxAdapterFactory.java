package org.apache.olingo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;

public class JakartaJavaxAdapterFactory {
  /**
   * Wrap an <i>jakarta</i> object into a <i>javax</i> equivalent one.
   * @param <I> The <i>jakarta</i> type
   * @param <O> The corresponding <i>javax</i> type
   *
   * @author Ralf Zozmann
   *
   */
  private static class JakartaJavaxProxyAdapter<I, O> implements InvocationHandler {

    private final I jakartaObject;

    private JakartaJavaxProxyAdapter(final I jakartaObject) {
      super();
      this.jakartaObject = jakartaObject;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      // simply invoke the method on jakarta object
      return method.invoke(jakartaObject, args);
    }
  }

  private static class JakartaJavaxAssistHandler<I, O> implements MethodHandler, MethodFilter {

    private final I jakartaObject;

    public JakartaJavaxAssistHandler(final I jakartaObject) {
      super();
      this.jakartaObject = jakartaObject;
    }

    @Override
    public boolean isHandled(final Method thisMethod) {
      return true;
    }

    @Override
    public Object invoke(final Object self, final Method thisMethod, final Method proceed, final Object[] args)
        throws Throwable {
      return thisMethod.invoke(jakartaObject, args);
    }

  }

  @SuppressWarnings("unchecked")
  public static <I, O> O adapt(final I object, final Class<O> primaryType,
      final Class<?>... additionalInterfaces) {
    if (primaryType.isAssignableFrom(object.getClass())) {
      // avoid double proxying...
      return (O) object;
    }
    if (!primaryType.getPackageName().startsWith("javax.")) {
      throw new IllegalArgumentException("given type to adapt comes not from 'javax' namespace");
    }
    if (!primaryType.isInterface()) {
      // use javassist
      if (additionalInterfaces != null && additionalInterfaces.length > 0) {
        throw new IllegalArgumentException("additional interfaces are not supported for classes to adapt");
      }
      try {
        return adaptJakartaClass(object, primaryType);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
    // use proxy
    return adaptProxy(object, primaryType, additionalInterfaces);

  }

  @SuppressWarnings("unchecked")
  private static <I, O> O adaptProxy(final I object, final Class<?> primaryType,
      final Class<?>... additionalInterfaces) {
    final List<Class<?>> proxyInterfaces = new LinkedList<>();
    if (primaryType.isInterface()) {
      proxyInterfaces.add(primaryType);
    }
    if (additionalInterfaces != null) {
      Arrays.asList(additionalInterfaces).stream().forEach(ifc -> {
        if (!ifc.isInterface()) {
          throw new IllegalArgumentException(ifc.getName() + " isn't an interface");
        }
      });
      proxyInterfaces.addAll(Arrays.asList(additionalInterfaces));
    }

    checkJakartaInterfaceMatch(object, primaryType);

    // search for specific adapter with more logic...
    final String adapterName = buildAdapterName(primaryType);
    final O specificAdapter = lookupSpecificAdapter(object, adapterName, proxyInterfaces);
    if (specificAdapter != null) {
      return specificAdapter;
    }
    // use generic adapter
    return (O) Proxy.newProxyInstance(object.getClass().getClassLoader(), proxyInterfaces.toArray(
        new Class[proxyInterfaces
                  .size()]), new JakartaJavaxProxyAdapter<I, O>(
                      object));
  }

  private static String buildAdapterName(final Class<?> primaryType) {
    final String[] frags = primaryType.getPackageName().split("[\\.]+");
    final List<String> fragNames = new LinkedList<>();
    for (final String s : frags) {
      if (s.length() < 1) {
        continue;
      }
      fragNames.add(s.substring(0, 1).toUpperCase(Locale.ENGLISH));
      fragNames.add(s.substring(1));
    }
    fragNames.add(primaryType.getSimpleName());
    return fragNames.stream().collect(Collectors.joining());
  }

  @SuppressWarnings("unchecked")
  private static <O> O lookupSpecificAdapter(final Object object, final String adapterName,
      final Collection<Class<?>> interfaces) {
    try {
      final Class<? extends InvocationHandler> clazz = (Class<? extends InvocationHandler>) JakartaJavaxProxyAdapter.class
          .getClassLoader().loadClass("org.apache.olingo.javaxadapters." + adapterName);

      final Constructor<?>[] cons = clazz.getConstructors();
      if(cons.length != 1) {
        return null;
      }
      if(cons[0].getParameterCount() != 1) {
        return null;
      }
      final InvocationHandler instance = (InvocationHandler) cons[0].newInstance(object);
      return (O) Proxy.newProxyInstance(object.getClass().getClassLoader(), interfaces.toArray(new Class<?>[interfaces
                                                                                                            .size()]), instance);
    } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException ex) {
      return null;
    }
  }

  private static void checkJakartaInterfaceMatch(final Object jakartaObject, final Class<?> javaxType) {
    if (!javaxType.isInterface()) {
      // currently ignore types not beeing interface
      return;
    }
    final Collection<Class<?>> ifcs = getImplementedInterfaces(jakartaObject.getClass());
    int jakartaNameMatchCount = 0;
    for (final Class<?> ifc : ifcs) {
      if (!ifc.getName().startsWith("jakarta.")) {
        continue;
      }
      if (!ifc.getSimpleName().endsWith(javaxType.getSimpleName())) {
        continue;
      }
      jakartaNameMatchCount++;
    }
    if (jakartaNameMatchCount < 1) {
      throw new IllegalArgumentException(jakartaObject.getClass().getName()
          + " does not implement a 'jakarta' interface matching the name of " + javaxType.getName());
    }
  }

  private static Collection<Class<?>> getImplementedInterfaces(final Class<?> objectClass) {
    if (objectClass == null || objectClass.getName().equals("java.lang.Object")) {
      return Collections.emptyList();
    }
    final Collection<Class<?>> ifcs = new LinkedList<>();
    ifcs.addAll(Arrays.asList(objectClass.getInterfaces()));
    Arrays.asList(objectClass.getInterfaces()).stream().forEach(ifc -> {
      ifcs.addAll(getImplementedInterfaces(ifc));
    });
    ifcs.addAll(getImplementedInterfaces(objectClass.getSuperclass()));
    return ifcs;
  }

  private static <I, O> O adaptJakartaClass(final I jakartaObject, final Class<O> javaxClass)
      throws NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException,
      InvocationTargetException {

    final ProxyFactory factory = new ProxyFactory();
    final JakartaJavaxAssistHandler<I, O> handler = new JakartaJavaxAssistHandler<>(jakartaObject);
    factory.setSuperclass(javaxClass);
    factory.setFilter(handler);
    return (O) factory.create(new Class[0], new Object[0], handler);
  }
}
