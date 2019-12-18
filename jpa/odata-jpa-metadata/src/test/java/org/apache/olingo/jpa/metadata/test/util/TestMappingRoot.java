package org.apache.olingo.jpa.metadata.test.util;

import javax.persistence.EntityManagerFactory;

import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.BeforeClass;

public abstract class TestMappingRoot extends org.apache.olingo.jpa.test.util.AbstractTest {

  protected static EntityManagerFactory emf;

  protected static final String BUPA_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner";
  protected static final String ADDR_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData";
  protected static final String COMM_CANONICAL_NAME = "org.apache.olingo.jpa.processor.core.testmodel.CommunicationData";
  protected static final String ADMIN_CANONICAL_NAME =
      "org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision";

  @BeforeClass
  public static void setupClass() {
    emf = createEntityManagerFactory(DataSourceHelper.DatabaseType.HSQLDB);
  }

  protected JPAProvider getJPAProvider() {
    if (emf == null) {
      throw new IllegalStateException("setup test before");
    }
    if (emf.getClass().getName().startsWith("org.hibernate")) {
      return JPAProvider.Hibernate;
    }
    if (emf.getClass().getName().startsWith("org.apache.openjpa")) {
      return JPAProvider.OpenJPA;
    }
    if (emf.getClass().getName().startsWith("org.eclipse.persistence")) {
      return JPAProvider.EclipseLink;
    }
    throw new UnsupportedOperationException("Current JPA provider not known");
  }

}