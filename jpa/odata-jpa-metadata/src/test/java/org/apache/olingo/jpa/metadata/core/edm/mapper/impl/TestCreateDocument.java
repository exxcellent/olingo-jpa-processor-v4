package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.junit.Test;

public class TestCreateDocument extends TestMappingRoot {

	@Test
	public void checkServiceDocumentCanBeCreated() throws ODataJPAModelException {
		new IntermediateServiceDocument(PUNIT_NAME);
	}

	@Test
	public void checkServiceDocumentGetSchemaList() throws ODataJPAModelException {
		final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
		svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
		assertEquals("Wrong number of schemas", 4, svc.getEdmSchemas().size());
	}

	@Test
	public void checkServiceDocumentGetContainerFromSchema() throws ODataJPAModelException {
		final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
		svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
		assertNotNull("Entity Container not found", svc.getEdmSchemas().get(0).getEntityContainer());
	}

	@Test
	public void checkServiceDocumentGetEntitySetsFromContainer() throws ODataJPAModelException {
		final IntermediateServiceDocument svc = new IntermediateServiceDocument(PUNIT_NAME);
		svc.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
		assertNotNull("Entity Set not found", svc.getEdmSchemas().get(0).getEntityContainer().getEntitySets());
	}

}
