package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.fail;

import java.util.Locale;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.DependencyInjector;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.jpa.processor.transformation.TransformingFactory;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;

public class JPAODataContextAccessDouble implements JPAODataRequestContext {

  private final JPAEdmProvider edmProvider;
  private final JPAAdapter persistenceAdapter;
  private final DependencyInjector dpi = new DependencyInjectorImpl();

  public JPAODataContextAccessDouble(final JPAEdmProvider edmProvider,
      final JPAAdapter persistenceAdapter) {
    super();
    this.edmProvider = edmProvider;
    this.persistenceAdapter = persistenceAdapter;
  }

  @Override
  public ServiceMetadata getServiceMetaData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public JPAEdmProvider getEdmProvider() {
    return edmProvider;
  }

  @Override
  public JPAODataDatabaseProcessor getDatabaseProcessor() {
    return persistenceAdapter.getDatabaseAccessor();
  }

  @Override
  public JPAServiceDebugger getServiceDebugger() {
    return new JPAEmptyDebugger();
  }

  @Override
  public OData getOdata() {
    return null;
  }

  @Override
  public DependencyInjector getDependencyInjector() {
    return dpi;
  }

  @Override
  public void setDebugSupport(final DebugSupport jpaDebugSupport) {
    // do nothing
  }

  @Override
  public Locale getLocale() {
    return Locale.ENGLISH;
  }

  @Override
  public TransformingFactory getTransformerFactory() {
    fail();
    return null;
  }

  @Override
  public EntityManager getEntityManager() {
    fail();
    return null;
  }

  @Override
  public JPAODataRequestContext createSubRequestContext() throws ODataException {
    fail();
    return null;
  }
}
