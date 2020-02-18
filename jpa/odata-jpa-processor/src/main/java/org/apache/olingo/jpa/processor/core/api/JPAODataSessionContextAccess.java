package org.apache.olingo.jpa.processor.core.api;

import java.util.List;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;

public interface JPAODataSessionContextAccess {

  public OData getOdata();

  public ServiceMetadata getServiceMetaData();

  public JPAODataDatabaseProcessor getDatabaseProcessor();

  public JPAServiceDebugger getServiceDebugger();

  public DebugSupport getDebugSupport();

  public JPAEdmProvider getEdmProvider();

  public List<EdmxReference> getReferences();

  public DependencyInjector getDependencyInjector();
}
