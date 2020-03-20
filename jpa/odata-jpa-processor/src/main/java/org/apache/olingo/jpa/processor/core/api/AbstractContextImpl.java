package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

public abstract class AbstractContextImpl {

  abstract ServerCoreDebugger getServerDebugger();

  protected abstract DependencyInjectorImpl getDependencyInjectorImpl();

  protected abstract OData getOdata();

  protected abstract ServiceMetadata getServiceMetaData();

  protected abstract JPAODataDatabaseProcessor getDatabaseProcessor();

  protected abstract JPAEdmProvider getEdmProvider();
}
