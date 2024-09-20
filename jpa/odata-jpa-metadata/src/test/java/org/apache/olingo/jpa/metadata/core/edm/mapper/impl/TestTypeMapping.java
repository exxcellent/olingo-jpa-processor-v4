package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.apache.olingo.commons.api.edm.geo.MultiLineString;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmGeospatial;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.junit.jupiter.api.Test;

public class TestTypeMapping extends TestMappingRoot {

  private static class GeospatialTest {

    @SuppressWarnings("unused")
    private MultiLineString geographyMLSUnsupportedWithoutAnnotation;

    @EdmGeospatial(dimension = Dimension.GEOGRAPHY)
    private MultiLineString geographyMLS;

    @EdmGeospatial(dimension = Dimension.GEOMETRY)
    private MultiLineString geometryMLS;
  }

  @Test
  public void checkGeometryMappings() throws ODataJPAModelException, NoSuchFieldException, SecurityException {

    // Geo types must have an annotation to qualify as Geometry or Geography, so without annotation (on a field) a type
    // mapping must fail
    assertThrows(ODataJPAModelException.class, () -> TypeMapping.convertToEdmSimpleType(MultiLineString.class));
    assertThrows(ODataJPAModelException.class, () -> TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geographyMLSUnsupportedWithoutAnnotation")));

    assertEquals(EdmPrimitiveTypeKind.GeographyMultiLineString, TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geographyMLS")));
    assertEquals(EdmPrimitiveTypeKind.GeometryMultiLineString, TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geometryMLS")));
  }

}
