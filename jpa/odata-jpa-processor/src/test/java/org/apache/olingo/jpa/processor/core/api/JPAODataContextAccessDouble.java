package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;

public class JPAODataContextAccessDouble implements JPAODataContext {

  private final JPAEdmProvider edmProvider;
  private final JPAAdapter persistenceAdapter;
  private final DependencyInjector dpi = new DependencyInjector();
  private final Map<String, List<String>> headers;

  public JPAODataContextAccessDouble(final JPAEdmProvider edmProvider,
      final JPAAdapter persistenceAdapter, final Map<String, List<String>> headers) {
    super();
    this.edmProvider = edmProvider;
    this.persistenceAdapter = persistenceAdapter;
    this.headers = headers;
  }

  @Override
  public ServiceMetadata getServiceMetaData() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<EdmxReference> getReferences() {
    fail();
    return null;
  }

  @Override
  public DebugSupport getDebugSupport() {
    fail();
    return null;
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
  public String getParameter(final String name) {
    final List<String> values = headers.get(name);
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.get(0);
  }

  @Override
  public Enumeration<String> getParameters(final String name) {
    return Collections.enumeration(headers.get(name));
  }

  @Override
  public Locale getLocale() {
    return Locale.ENGLISH;
  }
}
