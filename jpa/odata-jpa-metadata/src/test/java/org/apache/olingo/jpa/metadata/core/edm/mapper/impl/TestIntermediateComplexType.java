package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.persistence.metamodel.EmbeddableType;

import org.apache.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateComplexType extends TestMappingRoot {
	private Set<EmbeddableType<?>> etList;
	private IntermediateServiceDocument serviceDocument;
	private AbstractJPASchema schema;

	@Before
	public void setup() throws ODataJPAModelException {
		IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
		etList = emf.getMetamodel().getEmbeddables();
		serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
		schema = serviceDocument.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
	}

	@Test
	public void checkComplexTypeCanBeCreated() throws ODataJPAModelException {

		new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType("CommunicationData"),
				serviceDocument);
	}

	private EmbeddableType<?> getEmbeddedableType(final String typeName) {
		for (final EmbeddableType<?> embeddableType : etList) {
			if (embeddableType.getJavaType().getSimpleName().equals(typeName)) {
				return embeddableType;
			}
		}
		return null;
	}

	@Test
	public void checkGetAllProperties() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"CommunicationData"),
				serviceDocument);
		assertEquals("Wrong number of entities", 4, ct.getEdmItem().getProperties().size());
	}

	@Test
	public void checkGetPropertyByNameNotNull() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"CommunicationData"),
				serviceDocument);
		assertNotNull(ct.getEdmItem().getProperty("LandlinePhoneNumber"));
	}

	@Test
	public void checkGetPropertyByNameCorrectEntity() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"CommunicationData"),
				serviceDocument);
		assertEquals("LandlinePhoneNumber", ct.getEdmItem().getProperty("LandlinePhoneNumber").getName());
	}

	@Test
	public void checkGetPropertyIsNullable() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		// In case nullable = true, nullable is not past to $metadata, as this is the default
		assertTrue(ct.getEdmItem().getProperty("POBox").isNullable());
	}

	@Ignore("Some attributes are currently commented out")
	@Test
	public void checkGetAllNaviProperties() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertEquals("Wrong number of entities", 1, ct.getEdmItem().getNavigationProperties().size());
	}

	@Ignore("administrativeDivision is currently commented out")
	@Test
	public void checkGetNaviPropertyByNameNotNull() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertNotNull(ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
	}

	@Ignore("administrativeDivision is currently commented out")
	@Test
	public void checkGetNaviPropertyByNameRightEntity() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertEquals("AdministrativeDivision", ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
	}

	@Test
	public void checkGetPropertiesSkipIgnored() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"CommunicationData"),
				serviceDocument);
		assertEquals("Wrong number of entities", 3, ct.getEdmItem().getProperties().size());
	}

	@Ignore("countryName is currently commented out")
	@Test
	public void checkGetDescriptionPropertyManyToOne() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertNotNull(ct.getEdmItem().getProperty("CountryName"));
	}

	@Ignore("regionName is currently commented out")
	@Test
	public void checkGetDescriptionPropertyManyToMany() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertNotNull(ct.getEdmItem().getProperty("RegionName"));
	}

	@Ignore("countryName is currently commented out")
	@Test
	public void checkDescriptionPropertyType() throws ODataJPAModelException {
		final PostProcessorSetIgnore pPDouble = new PostProcessorSetIgnore();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		ct.getEdmItem();
		assertTrue(ct.getProperty("countryName") instanceof IntermediateProperty);
	}

	@Test
	public void checkGetPropertyOfNestedComplexType() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"AdministrativeInformation"),
				serviceDocument);
		assertNotNull(ct.getPath("Created/By"));
	}

	@Test
	public void checkGetPropertyDBName() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"PostalAddressData"),
				serviceDocument);
		assertEquals("\"Address.PostOfficeBox\"", ((JPAAttributePath) ct.getPath("POBox")).getDBFieldName());
	}

	@Test
	public void checkGetPropertyDBNameOfNestedComplexType() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"AdministrativeInformation"),
				serviceDocument);
		assertEquals("\"by\"", ((JPAAttributePath) ct.getPath("Created/By")).getDBFieldName());
	}

	@Test
	public void checkGetPropertyWithComplexType() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"AdministrativeInformation"),
				serviceDocument);
		assertNotNull(ct.getEdmItem().getProperty("Created"));
	}

	@Test
	public void checkGetPropertiesWithSameComplexTypeNotEqual() throws ODataJPAModelException {
		final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
				"AdministrativeInformation"),
				serviceDocument);
		assertNotEquals(ct.getEdmItem().getProperty("Created"), ct.getEdmItem().getProperty("Updated"));
		assertNotEquals(ct.getProperty("created"), ct.getProperty("updated"));
	}

	@Ignore
	@Test
	public void checkGetPropertyWithEnumerationType() {

	}

	private class PostProcessorSetIgnore extends JPAEdmMetadataPostProcessor {

		@Override
		public void processProperty(final IntermediatePropertyAccess property, final String jpaManagedTypeClassName) {
			if (jpaManagedTypeClassName.equals(
					COMM_CANONICAL_NAME)) {
				if (property.getInternalName().equals("landlinePhoneNumber")) {
					property.setIgnore(true);
				}
			}
		}

		@Override
		public void processNavigationProperty(final IntermediateNavigationPropertyAccess property,
				final String jpaManagedTypeClassName) {
			if (jpaManagedTypeClassName.equals(ADDR_CANONICAL_NAME)) {
				if (property.getInternalName().equals("countryName")) {
					property.setIgnore(false);
				}
			}
		}
	}

}
