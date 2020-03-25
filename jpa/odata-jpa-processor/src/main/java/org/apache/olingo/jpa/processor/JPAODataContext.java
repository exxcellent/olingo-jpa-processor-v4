package org.apache.olingo.jpa.processor;

import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;

/**
 * Represents the base context having API for global and request context.
 *
 */
public interface JPAODataContext {

  public OData getOdata();

  public ServiceMetadata getServiceMetaData();

  public JPAODataDatabaseProcessor getDatabaseProcessor();

  public JPAEdmProvider getEdmProvider();

}
