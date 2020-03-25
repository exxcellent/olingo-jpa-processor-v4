package org.apache.olingo.jpa.processor;

import org.apache.olingo.server.api.ODataApplicationException;

public interface DependencyInjector {

  /**
   *
   * @param type
   * The type as registered via
   * {@link #registerDependencyMapping(Class, Object)}.
   * @return The value for registered type.
   */
  public <T> T getDependencyValue(final Class<T> type);

  /**
   * Traverse the fields of given <i>target</i> object to inject available instances as value to
   * the fields.
   */
  public void injectDependencyValues(final Object target) throws ODataApplicationException;
}
