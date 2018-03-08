package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlOnDelete;
import org.apache.olingo.commons.api.edm.provider.CsdlOnDeleteAction;
import org.apache.olingo.commons.api.edm.provider.CsdlReferentialConstraint;
import org.apache.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateNavigationProperty extends TestMappingRoot {
	private TestHelper helper;

	@Before
	public void setup() throws ODataJPAModelException {
		helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
	}

	@Test
	public void checkNaviProptertyCanBeCreated() {
		final EntityType<?> et = helper.getEntityType("BusinessPartner");
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
		new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getStructuredType(jpaAttribute), jpaAttribute, helper.serviceDocument);
	}

	@Test
	public void checkGetName() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertEquals("Wrong name", "Roles", property.getEdmItem().getName());
	}

	@Test
	public void checkGetType() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertEquals("Wrong name", PUNIT_NAME + ".BusinessPartnerRole", property.getEdmItem().getType());
	}

	@Test
	public void checkGetIgnoreFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getStructuredType(jpaAttribute), jpaAttribute, helper.serviceDocument);
		assertFalse(property.ignore());
	}

	@Test
	public void checkGetIgnoreTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"),
				"customString1");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getStructuredType(jpaAttribute), jpaAttribute, helper.serviceDocument);
		assertTrue(property.ignore());
	}

	@Test
	public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertTrue(property.getEdmItem().isNullable());
	}

	@Ignore("mapping has changed, so cascade delete is not longer valid?!")
	@Test
	public void checkGetPropertyOnDelete() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertEquals(CsdlOnDeleteAction.Cascade, property.getEdmItem().getOnDelete().getAction());
	}

	@Test
	public void checkGetProptertyFacetsNullableFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartnerRole"),
				"businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, helper.serviceDocument);

		assertFalse(property.getEdmItem().isNullable());
	}

	@Test
	public void checkGetProptertyFacetsCollectionTrue() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertTrue(property.getEdmItem().isNullable());
	}

	@Test
	public void checkGetProptertyFacetsColletionFalse() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartnerRole"),
				"businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, helper.serviceDocument);

		assertFalse(property.getEdmItem().isCollection());
	}

	@Test
	public void checkGetJoinColumnsSize1BP() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartner");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals(1, property.getJoinColumns().size());
	}

	@Test
	public void checkGetPartnerAdmin_Parent() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("AdministrativeDivision");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "parent");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals("Children", property.getEdmItem().getPartner());
	}

	@Test
	public void checkGetPartnerAdmin_Children() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("AdministrativeDivision");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "children");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals("Parent", property.getEdmItem().getPartner());
	}

	@Test
	public void checkGetPartnerBP_Roles() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartner");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals("BusinessPartner", property.getEdmItem().getPartner());
	}

	@Test
	public void checkGetPartnerRole_BP() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals("Roles", property.getEdmItem().getPartner());
	}

	@Test
	public void checkGetJoinColumnFilledCompletely() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartner");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);

		final IntermediateJoinColumn act = property.getJoinColumns().get(0);
		assertEquals("\"BusinessPartnerID\"", act.getName());
		assertEquals("\"ID\"", act.getReferencedColumnName());
	}

	@Test
	public void checkGetJoinColumnFilledCompletelyInvert() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);

		final IntermediateJoinColumn act = property.getJoinColumns().get(0);
		assertEquals("\"BusinessPartnerID\"", act.getName());
		assertEquals("\"ID\"", act.getReferencedColumnName());
	}

	@Test
	public void checkGetJoinColumnsSize1Roles() throws ODataJPAModelException {
		final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		assertEquals(1, property.getJoinColumns().size());
	}

	@Ignore("attribute commented out")
	@Test
	public void checkGetJoinColumnsSize2() throws ODataJPAModelException {
		final EmbeddableType<?> et = helper.getEmbeddedableType("PostalAddressData");
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "administrativeDivision");
		assertNotNull(jpaAttribute);
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getComplexType(et.getJavaType()), jpaAttribute, helper.serviceDocument);
		final List<IntermediateJoinColumn> columns = property.getJoinColumns();
		assertEquals(3, columns.size());
	}

	@Test
	public void checkGetReferentialConstraintSize() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);
		assertEquals(1, property.getProperty().getReferentialConstraints().size());
	}

	@Test
	public void checkGetReferentialConstraintBuPaRole() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);
		final List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

		for (final CsdlReferentialConstraint c : constraints) {
			assertEquals("ID", c.getProperty());
			assertEquals("BusinessPartnerID", c.getReferencedProperty());
		}
	}

	@Test
	public void checkGetReferentialConstraintRoleBuPa() throws ODataJPAModelException {
		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartnerRole"),
				"businessPartner");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartnerRole.class), jpaAttribute, helper.serviceDocument);
		final List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

		for (final CsdlReferentialConstraint c : constraints) {
			assertEquals("BusinessPartnerID", c.getProperty());
			assertEquals("ID", c.getReferencedProperty());
		}
	}

	@Test
	public void checkPostProcessorCalled() throws ODataJPAModelException {
		final PostProcessorSpy spy = new PostProcessorSpy();
		IntermediateModelElement.setPostProcessor(spy);

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType(
				"BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		property.getEdmItem();
		assertTrue(spy.called);
	}

	@Test
	public void checkPostProcessorNameChanged() throws ODataJPAModelException {
		final PostProcessorSetName pPDouble = new PostProcessorSetName();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(BusinessPartner.class), jpaAttribute, helper.serviceDocument);

		assertEquals("Wrong name", "RoleAssignment", property.getEdmItem().getName());
	}

	@Test
	public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
		final PostProcessorSetName pPDouble = new PostProcessorSetName();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getStructuredType(jpaAttribute), jpaAttribute, helper.serviceDocument);

		assertEquals("Wrong name", "RoleAssignment", property.getExternalName());
	}

	@Test
	public void checkPostProcessorSetOnDelete() throws ODataJPAModelException {
		final PostProcessorOneDelete pPDouble = new PostProcessorOneDelete();
		IntermediateModelElement.setPostProcessor(pPDouble);

		final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("AdministrativeDivision"),
				"children");
		final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
				helper.schema.getEntityType(AdministrativeDivision.class), jpaAttribute, helper.serviceDocument);

		assertEquals(CsdlOnDeleteAction.None, property.getProperty().getOnDelete().getAction());
	}

	private class PostProcessorSpy extends JPAEdmMetadataPostProcessor {
		boolean called = false;

		@Override
		public void processNavigationProperty(final IntermediateNavigationPropertyAccess property,
				final String jpaManagedTypeClassName) {
			called = true;
		}

		@Override
		public void processProperty(final IntermediatePropertyAccess property, final String jpaManagedTypeClassName) {

		}

	}

	private class PostProcessorSetName extends JPAEdmMetadataPostProcessor {
		@Override
		public void processNavigationProperty(final IntermediateNavigationPropertyAccess property,
				final String jpaManagedTypeClassName) {
			if (jpaManagedTypeClassName.equals(
					BUPA_CANONICAL_NAME)) {
				if (property.getInternalName().equals("roles")) {
					property.setExternalName("RoleAssignment");
				}
			}
		}

		@Override
		public void processProperty(final IntermediatePropertyAccess property, final String jpaManagedTypeClassName) {

		}
	}

	private class PostProcessorOneDelete extends JPAEdmMetadataPostProcessor {
		@Override
		public void processNavigationProperty(final IntermediateNavigationPropertyAccess property,
				final String jpaManagedTypeClassName) {
			if (jpaManagedTypeClassName.equals(ADMIN_CANONICAL_NAME)) {
				if (property.getInternalName().equals("children")) {
					final CsdlOnDelete oD = new CsdlOnDelete();
					oD.setAction(CsdlOnDeleteAction.None);
					property.setOnDelete(oD);
				}
			}
		}

		@Override
		public void processProperty(final IntermediatePropertyAccess property, final String jpaManagedTypeClassName) {

		}
	}
}
