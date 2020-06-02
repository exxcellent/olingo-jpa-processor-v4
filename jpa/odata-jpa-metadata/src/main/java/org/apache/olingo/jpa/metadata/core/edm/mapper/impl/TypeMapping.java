package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.PluralAttribute;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmGeospatial;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * This class holds utility methods for type conversions between JPA Java types and OData Types.
 *
 */
public final class TypeMapping {

  private final static List<ODataMapping> MAPPINGS_JPA2ODATA = new LinkedList<>();

  private static class ODataMapping {
    private final EdmPrimitiveTypeKind kind;
    private final Class<?> odataRepresentationType;
    private final Class<?>[] jpaTypesToMap;

    /**
     * {@link org.apache.olingo.commons.api.edm.EdmPrimitiveType#getDefaultType() EdmPrimitiveType::getDefaultType()}
     * will not always announce the best/real used data type. As example: With Olingo 4.7.x
     * {@link org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset EdmDateTimeOffset} will offer
     * {@link java.sql.Timestamp} as default but internal {@link java.time.ZonedDateTime} is used for conversion. So we
     * have to define an mapping for data types ignoring the Olingo part.
     *
     * @param odataRepresentationType The type used for value representation while serialization in Olingo as OData
     * entity property.
     * @param odataKind The OData primitive kind
     * @param jpaTypes The data types used while modelling of JPA entities to map to <i>odataKind</i> (and
     * <i>odataRepresentationType</i>)
     */
    public ODataMapping(final Class<?> odataRepresentationType, final EdmPrimitiveTypeKind odataKind,
        final Class<?>... jpaTypes) {
      this.kind = odataKind;
      this.odataRepresentationType = odataRepresentationType;
      if (jpaTypes == null || jpaTypes.length < 1) {
        throw new IllegalStateException("At least one JPA type required");
      }
      jpaTypesToMap = jpaTypes;
    }

    /**
     *
     * @return TRUE if given type is one of the java JPA types assigned to this mapping.
     */
    public boolean isMatchingJPAType(final Class<?> iType) {
      for (final Class<?> t : jpaTypesToMap) {
        if (t.isAssignableFrom(iType)) {
          return true;
        }
      }
      return false;
    }
  }

  static {
    // global definition for trivial mappings without additional logic

    // JPA -> ODATA (n:1 mapping)
    MAPPINGS_JPA2ODATA.add(new ODataMapping(String.class, EdmPrimitiveTypeKind.String, String.class, Character.class,
        char.class, char[].class, Character[].class, Clob.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Long.class, EdmPrimitiveTypeKind.Int64, Long.class, long.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Short.class, EdmPrimitiveTypeKind.Int16, Short.class, short.class,
        java.time.Year.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Integer.class, EdmPrimitiveTypeKind.Int32, Integer.class, int.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Double.class, EdmPrimitiveTypeKind.Double, Double.class, double.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Float.class, EdmPrimitiveTypeKind.Single, Float.class, float.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(BigDecimal.class, EdmPrimitiveTypeKind.Decimal, BigDecimal.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(byte[].class, EdmPrimitiveTypeKind.Binary, Byte[].class, byte[].class,
        java.io.InputStream.class, Blob.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Byte.class, EdmPrimitiveTypeKind.SByte, Byte.class, byte.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(Boolean.class, EdmPrimitiveTypeKind.Boolean, Boolean.class, boolean.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(java.time.LocalTime.class, EdmPrimitiveTypeKind.TimeOfDay,
        java.time.LocalTime.class, java.sql.Time.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(java.time.LocalDate.class, EdmPrimitiveTypeKind.Date,
        java.time.LocalDate.class, java.sql.Date.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(ZonedDateTime.class, EdmPrimitiveTypeKind.DateTimeOffset,
        java.time.LocalDateTime.class, java.sql.Timestamp.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(BigDecimal.class, EdmPrimitiveTypeKind.Duration, java.time.Duration.class));
    MAPPINGS_JPA2ODATA.add(new ODataMapping(UUID.class, EdmPrimitiveTypeKind.Guid, UUID.class));

    // validate mapping (unique EdmPrimitiveTypeKind entries + unique JPA class entries)
    final Set<EdmPrimitiveTypeKind> setKinds = new HashSet<>();
    final Set<Class<?>> setJPAClasses = new HashSet<>();
    for (final ODataMapping mapping : MAPPINGS_JPA2ODATA) {
      if (setKinds.contains(mapping.kind)) {
        throw new IllegalStateException("invalid mapping declaration: multiple primitive types for " + mapping.kind);
      }
      setKinds.add(mapping.kind);
      for (final Class<?> e : mapping.jpaTypesToMap) {
        if (setJPAClasses.contains(e)) {
          throw new IllegalStateException("invalid mapping declaration: multiple class entries for " + mapping.kind
              + "->" + e.getCanonicalName());
        }
        setJPAClasses.add(e);
      }
    }

    // ODATA -> JPA mapping is handled by separate logic to manage 1:n problem for
    // ambiguous data type mappings

  }

  public static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> type) throws ODataJPAModelException {
    return convertToEdmSimpleType(type, (AccessibleObject) null);
  }

  /**
   *
   * @param field If field is a collection attribute then the {@link #extractElementTypeOfCollection(Field) embedded
   * type} will be detected
   */
  public static EdmPrimitiveTypeKind convertToEdmSimpleType(final Field field) throws ODataJPAModelException {
    final Class<?> javaType;
    if (Collection.class.isAssignableFrom(field.getType())) {
      javaType = extractElementTypeOfCollection(field);
    } else {
      javaType = field.getType();
    }
    return convertToEdmSimpleType(javaType, field);
  }

  /**
   * This utility method converts a given jpa Type to equivalent EdmPrimitiveTypeKind for maintaining compatibility
   * between Java and OData Types.
   *
   * @param jpaType
   *            The JPA Type input.
   * @return The corresponding EdmPrimitiveTypeKind.
   * @throws ODataJPAModelException
   * @throws org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPARuntimeException
   *
   * @see EdmPrimitiveTypeKind
   */

  static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> jpaType,
      final Attribute<?, ?> currentAttribute) throws ODataJPAModelException {
    return convertToEdmSimpleType(jpaType,
        (currentAttribute != null) ? (AccessibleObject) currentAttribute.getJavaMember() : null);
  }

  private static ODataMapping determineODataMapping(final Class<?> jpaType, final AccessibleObject javaMember)
      throws ODataJPAModelException {
    final EdmPrimitiveTypeKind customKind = determineSimpleTypeFromConverter(javaMember);
    if (customKind != null) {
      // find mapping based on kind
      for (final ODataMapping mapping : MAPPINGS_JPA2ODATA) {
        if (mapping.kind == customKind) {
          return mapping;
        }
      }
    }

    // special handling for types with additional annotations determining the final kind
    final EdmPrimitiveTypeKind temporalKind;
    final String memberName = (javaMember instanceof Field) ? ((Field) javaMember).getName() : null;
    if (java.util.Date.class.isAssignableFrom(jpaType) || java.util.Calendar.class.isAssignableFrom(jpaType)) {
      temporalKind = mapTemporalType(javaMember);
    } else if (isGeography(javaMember)) {
      temporalKind = convertGeography(jpaType, memberName);
    } else if (isGeometry(javaMember)) {
      temporalKind = convertGeometry(jpaType, memberName);
    } else {
      temporalKind = null;
    }
    if (temporalKind != null) {
      // find mapping based on kind
      for (final ODataMapping mapping : MAPPINGS_JPA2ODATA) {
        if (mapping.kind == temporalKind) {
          return mapping;
        }
      }
    }

    // normal lookup
    final List<ODataMapping> matchingMappings = new LinkedList<>();
    for (final ODataMapping mapping : MAPPINGS_JPA2ODATA) {
      if (mapping.isMatchingJPAType(jpaType)) {
        matchingMappings.add(mapping);
      }
    }
    if (matchingMappings.size() == 1) {
      return matchingMappings.get(0);
    } else if (matchingMappings.size() > 1) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
          "More than one mapping is available to define mapping from JPA type to OData type for " + jpaType
          .getCanonicalName());
    }

    // fallback handling for several types...
    if (java.util.Date.class == jpaType || java.util.Calendar.class == jpaType) {
      // java.util.Date is very ambiguous so we only define a mapping here on demand
      return new ODataMapping(java.time.LocalDateTime.class, EdmPrimitiveTypeKind.DateTimeOffset, java.util.Date.class);
    }
    return null;
  }

  /**
   *
   * @param jpaType
   *            The class object to convert into a primitive kind.
   * @param javaMember
   *            The optional java member ({@link Field} or {@link Method}) using
   *            the given type as field type or return type of method.
   * @return The primitive OData type.
   * @throws ODataJPAModelException
   *             If given type cannot be converted into a primitive type.
   */
  static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> jpaType,
      final AccessibleObject javaMember) throws ODataJPAModelException {

    final ODataMapping mapping = determineODataMapping(jpaType, javaMember);
    if (mapping != null) {
      return mapping.kind;
    }

    // Be aware: enumerations are handled as primitive types, but must be converted
    // as own (enum) type and so cannot be handled here

    // Type (%1$s) of attribute (%2$s) is not supported. Mapping not possible
    final String memberName = (javaMember instanceof Field) ? ((Field) javaMember).getName() : null;
    throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
        jpaType.getName(), (javaMember != null ? memberName : null));
  }

  private static EdmPrimitiveTypeKind determineSimpleTypeFromConverter(final AnnotatedElement javaMember) {
    if (javaMember == null) {
      return null;
    }
    final EdmAttributeConversion converterAnnotation = javaMember
        .getAnnotation(EdmAttributeConversion.class);
    if (converterAnnotation == null) {
      return null;
    }
    return converterAnnotation.odataType();
  }

  /**
   * @param jpaType The JPA type to find the OData representation for.
   * @return The java class representing the OData type of Olingo or <code>null</code> if no match was found.
   * @throws ODataJPAModelException For multiple mappings
   */
  public static Class<?> determineODataRepresentationtype(final JPATypedElement attribute)
      throws IllegalStateException, ODataJPAModelException {
    final ODataMapping mapping = determineODataMapping(attribute.getType(), IntermediateProperty.class.isInstance(
        attribute)
        ? (AccessibleObject) IntermediateProperty.class.cast(attribute).getJavaMember()
            : null);
    if (mapping != null) {
      return mapping.odataRepresentationType;
    }
    return null;
  }

  public static EdmPrimitiveTypeKind convertToEdmSimpleType(final JPATypedElement attribute)
      throws ODataJPAModelException {
    return convertToEdmSimpleType(attribute.getType(),
        IntermediateProperty.class.isInstance(attribute)
        ? (AccessibleObject) IntermediateProperty.class.cast(attribute).getJavaMember()
            : null);
  }

  /**
   * @deprecated This method cannot known all the possible primitive types
   */
  @Deprecated
  public static boolean isPrimitiveType(final Class<?> type) {
    if (type.isEnum()) {
      return true;
    }
    // TODO map to convertToEdmSimpleType(..)
    if (type == String.class ||
        type == Character.class ||
        type == Long.class ||
        type == Short.class ||
        type == Integer.class ||
        type == Double.class ||
        type == Float.class ||
        type == BigDecimal.class ||
        type == Byte.class ||
        type == Boolean.class ||
        type == java.time.Duration.class ||
        java.time.temporal.Temporal.class.isAssignableFrom(type) ||
        type == java.util.Calendar.class ||
        type == java.util.Date.class ||
        type == UUID.class) {
      return true;
    }
    //primitive are also (currently) all types where we have a mapping for
    for (final ODataMapping mapping : MAPPINGS_JPA2ODATA) {
      if (mapping.isMatchingJPAType(type)) {
        return true;
      }
    }
    return false;
  }

  private static EdmPrimitiveTypeKind convertGeography(final Class<?> jpaType, final String memberName)
      throws ODataJPAModelException {
    if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.Point.class)) {
      return EdmPrimitiveTypeKind.GeographyPoint;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiPoint.class)) {
      return EdmPrimitiveTypeKind.GeographyMultiPoint;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.LineString.class)) {
      return EdmPrimitiveTypeKind.GeographyLineString;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiLineString.class)) {
      return EdmPrimitiveTypeKind.GeographyMultiLineString;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.Polygon.class)) {
      return EdmPrimitiveTypeKind.GeographyPolygon;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiPolygon.class)) {
      return EdmPrimitiveTypeKind.GeographyMultiPolygon;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.GeospatialCollection.class)) {
      return EdmPrimitiveTypeKind.GeographyCollection;
    }
    // Type (%1$s) of attribute (%2$s) is not supported. Mapping not possible
    throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
        jpaType.getName(), memberName);
  }

  private static EdmPrimitiveTypeKind convertGeometry(final Class<?> jpaType, final String memberName)
      throws ODataJPAModelException {
    if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.Point.class)) {
      return EdmPrimitiveTypeKind.GeometryPoint;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiPoint.class)) {
      return EdmPrimitiveTypeKind.GeometryMultiPoint;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.LineString.class)) {
      return EdmPrimitiveTypeKind.GeometryLineString;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiLineString.class)) {
      return EdmPrimitiveTypeKind.GeometryMultiLineString;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.Polygon.class)) {
      return EdmPrimitiveTypeKind.GeometryPolygon;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.MultiPolygon.class)) {
      return EdmPrimitiveTypeKind.GeometryMultiPolygon;
    } else if (jpaType.equals(org.apache.olingo.commons.api.edm.geo.GeospatialCollection.class)) {
      return EdmPrimitiveTypeKind.GeometryCollection;
    }
    // Type (%1$s) of attribute (%2$s) is not supported. Mapping not possible
    throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
        jpaType.getName(), memberName);
  }

  private static EdmPrimitiveTypeKind mapTemporalType(final AnnotatedElement javaMember) {
    if (javaMember == null) {
      return null;
    }
    final Temporal temporal = javaMember.getAnnotation(Temporal.class);
    if (temporal == null) {
      return null;
    }
    switch (temporal.value()) {
    case TIME:
      return EdmPrimitiveTypeKind.TimeOfDay;
    case DATE:
      return EdmPrimitiveTypeKind.Date;
    default:
      return EdmPrimitiveTypeKind.DateTimeOffset;
    }
  }

  private static Dimension getDimension(final AnnotatedElement javaMember) {
    if (AnnotatedElement.class.isInstance(javaMember)) {
      final EdmGeospatial spatialDetails = javaMember.getAnnotation(EdmGeospatial.class);
      if (spatialDetails != null) {
        return spatialDetails.dimension();
      }
    }
    return null;
  }

  @SuppressWarnings("unused")
  private static boolean isLargeObject(final AnnotatedElement javaMember) {
    if (javaMember != null && javaMember.getAnnotation(Lob.class) != null) {
      return true;
    }
    return false;
  }

  private static boolean isGeography(final AnnotatedElement javaMember) {
    return getDimension(javaMember) == Dimension.GEOGRAPHY ? true : false;
  }

  private static boolean isGeometry(final AnnotatedElement javaMember) {
    return getDimension(javaMember) == Dimension.GEOMETRY ? true : false;
  }

  /**
   *
   * @return TRUE if the given JPA attribute describes an attribute with any
   *         collection type with and a complex element type, marked as
   *         {@link Embeddable @Embeddable} in the collection. Example:
   *         <code>Set<String</code>
   */
  static boolean isEmbeddableTypeCollection(final Attribute<?, ?> currentAttribute) {
    if (currentAttribute instanceof PluralAttribute) {
      final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) currentAttribute;
      return EmbeddableType.class.isInstance(pa.getElementType());
    }
    return false;
  }

  /**
   *
   * @param field
   *            The field assuming to be an collection type.
   * @return The extracted element type from wrapping collection type
   * @throws ODataJPAModelException
   *             If the given field is not of (supported) collection type.
   */
  static Class<?> extractElementTypeOfCollection(final Field field) throws ODataJPAModelException {
    if (ParameterizedType.class.isInstance(field.getGenericType())) {
      final java.lang.reflect.Type[] types = ((ParameterizedType) field.getGenericType())
          .getActualTypeArguments();
      if (types.length == 1) {
        return (Class<?>) types[0];
      }
    }
    throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
        field.getGenericType().getTypeName(), field.getName());
  }

  static boolean isFieldTargetingDTO(final Field field) throws ODataJPAModelException {
    Class<?> javaType;
    if (Collection.class.isAssignableFrom(field.getType())) {
      // javaType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
      javaType = TypeMapping.extractElementTypeOfCollection(field);
    } else {
      javaType = field.getType();
    }
    if (javaType == null) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
          "Java type not available");
    }
    return javaType.getAnnotation(ODataDTO.class) != null;
  }

}
