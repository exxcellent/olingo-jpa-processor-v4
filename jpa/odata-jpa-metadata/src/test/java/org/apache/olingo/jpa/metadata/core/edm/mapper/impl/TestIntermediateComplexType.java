package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.persistence.metamodel.EmbeddableType;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.Phone;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateComplexType extends TestMappingRoot {
  private Set<EmbeddableType<?>> etList;
  private IntermediateServiceDocument serviceDocument;

  @Before
  public void setup() throws ODataJPAModelException {
    etList = emf.getMetamodel().getEmbeddables();
    serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
    serviceDocument.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
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
  public void checkGetPropertiesSkipIgnored() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "CommunicationData"),
        serviceDocument);
    // one attribute is ignored, so we should have 3 public entries
    assertEquals("Wrong number of declared attributes", 4, ct.getSimpleAttributePathMap().size());
    assertEquals("Wrong number of attributes", 3, ct.getAttributes().size());
    assertEquals("Wrong number of properties", 3, ct.getEdmItem().getProperties().size());
    assertEquals("Wrong number of paths", 3, ct.getPathList().size());
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
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    // In case nullable = true, nullable is not past to $metadata, as this is the default
    assertTrue(ct.getEdmItem().getProperty("POBox").isNullable());
  }

  @Test
  public void checkGetAllNaviProperties() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertEquals("Wrong number of properties", 1, ct.getEdmItem().getNavigationProperties().size());
  }

  @Test
  public void checkGetNaviPropertyByNameNotNull() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
  }

  @Test
  public void checkGetNaviPropertyByNameRightEntity() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertEquals("AdministrativeDivision", ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
  }

  @Ignore("countryName is currently commented out")
  @Test
  public void checkGetDescriptionPropertyManyToOne() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("CountryName"));
  }

  @Ignore("regionName is currently commented out")
  @Test
  public void checkGetDescriptionPropertyManyToMany() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("RegionName"));
  }

  @Ignore("countryName is currently commented out")
  @Test
  public void checkDescriptionPropertyType() throws ODataJPAModelException {
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
        AdministrativeInformation.class.getSimpleName()),
        serviceDocument);
    assertNotEquals(ct.getEdmItem().getProperty("Created"), ct.getEdmItem().getProperty("Updated"));
    assertNotEquals(ct.getProperty("created"), ct.getProperty("updated"));
  }

  @Test
  public void checkComplexTypeWithAsIsAttributeNames() throws ODataJPAModelException {
    final IntermediateComplexType ct = new IntermediateComplexType(new JPAEdmNameBuilder(PUNIT_NAME),
        getEmbeddedableType(
            Phone.class.getSimpleName()),
        serviceDocument);
    assertTrue(ct.getAttribute("phoneNumber").getExternalName().equals("phoneNumber"));
  }

}
