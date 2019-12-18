package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ConfigurationImpl;
import org.apache.olingo.client.core.uri.URIBuilderImpl;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TestHelper;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class TestBase {

  protected TestHelper helper;
  protected final static JPAEdmNameBuilder nameBuilder = new JPAEdmNameBuilder(Constant.PUNIT_NAME);
  protected TestGenericJPAPersistenceAdapter persistenceAdapter;
  protected JPAEdmProvider jpaEdm;

  /**
   * Execute every test class with a fresh created database
   */
  @BeforeClass
  public static void setupDatabase() {
    DataSourceHelper.forceFreshCreatedDatabase();
  }

  @Before
  public final void setupTest() throws ODataException {
    persistenceAdapter = createPersistenceAdapter();
    jpaEdm = new JPAEdmProvider(persistenceAdapter.getNamespace(), persistenceAdapter.getMetamodel());
  }

  /**
   * Hook method for test sub classes to create an alternative persistence adapter for complete test class: example is using another
   * persistence unit.
   */
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    return new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
  }

  /**
   * Register a DTO class in meta model for later use. Must be called after {@link #setupTest()}.
   */
  @SuppressWarnings("unchecked")
  protected <T extends JPAEntityType> T registerDTO(final Class<?> dtoClass) throws ODataJPAModelException {
    return (T) jpaEdm.getServiceDocument().createDTOType(dtoClass);
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

  public static URIBuilder newUriBuilder() {
    return new URIBuilderImpl(new ConfigurationImpl(), IntegrationTestHelper.uriPrefix);
  }
}