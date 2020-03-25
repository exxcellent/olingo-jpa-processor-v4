package org.apache.olingo.jpa.processor;

/**
 * Represents the global application context.
 *
 */
public interface JPAODataGlobalContext extends JPAODataContext {

  /**
   *
   * @return The dependency injector with (only) global context support (no request specific data)
   */
  public DependencyInjector getDependencyInjector();

}
