package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.test.TestDataConstants;
import org.junit.Before;
import org.junit.Test;

public class TestIntermediateSchema extends TestMappingRoot {

	private AbstractJPASchema schema;

	@Before
	public void setup() throws ODataJPAModelException {
		final IntermediateServiceDocument serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
		schema = serviceDocument.createMetamodelSchema(PUNIT_NAME,
				emf.getMetamodel());
	}

	@Test
	public void checkSchemaGetAllEntityTypes() throws ODataJPAModelException {
		assertEquals("Wrong number of entities", TestDataConstants.NO_ENTITY_TYPES, schema.getEdmItem().getEntityTypes()
				.size());
	}

	@Test
	public void checkSchemaGetEntityTypeByNameNotNull() throws ODataJPAModelException {
		assertNotNull(schema.getEdmItem().getEntityType("BusinessPartner"));
	}

	@Test
	public void checkSchemaGetEntityTypeByNameRightEntity() throws ODataJPAModelException {
		assertEquals("BusinessPartner", schema.getEdmItem().getEntityType("BusinessPartner").getName());
	}

	@Test
	public void checkSchemaGetAllComplexTypes() throws ODataJPAModelException {
		// ChangeInformation,CommunicationData,AdministrativeInformation,PostalAddressData
		assertEquals("Wrong number of entities", 6, schema.getEdmItem().getComplexTypes().size());
	}

	@Test
	public void checkSchemaGetComplexTypeByNameNotNull() throws ODataJPAModelException {
		assertNotNull(schema.getEdmItem().getComplexType("CommunicationData"));
	}

	@Test
	public void checkSchemaGetComplexTypeByNameRightEntity() throws ODataJPAModelException {
		assertEquals("CommunicationData", schema.getEdmItem().getComplexType("CommunicationData").getName());
	}

	@Test
	public void checkSchemaGetAllFunctions() throws ODataJPAModelException {
		assertEquals("Wrong number of entities", 6, schema.getEdmItem().getFunctions().size());
	}
}
