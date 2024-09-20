package org.apache.olingo.jpa.persistenceunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.Set;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData;
import org.apache.olingo.jpa.processor.core.util.TestBase;
//import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;

public class TestPersistenceManager extends TestBase {

  @Test
  public void testPersistenceUnitSelectionAndMetamodel() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate has a bug switching to another PU -> the metamodel is not reloaded");

    final Set<EntityType<?>> entitiesDefault = persistenceAdapter.getMetamodel().getEntities();
    assertTrue(entitiesDefault.size() > 1);
    persistenceAdapter.dispose();

    // check the correct handling of a second persistence unit in persistence.xml
    final TestGenericJPAPersistenceAdapter specialPersistenceAdapter = new TestGenericJPAPersistenceAdapter("DUMMY",
        DataSourceHelper.DatabaseType.HSQLDB);
    final Set<EntityType<?>> entitiesSpecial = specialPersistenceAdapter.getMetamodel().getEntities();
    // Hibernate returns the number of the first loaded PU
    assertEquals(1, entitiesSpecial.size());
  }

  @Test
  public void testCardinality() throws IOException, ODataException {
    // skip test with openJPA
    assumeTrue(getJPAProvider() != JPAProvider.OpenJPA, "openJPA is using a 1:1 relationship instead of m:1");

    final EmbeddableType<PostalAddressData> paET = persistenceAdapter.getMetamodel()
        .embeddable(PostalAddressData.class);
    // openJPA 3.0.0 will fail here
    assertEquals(PersistentAttributeType.MANY_TO_ONE,
        paET.getAttribute("administrativeDivision").getPersistentAttributeType());
  }

  /**
   * Check while test execution that the correct PU is used for the JPA provider
   * in the actual maven profile
   */
  @Test
  public void testOpenJPASpecificPersistenceUnit() throws IOException, ODataException {
    // openJPA has own persistence unit declaration, because some settings are
    // incompatible to other JPA providers
    assumeTrue(getJPAProvider() == JPAProvider.OpenJPA, "openJPA is using a 1:1 relationship instead of m:1");

    assertEquals("openjpa", Constant.PUNIT_NAME);
    assertEquals(Constant.PUNIT_NAME, persistenceAdapter.getNamespace());
  }
}
