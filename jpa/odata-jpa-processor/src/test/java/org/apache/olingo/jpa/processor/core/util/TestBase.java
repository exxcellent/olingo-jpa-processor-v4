package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ConfigurationImpl;
import org.apache.olingo.client.core.uri.URIBuilderImpl;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class TestBase {

  public enum JPAProvider {
    EclipseLink, Hibernate, OpenJPA;
  }

  protected TestHelper helper;
  protected final static JPAEdmNameBuilder nameBuilder = new JPAEdmNameBuilder(Constant.PUNIT_NAME);
  protected TestGenericJPAPersistenceAdapter persistenceAdapter;

  /**
   * Execute every test class with a fresh created database
   */
  @BeforeClass
  public static void setupDatabase() {
    DataSourceHelper.forceFreshCreatedDatabase();
  }

  @Before
  public final void setupTest() throws ODataJPAModelException {
    persistenceAdapter = createPersistenceAdapter();
  }

  /**
   * Hook method for test sub classes to create an alternative persistence adapter for complete test class: example is using another
   * persistence unit.
   */
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    return new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
  }

  protected JPAProvider getJPAProvider() {
    if (persistenceAdapter == null) {
      throw new IllegalStateException("setup test before");
    }
    if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.hibernate")) {
      return JPAProvider.Hibernate;
    }
    if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.apache.openjpa")) {
      return JPAProvider.OpenJPA;
    }
    if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.eclipse.persistence")) {
      return JPAProvider.EclipseLink;
    }
    throw new UnsupportedOperationException("Current JPA provider not known");
  }

  protected Map<String, List<String>> createHeaders() {
    final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    final List<String> languageHeaders = new ArrayList<String>();
    languageHeaders.add("de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
    headers.put("accept-language", languageHeaders);
    return headers;
  }

  protected URIBuilder newUriBuilder() {
    return new URIBuilderImpl(new ConfigurationImpl(), IntegrationTestHelper.uriPrefix);
  }
}