package org.apache.olingo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Wrap an <i>jakarta</i> object into a <i>javax</i> equivalent one.
 * @param <I> The <i>jakarta</i> type
 * @param <O> The corresponding <i>javax</i> type
 *
 * @author Ralf Zozmann
 *
 */
public class JakartaJavaxAdapter<I, O> implements InvocationHandler {

  private JakartaJavaxAdapter() {
    super();
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    // TODO Auto-generated method stub
    return null;
  }

  @SuppressWarnings("unchecked")
  public static <I, O> O adapt(final I object, final Class<O> interfacePrimary,
      final Class<?>... additionalInterfaces) {
    final List<Class<?>> ifcs = new LinkedList<>();
    ifcs.add(interfacePrimary);
    if (additionalInterfaces != null) {
      ifcs.addAll(Arrays.asList(additionalInterfaces));
    }
    return (O) Proxy.newProxyInstance(object.getClass().getClassLoader(), ifcs.toArray(new Class<?>[ifcs.size()]),
        new JakartaJavaxAdapter<I, O>());
  }

}
