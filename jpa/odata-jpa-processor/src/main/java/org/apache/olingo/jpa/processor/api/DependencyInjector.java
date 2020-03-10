package org.apache.olingo.jpa.processor.api;

import org.apache.olingo.jpa.processor.core.util.DependencyMapping;
import org.apache.olingo.server.api.ODataApplicationException;

public interface DependencyInjector {

  /**
   * Register a value to inject into {@link #injectFields(Object) targets}.
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
  public void registerDependencyMappings(final DependencyMapping... dependencies);

  /**
   *
   * @param type
   * The type as registered via
   * {@link #registerDependencyMapping(Class, Object)}.
   * @return The value for registered type.
   */
  public <T> T getDependencyValue(final Class<T> type);

  /**
   *
   * @param type The key type to remove value for.
   */
  public void removeDependencyValue(final Class<?> type);

  /**
   * Traverse the fields of given <i>target</i> object to inject available instances as value to
   * the fields.
   */
  public void injectFields(final Object target) throws ODataApplicationException;
}
