package org.apache.olingo.jpa.metadata.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData;
import org.junit.Before;
import org.junit.Test;

public class TestProvider extends TestMappingRoot {

  private JPAEdmProvider edmProvider;

  @Before
  public void setup() throws ODataException {
    edmProvider = new JPAEdmProvider(PUNIT_NAME, emf.getMetamodel());
  }

  @Test
  public void checkComplexTypeExisting() throws ODataException {
    assertNotNull(edmProvider.getComplexType(new FullQualifiedName(PUNIT_NAME, PostalAddressData.class
        .getSimpleName())));
  }

  @Test
  public void checkEnumTypeExisting() throws ODataException {
    assertNotNull(edmProvider.getEnumType(new FullQualifiedName(ChronoUnit.class
        .getName())));
    assertNull(edmProvider.getEnumType(new FullQualifiedName(ChronoUnit.class.getPackage().getName() + ".NoEnum")));
  }

  @Test
  public void checkEntityContainer() throws ODataException {
    assertNotNull(edmProvider.getEntityContainer());
  }

  @Test
  public void checkEntityContainerInfo() throws ODataException {
    // default container must exist
    assertNotNull(edmProvider.getEntityContainerInfo(null));
    // other container must not exist
    assertNull(edmProvider.getEntityContainerInfo(new FullQualifiedName(ChronoUnit.class
        .getName())));
  }

  @Test
  public void checkEntitySet() throws ODataException {
    // the FQN for entity container is not used... so both calls must return the same
    final CsdlEntitySet esFQN = edmProvider.getEntitySet(new FullQualifiedName(PUNIT_NAME, edmProvider
        .getEntityContainer()
        .getName()),
        "Organizations");
    assertNotNull(esFQN);
    final CsdlEntitySet esPure = edmProvider.getEntitySet(null, "Organizations");
    assertNotNull(esPure);
    assertEquals(esFQN, esPure);
  }

  @Test
  public void checkEntityType() throws ODataException {
    assertNotNull(edmProvider.getEntityType(new FullQualifiedName(PUNIT_NAME, Organization.class
        .getSimpleName())));
    assertNull(edmProvider.getEntityType(new FullQualifiedName(PUNIT_NAME, "NoEntity")));
  }

  @Test
  public void checkFunctionImport() throws ODataException {
    assertNotNull(edmProvider.getFunctionImport(null, "PopulationDensity"));
    assertNull(edmProvider.getFunctionImport(null, "___dummy__"));
  }

  @Test
  public void checkFunctions() throws ODataException {
    assertEquals(1, edmProvider.getFunctions(new FullQualifiedName(PUNIT_NAME, "CountRoles")).size());
    assertEquals(0, edmProvider.getFunctions(new FullQualifiedName(PUNIT_NAME, "NonExistingFunction")).size());
  }

  @Test
  public void checkActionImport() throws ODataException {
    assertNotNull(edmProvider.getActionImport(null, "checkPersonImageWithoutEmbeddedArgument"));
    assertNull(edmProvider.getActionImport(null, "___dummy__"));
  }

  @Test
  public void checkActions() throws ODataException {
    assertEquals(1, edmProvider.getActions(new FullQualifiedName(PUNIT_NAME, "addPhoneToOrganizationAndSave")).size());
    assertEquals(0, edmProvider.getActions(new FullQualifiedName(PUNIT_NAME, "__dummy__")).size());
  }

  @Test
  public void checkSchemas() throws ODataException {
    final List<CsdlSchema> list = edmProvider.getSchemas();
    // number depends strongly of used data model... test must be adapt often...
    // org.apache.olingo.jpa.processor.core.testmodel.otherpackage
    // java.time.temporal
    // java.time.chrono
    // org.apache.olingo.jpa
    assertEquals(4, list.size());
  }

}