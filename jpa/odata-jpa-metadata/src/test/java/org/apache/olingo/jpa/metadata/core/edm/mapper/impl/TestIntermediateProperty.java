package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateProperty extends TestMappingRoot {

	private TestHelper helper;

	@Before
	public void setup() throws ODataJPAModelException {
		helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
	}

	@Test
	public void checkProptertyCanBeCreated() throws ODataJPAModelException {
		final EmbeddableType<?> et = helper.getEmbeddedableType("CommunicationData");
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(et, "landlinePhoneNumber");
		new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute, helper.serviceDocument);
	}

	@Test
	public void checkGetProptertyName() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals("Wrong name", "Type", property.getEdmItem().getName());
	}

	@Test
	public void checkGetProptertyDBFieldName() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals("Wrong name", "\"Type\"", property.getDBFieldName());
	}

	@Test
	public void checkGetProptertyType() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals("Wrong type", EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
				property.getEdmItem().getType());
	}

	@Test
	public void checkGetProptertyComplexType() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "communicationData");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals("Wrong type", PUNIT_NAME + ".CommunicationData", property.getEdmItem().getType());
	}

	@Test
	public void checkGetProptertyIgnoreFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertFalse(property.ignore());
	}

	@Test
	public void checkGetProptertyIgnoreTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "customString1");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertTrue(property.ignore());
	}

	@Test
	public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "customString1");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertTrue(property.getEdmItem().isNullable());
	}

	@Test
	public void checkGetProptertyFacetsNullableTrueComplex() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEmbeddedableType("PostalAddressData"), "POBox");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertTrue(property.getEdmItem().isNullable());
	}

	@Test
	public void checkGetProptertyFacetsNullableFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "eTag");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertFalse(property.getEdmItem().isNullable());
	}

	@Test
	public void checkGetProptertyIsETagTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "eTag");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertTrue(property.isEtag());
	}

	@Test
	public void checkGetProptertyIsETagFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertFalse(property.isEtag());
	}

	@Test
	public void checkGetProptertyMaxLength() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "type");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals(new Integer(1), property.getEdmItem().getMaxLength());
	}

	@Test
	public void checkGetProptertyPrecisionDecimal() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "customNum1");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals(new Integer(16), property.getEdmItem().getPrecision());
	}

	@Test
	public void checkGetProptertyScaleDecimal() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "customNum1");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals(new Integer(5), property.getEdmItem().getScale());
	}

	@Test
	public void checkGetProptertyPrecisionTime() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("BusinessPartner"), "creationDateTime");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals(new Integer(3), property.getEdmItem().getPrecision());
	}

	@Test
	public void checkGetProptertyDefaultValue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEmbeddedableType("PostalAddressData"), "regionCodePublisher");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertEquals("ISO", property.getEdmItem().getDefaultValue());
	}

	@Test
	public void checkGetPropertyIsStream() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getAttribute(helper.getEntityType("PersonImage"), "image");
		final IntermediateProperty property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
				helper.serviceDocument);
		assertTrue(property.isStream());
	}

	@Ignore
	@Test
	public void checkGetSRID() {
		// Test for spatial data missing
	}

}
