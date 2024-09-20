package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.junit.jupiter.api.Test;

public class TestCreateDocument extends TestMappingRoot {

  @Test
  public void checkServiceDocumentCanBeCreated() throws ODataJPAModelException {
    new IntermediateServiceDocument(PUNIT_NAME);
  }

  @Test
  public void checkServiceDocumentGetSchemaList() throws ODataJPAModelException {
    final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
    svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
    assertEquals(5, svc.getEdmSchemas().size(), "Wrong number of schemas");
  }

  @Test
  public void checkServiceDocumentGetContainerFromSchema() throws ODataJPAModelException {
    final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
    svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
    assertNotNull(svc.getEdmSchemas().get(0).getEntityContainer(), "Entity Container not found");
  }

  @Test
  public void checkServiceDocumentGetEntitySetsFromContainer() throws ODataJPAModelException {
    final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
    svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
    assertNotNull(svc.getEdmSchemas().get(0).getEntityContainer().getEntitySets(), "Entity Set not found");
  }

}
