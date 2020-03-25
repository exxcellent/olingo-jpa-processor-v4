package org.apache.olingo.jpa.processor;

public interface ModifiableJPAODataRequestContext extends JPAODataRequestContext {
  /**
   *
   * @return The dependency injector with local scope including {@link JPAODataRequestContext#getDependencyInjector()
   * request dependency injection values} data.
   */
  @Override
  public ModifiableDependencyInjector getDependencyInjector();

}
