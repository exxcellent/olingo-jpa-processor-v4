package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.jpa.processor.JPAODataContext;
import org.apache.olingo.jpa.processor.ModifiableDependencyInjector;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

public abstract class AbstractContextImpl implements JPAODataContext {

  abstract ServerCoreDebugger getServerDebugger();

  protected abstract JPAAdapter getPersistenceAdapter();

  protected abstract ModifiableDependencyInjector getDependencyInjector();

  /**
   *
   * @return The JPAAdapter, with refreshed preparation from {@link #getDependencyInjector() dependency injector}.
   */
  final JPAAdapter refreshMappingAdapter() {
    final JPAAdapter mappingAdapter = getPersistenceAdapter();
    try {
      getDependencyInjector().injectDependencyValues(mappingAdapter);
    } catch (final ODataApplicationException e) {
      throw new RuntimeException(e);
    }
    return mappingAdapter;
  }

}
