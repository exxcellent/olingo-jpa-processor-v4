package org.apache.olingo.jpa.processor;

import org.apache.olingo.jpa.processor.core.util.TypedParameter;

/**
 * More specific interface allowing the modification of values handled by the dependency injector.
 *
 */
public interface ModifiableDependencyInjector extends DependencyInjector {
  /**
   * Register a value to inject into {@link #injectDependencyValues(Object) targets}.
   *
   * @param type
   * The type object used to register. The type must match the (field)
   * type of injection.
   * @param value
   * The value to inject.
   */
  public void registerDependencyMapping(final Class<?> type, final Object value);

  /**
   * Register multiple dependency mappings consisting of type and value.
   *
   * @see #registerDependencyMapping(Class, Object)
   */
  public void registerDependencyMappings(final TypedParameter... dependencies);

  /**
   *
   * @param type The key type to remove value for.
   */
  public void removeDependencyValue(final Class<?> type);

}
