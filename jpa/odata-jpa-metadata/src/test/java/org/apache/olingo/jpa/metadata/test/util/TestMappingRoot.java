package org.apache.olingo.jpa.metadata.test.util;

import java.util.logging.Level;

import javax.persistence.EntityManagerFactory;

import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.BeforeClass;
import org.slf4j.bridge.SLF4JBridgeHandler;

public abstract class TestMappingRoot extends org.apache.olingo.jpa.test.util.AbstractTest {

  static {
    // enable logging redirect
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
  }

  protected static EntityManagerFactory emf;

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