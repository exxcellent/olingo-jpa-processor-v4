package org.apache.olingo.jpa.metadata.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.temporal.ChronoUnit;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
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
  public void checkEntityType() throws ODataException {
    assertNotNull(edmProvider.getEntityType(new FullQualifiedName(PUNIT_NAME, Organization.class
        .getSimpleName())));
  }

}