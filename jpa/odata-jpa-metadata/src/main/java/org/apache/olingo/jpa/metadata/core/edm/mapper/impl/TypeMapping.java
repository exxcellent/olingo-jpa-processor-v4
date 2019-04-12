package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

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

	private final static Map<Class<?>, EdmPrimitiveTypeKind> MAPPING_JPA2ODATA = new HashMap<>();

	static {
		// global definition for trivial mappings without additional logic

		// JPA -> ODATA (n:1 mapping)
		MAPPING_JPA2ODATA.put(String.class, EdmPrimitiveTypeKind.String);
		MAPPING_JPA2ODATA.put(Character.class, EdmPrimitiveTypeKind.String);
		MAPPING_JPA2ODATA.put(char.class, EdmPrimitiveTypeKind.String);
		MAPPING_JPA2ODATA.put(char[].class, EdmPrimitiveTypeKind.String);
		MAPPING_JPA2ODATA.put(Character[].class, EdmPrimitiveTypeKind.String);

		MAPPING_JPA2ODATA.put(Long.class, EdmPrimitiveTypeKind.Int64);
		MAPPING_JPA2ODATA.put(long.class, EdmPrimitiveTypeKind.Int64);

		MAPPING_JPA2ODATA.put(Short.class, EdmPrimitiveTypeKind.Int16);
		MAPPING_JPA2ODATA.put(short.class, EdmPrimitiveTypeKind.Int16);

		MAPPING_JPA2ODATA.put(Integer.class, EdmPrimitiveTypeKind.Int32);
		MAPPING_JPA2ODATA.put(int.class, EdmPrimitiveTypeKind.Int32);

		MAPPING_JPA2ODATA.put(Double.class, EdmPrimitiveTypeKind.Double);
		MAPPING_JPA2ODATA.put(double.class, EdmPrimitiveTypeKind.Double);

		MAPPING_JPA2ODATA.put(Float.class, EdmPrimitiveTypeKind.Single);
		MAPPING_JPA2ODATA.put(float.class, EdmPrimitiveTypeKind.Single);

		MAPPING_JPA2ODATA.put(BigDecimal.class, EdmPrimitiveTypeKind.Decimal);

		MAPPING_JPA2ODATA.put(Byte[].class, EdmPrimitiveTypeKind.Binary);
		MAPPING_JPA2ODATA.put(byte[].class, EdmPrimitiveTypeKind.Binary);

		MAPPING_JPA2ODATA.put(Byte.class, EdmPrimitiveTypeKind.SByte);
		MAPPING_JPA2ODATA.put(byte.class, EdmPrimitiveTypeKind.SByte);

		MAPPING_JPA2ODATA.put(Boolean.class, EdmPrimitiveTypeKind.Boolean);
		MAPPING_JPA2ODATA.put(boolean.class, EdmPrimitiveTypeKind.Boolean);

		MAPPING_JPA2ODATA.put(java.time.LocalTime.class, EdmPrimitiveTypeKind.TimeOfDay);
		MAPPING_JPA2ODATA.put(java.sql.Time.class, EdmPrimitiveTypeKind.TimeOfDay);

		MAPPING_JPA2ODATA.put(java.time.LocalDate.class, EdmPrimitiveTypeKind.Date);
		MAPPING_JPA2ODATA.put(java.sql.Date.class, EdmPrimitiveTypeKind.Date);

		MAPPING_JPA2ODATA.put(java.time.Duration.class, EdmPrimitiveTypeKind.Duration);

		MAPPING_JPA2ODATA.put(java.time.Year.class, EdmPrimitiveTypeKind.Int16);

		MAPPING_JPA2ODATA.put(java.time.LocalDateTime.class, EdmPrimitiveTypeKind.DateTimeOffset);

		MAPPING_JPA2ODATA.put(UUID.class, EdmPrimitiveTypeKind.Guid);

		// ODATA -> JPA mapping is handled by separate logic to manage 1:n problem for
		// ambiguous data type mappings

	}

	public static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> type) throws ODataJPAModelException {
		return convertToEdmSimpleType(type, (AccessibleObject) null);
	}

	public static EdmPrimitiveTypeKind convertToEdmSimpleType(final Field field) throws ODataJPAModelException {
		return convertToEdmSimpleType(field.getType(), field);
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

	public static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> jpaType,
	        final Attribute<?, ?> currentAttribute) throws ODataJPAModelException {
		return convertToEdmSimpleType(jpaType,
		        (currentAttribute != null) ? (AccessibleObject) currentAttribute.getJavaMember() : null);
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
	private static EdmPrimitiveTypeKind convertToEdmSimpleType(final Class<?> jpaType,
	        final AccessibleObject javaMember) throws ODataJPAModelException {
		// use a converter if available
		EdmPrimitiveTypeKind simpleType = determineSimpleTypeFromConverter(javaMember);
		if (simpleType != null) {
			return simpleType;
		}

		simpleType = MAPPING_JPA2ODATA.get(jpaType);
		if (simpleType != null) {
			return simpleType;
		}
		// determine mappings with more logic...
		final String memberName = (javaMember instanceof Field) ? ((Field) javaMember).getName() : null;
		if (jpaType.equals(java.util.Calendar.class) || jpaType.equals(java.sql.Timestamp.class)
		        || jpaType.equals(java.util.Date.class)) {
			return mapTemporalType(javaMember);
		} else if (jpaType.equals(UUID.class)) {
			return EdmPrimitiveTypeKind.Guid;
		} else if (jpaType.equals(Byte[].class)) {
			return EdmPrimitiveTypeKind.Binary;
		} else if (jpaType.equals(Blob.class) && isBlob(javaMember)) {
			return EdmPrimitiveTypeKind.Binary;
		} else if (jpaType.equals(Clob.class) && isBlob(javaMember)) {
			return EdmPrimitiveTypeKind.String;
		} else if (isGeography(javaMember)) {
			return convertGeography(jpaType, memberName);
		} else if (isGeometry(javaMember)) {
			return convertGeometry(jpaType, memberName);
		}
		// Be aware: enumerations are handled as primitive types, but must be converted
		// as own (enum) type and so cannot be handled here

		// Type (%1$s) of attribute (%2$s) is not supported. Mapping not possible
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
		        jpaType.getName(), (javaMember != null ? memberName : null));
	}

	private static EdmPrimitiveTypeKind determineSimpleTypeFromConverter(final AnnotatedElement javaMember)
	        throws ODataJPAModelException {
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

	public static EdmPrimitiveTypeKind convertToEdmSimpleType(final JPATypedElement attribute)
	        throws ODataJPAModelException {
		return convertToEdmSimpleType(attribute.getType(),
		        IntermediateProperty.class.isInstance(attribute)
		                ? (AccessibleObject) IntermediateProperty.class.cast(attribute).getJavaMember()
		                : null);
	}

	public static boolean isScalarType(final Class<?> type) {
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
		        type == java.time.LocalTime.class ||
		        type == java.sql.Time.class ||
		        type == java.time.Duration.class ||
		        type == java.time.LocalDate.class ||
		        type == java.time.Year.class ||
		        type == java.sql.Date.class ||
		        type == java.util.Calendar.class || type == java.sql.Timestamp.class ||
		        type == java.util.Date.class ||
		        type == UUID.class) {
			return true;
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
		if (javaMember != null) {
			final Temporal temporal = javaMember.getAnnotation(Temporal.class);
			if (temporal != null) {
				switch (temporal.value()) {
				case TIME:
					return EdmPrimitiveTypeKind.TimeOfDay;
				case DATE:
					return EdmPrimitiveTypeKind.Date;
				default:
					return EdmPrimitiveTypeKind.DateTimeOffset;
				}
			}
		}
		// default
		return EdmPrimitiveTypeKind.DateTimeOffset;
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

	private static boolean isBlob(final AnnotatedElement javaMember) {
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

	private static boolean isCollectionTypeOfPrimitive(final Field field) throws ODataJPAModelException {
		if (Collection.class.isAssignableFrom(field.getType())) {
			final Class<?> type = extractElementTypeOfCollection(field);
			return (convertToEdmSimpleType(type, field) != null);
		}
		return false;
	}

	/**
	 *
	 * @return TRUE if the given JPA attribute describes an attribute with any
	 *         collection type with any primitive type as element type in the
	 *         collection. Example: <code>Set<String</code>
	 */
	static boolean isCollectionTypeOfPrimitive(final Attribute<?, ?> currentAttribute) {
		if (currentAttribute instanceof PluralAttribute) {
			final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) currentAttribute;
			try {
				final EdmPrimitiveTypeKind kind = convertToEdmSimpleType(pa.getElementType().getJavaType(),
				        (AccessibleObject) currentAttribute.getJavaMember());
				return (kind != null);
			} catch (final ODataJPAModelException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 *
	 * @return TRUE if the given JPA attribute describes an attribute with any
	 *         collection type with and a complex element type, marked as
	 *         {@link Embeddable @Embeddable} in the collection. Example:
	 *         <code>Set<String</code>
	 */
	static boolean isCollectionTypeOfEmbeddable(final Attribute<?, ?> currentAttribute) {
		if (currentAttribute instanceof PluralAttribute) {
			final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) currentAttribute;
			return EmbeddableType.class.isInstance(pa.getElementType());
		}
		return false;
	}

	/**
	 *
	 * @return TRUE if the given JPA attribute describes an attribute that can be
	 *         handled as primitive type. Example: <code>String</code>
	 *
	 * @see #isPrimitiveType(Type)
	 */
	static boolean isPrimitiveType(final Attribute<?, ?> currentAttribute) {
		if (currentAttribute instanceof SingularAttribute) {
			try {
				final EdmPrimitiveTypeKind kind = convertToEdmSimpleType(currentAttribute.getJavaType(),
				        (AccessibleObject) currentAttribute.getJavaMember());
				return (kind != null);
			} catch (final ODataJPAModelException e) {
				return false;
			}
		}
		return false;
	}

	static boolean isPrimitiveType(final Field field) {
		try {
			if (isCollectionTypeOfPrimitive(field)) {
				return true;
			}
			return (convertToEdmSimpleType(field) != null);
		} catch (final ODataJPAModelException e) {
			return false;
		}
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
		} else
			javaType = field.getType();
		if (javaType == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
			        "Java type not available");
		}
		return javaType.getAnnotation(ODataDTO.class) != null;
	}

}
