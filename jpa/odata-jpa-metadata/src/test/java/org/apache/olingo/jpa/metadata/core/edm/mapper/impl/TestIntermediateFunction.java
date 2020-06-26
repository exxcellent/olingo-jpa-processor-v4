package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner;
import org.junit.Before;
import org.junit.Test;

public class TestIntermediateFunction extends TestMappingRoot {
  private TestHelper helper;

  @Before
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
  }

  @Test
  public void checkByEntityAnnotationCreate() throws ODataJPAModelException {
    new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(helper.getEntityType(
        "BusinessPartner"), "CountRoles"), BusinessPartner.class, helper.getServiceDocument());
  }

  @Test
  public void checkByEntityAnnotationGetName() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType("BusinessPartner"), "CountRoles"), BusinessPartner.class, helper.getServiceDocument());
    assertEquals("CountRoles", func.getEdmItem().getName());
  }

  @Test
  public void checkByEntityAnnotationGetStoredProcedureName() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType(
            "BusinessPartner"),
        "CountRoles"), BusinessPartner.class, helper.getServiceDocument());
    assertEquals("COUNT_ROLES", func.getUserDefinedFunction());
  }

  private static void assertListEquals(final List<?> exp, final List<?> act) {
    assertTrue(EqualsBuilder.reflectionEquals(exp, act));
  }

  @Test
  public void checkByEntityAnnotationInputParameter1() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType("BusinessPartner"), "CountRoles"), BusinessPartner.class, helper.getServiceDocument());

    final List<CsdlParameter> expInput = new ArrayList<CsdlParameter>();
    final CsdlParameter param = new CsdlParameter();
    param.setName("Amount");
    param.setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
    param.setNullable(false);
    expInput.add(param);
    assertListEquals(expInput, func.getEdmItem().getParameters());
  }

  @Test
  public void checkByEntityAnnotationInputParameter2() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType("BusinessPartner"), "IsPrime"), BusinessPartner.class, helper.getServiceDocument());

    final List<CsdlParameter> expInput = new ArrayList<CsdlParameter>();
    final CsdlParameter param = new CsdlParameter();
    param.setName("Number");
    param.setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName());
    param.setNullable(false);
    param.setPrecision(Integer.valueOf(32));
    param.setScale(Integer.valueOf(0));
    expInput.add(param);
    assertListEquals(expInput, func.getEdmItem().getParameters());
  }

  @Test
  public void checkByEntityAnnotationResultParameterSimple() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType("BusinessPartner"), "IsPrime"), BusinessPartner.class, helper.getServiceDocument());

    assertEquals(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName().getFullQualifiedNameAsString(), func.getEdmItem()
        .getReturnType()
        .getType());
    assertEquals(ValueType.PRIMITIVE, func.getResultParameter().getResultValueType());
  }

  @Test
  public void checkByEntityAnnotationResultParameterIsEmpty() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunction(new JPAEdmNameBuilder(PUNIT_NAME), helper.getStoredProcedure(
        helper.getEntityType("BusinessPartner"), "CountRoles"), BusinessPartner.class, helper.getServiceDocument());

    assertEquals(PUNIT_NAME + ".BusinessPartner", func.getEdmItem().getReturnType().getType());
    assertEquals(null, func.getResultParameter().getResultValueType());
  }

  @Test
  public void checkByEntityAnnotationResultParameterIsEntity() throws ODataJPAModelException {
    final IntermediateFunction func = new IntermediateFunctionFactory().create(new JPAEdmNameBuilder(PUNIT_NAME), helper
        .getEntityType("Organization"), helper.getServiceDocument()).get("AllCustomersByABC");
    assertEquals(PUNIT_NAME + ".Organization", func.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkByEntityAnnotationResultParameterIsCollection() throws ODataJPAModelException {
    IntermediateFunction func = new IntermediateFunctionFactory().create(new JPAEdmNameBuilder(PUNIT_NAME), helper
        .getEntityType("Organization"), helper.getServiceDocument()).get("AllCustomersByABC");
    assertTrue(func.getEdmItem().getReturnType().isCollection());
    assertEquals(ValueType.COLLECTION_ENTITY, func.getResultParameter().getResultValueType());

    func = new IntermediateFunctionFactory().create(new JPAEdmNameBuilder(PUNIT_NAME), helper
        .getEntityType("BusinessPartner"), helper.getServiceDocument()).get("IsPrime");
    assertFalse(func.getEdmItem().getReturnType().isCollection());
  }

  @Test
  public void checkByEntityAnnotationResultParameterIsNullable() throws ODataJPAModelException {
    IntermediateFunction func = new IntermediateFunctionFactory().create(new JPAEdmNameBuilder(PUNIT_NAME), helper
        .getEntityType("Organization"), helper.getServiceDocument()).get("AllCustomersByABC");
    assertTrue(func.getEdmItem().getReturnType().isNullable());

    func = new IntermediateFunctionFactory().create(new JPAEdmNameBuilder(PUNIT_NAME), helper
        .getEntityType("BusinessPartner"), helper.getServiceDocument()).get("IsPrime");
    assertFalse(func.getEdmItem().getReturnType().isNullable());
  }
}
