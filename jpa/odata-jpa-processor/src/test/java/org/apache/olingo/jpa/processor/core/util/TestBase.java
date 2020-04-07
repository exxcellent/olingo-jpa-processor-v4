package org.apache.olingo.jpa.processor.core.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientLink;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ConfigurationImpl;
import org.apache.olingo.client.core.domain.ClientComplexValueImpl;
import org.apache.olingo.client.core.uri.URIBuilderImpl;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TestHelper;
import org.apache.olingo.jpa.processor.core.query.NavigationIfc;
import org.apache.olingo.jpa.processor.core.query.NavigationRoot;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfoKind;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.UriResourceEntitySetImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.bridge.SLF4JBridgeHandler;

public abstract class TestBase {

  static {
    // enable logging redirect
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
  }

  protected TestHelper helper;
  protected final static JPAEdmNameBuilder nameBuilder = new JPAEdmNameBuilder(Constant.PUNIT_NAME);
  protected TestGenericJPAPersistenceAdapter persistenceAdapter;
  protected JPAEdmProvider jpaEdm;
  protected OData odata;
  protected ServiceMetadata serviceMetaData;

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
    helper = new TestHelper(persistenceAdapter.getMetamodel(), Constant.PUNIT_NAME);
    jpaEdm = helper.getEdmProvider();
    odata = OData.newInstance();
    serviceMetaData = odata.createServiceMetadata(jpaEdm, Collections.emptyList());
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
    return (T) helper.getEdmProvider().getServiceDocument().createDTOType(dtoClass);
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

  @Deprecated
  protected Map<String, List<String>> createHeaders() {
    final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    final List<String> languageHeaders = new ArrayList<String>();
    languageHeaders.add("de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
    headers.put("accept-language", languageHeaders);
    return headers;
  }

  public static URIBuilder newUriBuilder() {
    return new URIBuilderImpl(new ConfigurationImpl(), ServerCallSimulator.uriPrefix) {
      @Override
      protected String getOperationInvokeMarker() {
        return "";
      }
    };
  }

  protected NavigationIfc createTestUriInfo(final String entitySetName) {
    final UriInfoImpl impl = new UriInfoImpl();
    impl.setKind(UriInfoKind.resource);
    final UriResourceEntitySetImpl ri = new UriResourceEntitySetImpl(new EdmEntitySetDouble(nameBuilder,
        entitySetName));
    impl.addResourcePart(ri);
    return new NavigationRoot(impl);
  }

  /**
   * Helper method to convert an {@link ClientEntity entity} to an {@link ClientComplexValue complex value} usable as
   * parameter for action calls.
   */
  protected final ClientComplexValue convertToActionParameter(final ClientEntity entity, final String fqnEntityTypeName)
      throws ODataException,
      IOException {
    if (entity.getTypeName() != null) {
      assert entity.getTypeName()
      .getFullQualifiedNameAsString().equals(fqnEntityTypeName);
    }
    final ClientComplexValue complexValue = new ClientComplexValueImpl(fqnEntityTypeName);
    // 1. properties
    final StringBuilder skippedProperties = new StringBuilder();
    for (final ClientProperty entityProperty : entity.getProperties()) {
      // simply reassign the entity property (without cloning)
      complexValue.add(entityProperty);
    }
    if (skippedProperties.length() > 0) {
      throw new IllegalStateException("Incomplete conversion, not processed properties: " + skippedProperties
          .toString());
    }
    // 2. relationships
    final StringBuilder skippedLinks = new StringBuilder();
    for (final ClientLink entityLink : entity.getNavigationLinks()) {
      // simply reassign the entity link (without cloning)
      complexValue.addLink(entityLink);
    }
    if (skippedLinks.length() > 0) {
      throw new IllegalStateException("Incomplete conversion, not processed relationships: " + skippedLinks
          .toString());
    }

    return complexValue;
  }

}