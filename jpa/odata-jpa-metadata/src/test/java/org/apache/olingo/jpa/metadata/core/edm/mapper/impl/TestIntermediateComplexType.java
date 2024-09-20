package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.Phone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.metamodel.EmbeddableType;

public class TestIntermediateComplexType extends TestMappingRoot {
  private Set<EmbeddableType<?>> etList;
  private IntermediateServiceDocument serviceDocument;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    etList = emf.getMetamodel().getEmbeddables();
    serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
    serviceDocument.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
  }

  @Test
  public void checkComplexTypeCanBeCreated() throws ODataJPAModelException {

    new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType("CommunicationData"),
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
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "CommunicationData"), serviceDocument);
    // one attribute is ignored, so we should have 3 public entries
    assertEquals(4, ct.getSimpleAttributePathMap().size(), "Wrong number of declared attributes");
    assertEquals(3, ct.getAttributes(true).size(), "Wrong number of attributes");
    assertEquals(3, ct.getEdmItem().getProperties().size(), "Wrong number of properties");
    assertEquals(3, ct.getPathList().size(), "Wrong number of paths");
  }

  @Test
  public void checkGetPropertyByNameNotNull() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "CommunicationData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("LandlinePhoneNumber"));
  }

  @Test
  public void checkGetPropertyByNameCorrectEntity() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "CommunicationData"),
        serviceDocument);
    assertEquals("LandlinePhoneNumber", ct.getEdmItem().getProperty("LandlinePhoneNumber").getName());
  }

  @Test
  public void checkGetPropertyIsNullable() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    // In case nullable = true, nullable is not past to $metadata, as this is the default
    assertTrue(ct.getEdmItem().getProperty("POBox").isNullable());
  }

  @Test
  public void checkGetAllNaviProperties() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertEquals(1, ct.getEdmItem().getNavigationProperties().size(), "Wrong number of properties");
  }

  @Test
  public void checkGetNaviPropertyByNameNotNull() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
  }

  @Test
  public void checkGetNaviPropertyByNameRightEntity() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertEquals("AdministrativeDivision", ct.getEdmItem().getNavigationProperty("AdministrativeDivision").getName());
  }

  @Disabled("countryName is currently commented out")
  @Test
  public void checkGetDescriptionPropertyManyToOne() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("CountryName"));
  }

  @Disabled("regionName is currently commented out")
  @Test
  public void checkGetDescriptionPropertyManyToMany() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("RegionName"));
  }

  @Disabled("countryName is currently commented out")
  @Test
  public void checkDescriptionPropertyType() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    ct.getEdmItem();
    assertTrue(ct.getAttribute("countryName") instanceof IntermediateProperty);
  }

  @Test
  public void checkGetPropertyOfNestedComplexType() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "AdministrativeInformation"),
        serviceDocument);
    assertNotNull(ct.getPath("Created/By"));
  }

  @Test
  public void checkGetPropertyDBName() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "PostalAddressData"),
        serviceDocument);
    assertEquals("\"Address.PostOfficeBox\"", ((JPAAttributePath) ct.getPath("POBox")).getDBFieldName());
  }

  @Test
  public void checkGetPropertyDBNameOfNestedComplexType() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "AdministrativeInformation"),
        serviceDocument);
    assertEquals("\"by\"", ((JPAAttributePath) ct.getPath("Created/By")).getDBFieldName());
  }

  @Test
  public void checkGetPropertyWithComplexType() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        "AdministrativeInformation"),
        serviceDocument);
    assertNotNull(ct.getEdmItem().getProperty("Created"));
  }

  @Test
  public void checkGetPropertiesWithSameComplexTypeNotEqual() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME), getEmbeddedableType(
        AdministrativeInformation.class.getSimpleName()),
        serviceDocument);
    assertNotEquals(ct.getEdmItem().getProperty("Created"), ct.getEdmItem().getProperty("Updated"));
    assertNotEquals(ct.getAttribute("created"), ct.getAttribute("updated"));
  }

  @Test
  public void checkComplexTypeWithAsIsAttributeNames() throws ODataJPAModelException {
    final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(new JPAEdmNameBuilder(PUNIT_NAME),
        getEmbeddedableType(
            Phone.class.getSimpleName()),
        serviceDocument);
    assertTrue(ct.getAttribute("phoneNumber").getExternalName().equals("phoneNumber"));
  }

}
