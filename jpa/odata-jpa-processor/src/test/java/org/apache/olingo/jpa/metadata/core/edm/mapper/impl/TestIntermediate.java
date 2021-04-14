package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.Country;
import org.apache.olingo.jpa.processor.core.testmodel.dto.sub.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

public class TestIntermediate extends TestBase {

  @Test
  public void testDTOEntitySetName() throws ODataJPAModelException {
    final JPAEntityType registeredDTOType = registerDTO(SystemRequirement.class);
    assertTrue(registeredDTOType.getEntitySetName().equals("SystemRequirementEntitySet"));
    final JPAEntityType dtoType = jpaEdmProvider.getServiceDocument().getEntityType(SystemRequirement.class);
    assertNotNull(dtoType);
    final IntermediateEntitySet eS = (IntermediateEntitySet) jpaEdmProvider.getServiceDocument().getEntitySet(dtoType);
    assertNotNull(eS);
    assertEquals("SystemRequirementEntitySet", eS.getExternalName());
  }

  @Test
  public void testJPAEntitySetName() throws ODataJPAModelException {
    final JPAEntityType entityType = jpaEdmProvider.getServiceDocument().getEntityType(Country.class);
    assertNotNull(entityType);
    final IntermediateEntitySet eS = (IntermediateEntitySet) jpaEdmProvider.getServiceDocument().getEntitySet(entityType);
    assertNotNull(eS);
    assertEquals("CountryEntitySet", eS.getExternalName());
  }
}
