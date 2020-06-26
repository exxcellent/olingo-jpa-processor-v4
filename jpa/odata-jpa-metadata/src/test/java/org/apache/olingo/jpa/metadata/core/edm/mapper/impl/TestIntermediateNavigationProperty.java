package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlOnDeleteAction;
import org.apache.olingo.commons.api.edm.provider.CsdlReferentialConstraint;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import org.junit.Before;
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
        (IntermediateStructuredType<?>) helper.getServiceDocument().getStructuredType(jpaAttribute.getJavaType()),
        jpaAttribute,
        helper.serviceDocument);
  }

  @Test
  public void checkGetName() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);

    assertEquals("Wrong name", "Roles", property.getEdmItem().getName());
  }

  @Test
  public void checkGetType() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);

    assertEquals("Wrong name", PUNIT_NAME + ".BusinessPartnerRole", property.getEdmItem().getType());
  }

  @Test
  public void checkGetIgnoreFalse() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getStructuredType(jpaAttribute.getJavaType()),
        jpaAttribute,
        helper.serviceDocument);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetIgnoreTrue() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"),
        "customString1");
    // customString1 is not a relation, but that is ok for this test
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getStructuredType(jpaAttribute.getJavaType()),
        jpaAttribute,
        helper.serviceDocument);
    assertTrue(property.ignore());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);

    assertTrue(property.getEdmItem().isNullable().booleanValue());
  }

  @Test
  public void checkGetPropertyOnDelete() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);

    assertEquals(CsdlOnDeleteAction.Cascade, property.getEdmItem().getOnDelete().getAction());
  }

  @Test
  public void checkGetProptertyFacetsNullableFalse() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartnerRole"),
        "businessPartner");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartnerRole.class),
        jpaAttribute,
        helper.serviceDocument);

    assertFalse(property.getEdmItem().isNullable().booleanValue());
  }

  @Test
  public void checkGetProptertyFacetsCollectionTrue() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);

    assertTrue(property.getEdmItem().isNullable().booleanValue());
  }

  @Test
  public void checkGetProptertyFacetsColletionFalse() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartnerRole"),
        "businessPartner");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartnerRole.class),
        jpaAttribute,
        helper.serviceDocument);

    assertFalse(property.getEdmItem().isCollection());
  }

  @Test
  public void checkGetJoinColumnsSize1BP() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartner");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals(1, property.getSourceJoinColumns().size());
  }

  @Test
  public void checkGetPartnerAdmin_Parent() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("AdministrativeDivision");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "parent");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals("Children", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerAdmin_Children() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("AdministrativeDivision");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "children");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals("Parent", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerBP_Roles() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartner");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals("BusinessPartner", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetPartnerRole_BP() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals("Roles", property.getEdmItem().getPartner());
  }

  @Test
  public void checkGetJoinColumnFilledCompletely() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartner");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);

    final IntermediateJoinColumn act = property.getSourceJoinColumns().get(0);
    assertEquals("\"ID\"", act.getSourceEntityColumnName());
    assertEquals("\"BusinessPartnerID\"", act.getTargetColumnName());
  }

  @Test
  public void checkGetJoinColumnFilledCompletelyInvert() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);

    final IntermediateJoinColumn act = property.getSourceJoinColumns().get(0);
    assertEquals("\"BusinessPartnerID\"", act.getSourceEntityColumnName());
    assertEquals("\"ID\"", act.getTargetColumnName());
  }

  @Test
  public void checkGetJoinColumnsSize1Roles() throws ODataJPAModelException {
    final EntityType<?> et = helper.getEntityType("BusinessPartnerRole");

    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "businessPartner");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(et.getJavaType()), jpaAttribute,
        helper.serviceDocument);
    assertEquals(1, property.getSourceJoinColumns().size());
  }

  @Test
  public void checkGetJoinColumnsSize2() throws ODataJPAModelException {
    final EmbeddableType<?> et = helper.getEmbeddedableType("PostalAddressData");
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "administrativeDivision");
    assertNotNull(jpaAttribute);
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getComplexType(et.getJavaType()), jpaAttribute,
        helper.getServiceDocument());
    final List<IntermediateJoinColumn> columns = property.getSourceJoinColumns();
    assertEquals(3, columns.size());
  }

  @Test
  public void checkGetReferentialConstraintSize() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);
    assertEquals(1, property.getProperty().getReferentialConstraints().size());
  }

  @Test
  public void checkGetReferentialConstraintBuPaRole() throws ODataJPAModelException {
    final Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"), "roles");
    final IntermediateNavigationProperty property = new IntermediateNavigationProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartner.class), jpaAttribute,
        helper.serviceDocument);
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
        (IntermediateStructuredType<?>) helper.getServiceDocument().getEntityType(BusinessPartnerRole.class),
        jpaAttribute,
        helper.serviceDocument);
    final List<CsdlReferentialConstraint> constraints = property.getProperty().getReferentialConstraints();

    for (final CsdlReferentialConstraint c : constraints) {
      assertEquals("BusinessPartnerID", c.getProperty());
      assertEquals("ID", c.getReferencedProperty());
    }
  }

}
