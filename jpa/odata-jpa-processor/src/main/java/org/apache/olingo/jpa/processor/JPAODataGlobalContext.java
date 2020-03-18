package org.apache.olingo.jpa.processor;

import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;

/**
 * Represents the global system context.
 *
 */
public interface JPAODataGlobalContext {

  /**
   *
   * @return The dependency injector with (only) global context support (no request specific data)
   */
  public DependencyInjector getDependencyInjector();

  public OData getOdata();

  public ServiceMetadata getServiceMetaData();

  public JPAODataDatabaseProcessor getDatabaseProcessor();

  public JPAEdmProvider getEdmProvider();

  //  public List<EdmxReference> getReferences();


}
