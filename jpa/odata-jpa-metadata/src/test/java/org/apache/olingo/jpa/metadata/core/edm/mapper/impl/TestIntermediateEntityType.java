package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import org.apache.olingo.jpa.processor.core.test.TestDataConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateEntityType extends TestMappingRoot {
	private Set<EntityType<?>> etList;
	private IntermediateServiceDocument serviceDocument;

	@Before
	public void setup() throws ODataJPAModelException {
		IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
		etList = emf.getMetamodel().getEntities();
		this.serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
		serviceDocument.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
	}

	@Test
	public void checkEntityTypeCanBeCreated() throws ODataJPAModelException {

		new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType("BusinessPartner"),
				serviceDocument);
	}

	@Test
	public void checkEntityTypeIgnoreSet() throws ODataJPAModelException {

		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"DummyToBeIgnored"),
				serviceDocument);
		et.getEdmItem();
		assertTrue(et.ignore());
	}

	@Ignore("Some attributes are currently commented out")
	@Test
	public void checkGetAllProperties() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Wrong number of entities", TestDataConstants.NO_DEC_ATTRIBUTES_BUISNESS_PARTNER, et.getEdmItem()
				.getProperties()
				.size());
	}

	@Test
	public void checkGetPropertyByNameNotNull() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertNotNull(et.getEdmItem().getProperty("Type"));
	}

	@Test
	public void checkGetPropertyByNameCorrectEntity() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Type", et.getEdmItem().getProperty("Type").getName());
	}

	@Test
	public void checkGetPropertyByNameCorrectEntityID() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("ID", et.getEdmItem().getProperty("ID").getName());
	}

	@Test
	public void checkGetPathByNameCorrectEntityID() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("ID", et.getPath("ID").getLeaf().getExternalName());
	}

	@Ignore("Attributea re now not longer ignored by immediate/meta-model, but by querying from DB, so that this test is wrong")
	@Test
	public void checkGetPathByNameIgnore() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertNull(et.getPath("CustomString2"));
	}

	@Ignore("Attributea re now not longer ignored by immediate/meta-model, but by querying from DB, so that this test is wrong")
	@Test
	public void checkGetPathByNameIgnoreCompexType() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertNull(et.getPath("Address/RegionCodePublisher"));
	}

	@Test
	public void checkGetInheritedAttributeByNameCorrectEntityID() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Person"),
				serviceDocument);
		assertEquals("ID", et.getPath("ID").getLeaf().getExternalName());
	}

	@Test
	public void checkGetAllNaviProperties() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Wrong number of navigation entities", 2, et.getEdmItem().getNavigationProperties().size());
	}

	@Test
	public void checkGetNaviPropertyByNameNotNull() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertNotNull(et.getEdmItem().getNavigationProperty("Roles"));
	}

	@Test
	public void checkGetNaviPropertyByNameCorrectEntity() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Roles", et.getEdmItem().getNavigationProperty("Roles").getName());
	}

	@Ignore("Address/AdministrativeDivision commented out")
	@Test
	public void checkGetAssoziationOfComplexTypeByNameCorrectEntity() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Address/AdministrativeDivision", et.getAssociationPath("Address/AdministrativeDivision").getAlias());
	}

	@Ignore("Address/AdministrativeDivision commented out")
	@Test
	public void checkGetAssoziationOfComplexTypeByNameJoinColumns() throws ODataJPAModelException {
		int actCount = 0;
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		for (final JPAOnConditionItem item : et.getAssociationPath("Address/AdministrativeDivision").getJoinConditions()) {
			if (item.getLeftPath().getAlias().equals("Address/Region")) {
				assertTrue(item.getRightPath().getAlias().equals("DivisionCode"));
				actCount++;
			}
			if (item.getLeftPath().getAlias().equals("Address/RegionCodeID")) {
				assertTrue(item.getRightPath().getAlias().equals("CodeID"));
				actCount++;
			}
			if (item.getLeftPath().getAlias().equals("Address/RegionCodePublisher")) {
				assertTrue(item.getRightPath().getAlias().equals("CodePublisher"));
				actCount++;
			}
		}
		assertEquals("Not all join columns found", 3, actCount);
	}

	@Ignore("Some attributes are currently commented out")
	@Test
	public void checkGetPropertiesSkipIgnored() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertEquals("Wrong number of entities", TestDataConstants.NO_DEC_ATTRIBUTES_BUISNESS_PARTNER - 1, et.getEdmItem()
				.getProperties().size());
	}

	@Test
	public void checkGetIsAbstract() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"),
				serviceDocument);
		assertTrue(et.getEdmItem().isAbstract());
	}

	@Test
	public void checkGetIsNotAbstract() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"),
				serviceDocument);
		assertFalse(et.getEdmItem().isAbstract());
	}

	@Test
	public void checkGetHasBaseType() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"),
				serviceDocument);
		assertEquals(PUNIT_NAME + ".BusinessPartner", et.getEdmItem().getBaseType());
	}

	@Test
	public void checkGetKeyProperties() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartnerRole"), serviceDocument);
		assertEquals("Wrong number of key propeties", 2, et.getEdmItem().getKey().size());
	}

	@Test
	public void checkGetAllAttributes() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartnerRole"),
				serviceDocument);
		assertEquals("Wrong number of entities", 2, et.getPathList().size());
	}

	@Ignore("Some attributes are currently commented out")
	@Test
	public void checkGetAllAttributesWithBaseType() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"),
				serviceDocument);
		final int exp = TestDataConstants.NO_ATTRIBUTES_BUISNESS_PARTNER
				+ TestDataConstants.NO_ATTRIBUTES_POSTAL_ADDRESS
				+ TestDataConstants.NO_ATTRIBUTES_COMMUNICATION_DATA
				+ 2 * TestDataConstants.NO_ATTRIBUTES_CHANGE_INFO
				+ TestDataConstants.NO_ATTRIBUTES_ORGANIZATION;
		assertEquals("Wrong number of entities", exp, et.getPathList().size());
	}

	@Test
	public void checkGetAllAttributesWithBaseTypeFields() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"),
				serviceDocument);

		assertNotNull(et.getPath("Type"));
		assertNotNull(et.getPath("Name1"));
		assertNotNull(et.getPath("Address" + JPASelector.PATH_SEPERATOR + "Region"));
		assertNotNull(et.getPath("AdministrativeInformation" + JPASelector.PATH_SEPERATOR
				+ "Created" + JPASelector.PATH_SEPERATOR + "By"));
	}

	@Test
	public void checkGetAllAttributeIDWithBaseType() throws ODataJPAModelException {
		final IntermediateStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"),
				serviceDocument);
		assertEquals("ID", et.getPath("ID").getAlias());
	}

	@Test
	public void checkGetKeyWithBaseType() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"Organization"), serviceDocument);
		assertEquals(1, et.getKeyAttributes().size());
	}

	@Test
	public void checkEmbeddedIdResovedProperties() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivisionDescription"), serviceDocument);
		assertEquals(5, et.getEdmItem().getProperties().size());
	}

	@Test
	public void checkEmbeddedIdResovedKey() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivisionDescription"), serviceDocument);
		assertEquals(4, et.getEdmItem().getKey().size());
	}

	@Test
	public void checkEmbeddedIdResovedKeyInternal() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivisionDescription"), serviceDocument);
		assertEquals(4, et.getKeyAttributes().size());
	}

	@Test
	public void checkEmbeddedIdResovedPath() throws ODataJPAModelException {
		final JPAStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivisionDescription"), serviceDocument);
		assertEquals(5, et.getPathList().size());
	}

	@Test
	public void checkEmbeddedIdResovedPathCodeId() throws ODataJPAModelException {
		final JPAStructuredType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivisionDescription"), serviceDocument);
		assertEquals(2, et.getPath("CodeID").getPathElements().size());
	}

	@Test
	public void checkHasStreamNoProperties() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"PersonImage"), serviceDocument);
		assertEquals(2, et.getEdmItem().getProperties().size());
	}

	@Test
	public void checkHasStreamTrue() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"PersonImage"), serviceDocument);
		assertTrue(et.getEdmItem().hasStream());
	}

	@Test
	public void checkHasStreamFalse() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"), serviceDocument);
		assertFalse(et.getEdmItem().hasStream());
	}

	@Test
	public void checkHasETagTrue() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"BusinessPartner"), serviceDocument);
		assertTrue(et.hasEtag());
	}

	@Test
	public void checkHasETagFalse() throws ODataJPAModelException {
		final IntermediateEntityType et = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), getEntityType(
				"AdministrativeDivision"), serviceDocument);
		assertFalse(et.hasEtag());
	}

	@Ignore
	@Test
	public void checkGetPropertyWithEnumerationType() {

	}

	private class PostProcessorSetIgnore extends JPAEdmMetadataPostProcessor {

		@Override
		public void processProperty(final IntermediatePropertyAccess property, final String jpaManagedTypeClassName) {
			if (jpaManagedTypeClassName.equals(
					TestMappingRoot.BUPA_CANONICAL_NAME)) {
				if (property.getInternalName().equals("communicationData")) {
					property.setIgnore(true);
				}
			}
		}

		@Override
		public void processNavigationProperty(final IntermediateNavigationPropertyAccess property,
				final String jpaManagedTypeClassName) {}
	}

	private EntityType<?> getEntityType(final String typeName) {
		for (final EntityType<?> entityType : etList) {
			if (entityType.getJavaType().getSimpleName().equals(typeName)) {
				return entityType;
			}
		}
		return null;
	}
}
