package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.apache.olingo.commons.api.edm.geo.MultiLineString;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmGeospatial;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

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
    ThrowingRunnable throwingRunnable = () -> TypeMapping.convertToEdmSimpleType(MultiLineString.class);
    assertThrows(ODataJPAModelException.class, throwingRunnable);
    throwingRunnable = () -> TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geographyMLSUnsupportedWithoutAnnotation"));
    assertThrows(ODataJPAModelException.class, throwingRunnable);

    assertEquals(EdmPrimitiveTypeKind.GeographyMultiLineString, TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geographyMLS")));
    assertEquals(EdmPrimitiveTypeKind.GeometryMultiLineString, TypeMapping.convertToEdmSimpleType(GeospatialTest.class
        .getDeclaredField("geometryMLS")));
  }

}
