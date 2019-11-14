package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.GeneratedValue;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDateTime2SqlTimestampODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalTime2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.SqlDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.SqlTime2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.UtilDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.server.api.ODataApplicationException;

public abstract class AbstractConverter {

  private static class ConverterMapping {

    private final Class<?> odataAttributeType;
    private final Class<?> jpaAttributeType;
    private final boolean matchSubClasses4JPA;
    private final ODataAttributeConverter<Object, Object> converterInstance;

    @SuppressWarnings("unchecked")
    ConverterMapping(final Class<?> odataAttributeType, final Class<?> jpaAttributeType,
        final boolean matchSubClassesForJPA,
        @SuppressWarnings("rawtypes") final ODataAttributeConverter converterInstance) {
      this.odataAttributeType = odataAttributeType;
      this.jpaAttributeType = jpaAttributeType;
      this.matchSubClasses4JPA = matchSubClassesForJPA;
      this.converterInstance = converterInstance;
    }

    ODataAttributeConverter<Object, Object> getConverterInstance() {
      return converterInstance;
    }

    boolean isMatching(final Class<?> odataAttributeType, final Class<?> jpaAttributeType) {
      if (!this.odataAttributeType.isAssignableFrom(odataAttributeType)) {
        return false;
      }
      if (matchSubClasses4JPA) {
        return this.jpaAttributeType.isAssignableFrom(jpaAttributeType);
      } else {
        return this.jpaAttributeType.equals(jpaAttributeType);
      }
    }
  }

  private final static Collection<ConverterMapping> DEFAULT_ODATA_ATTRIBUTE_CONVERTERS = new LinkedList<>();

  static {
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class,
        java.sql.Date.class, false,
        new SqlDate2UtilCalendarODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class,
        java.sql.Time.class, false,
        new SqlTime2UtilCalendarODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class, java.util.Date.class,
        false, new UtilDate2UtilCalendarODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS
    .add(new ConverterMapping(java.util.Calendar.class, java.time.LocalDate.class, true,
        new LocalDate2UtilCalendarODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.util.Calendar.class,
        java.time.LocalTime.class, true, new LocalTime2UtilCalendarODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.sql.Timestamp.class,
        java.time.LocalDateTime.class, true, new LocalDateTime2SqlTimestampODataAttributeConverter()));
  }

  protected final Logger log = Logger.getLogger(AbstractConverter.class.getName());
  @SuppressWarnings("rawtypes")
  private final Map<String, ODataAttributeConverter> converterLookupCache = new HashMap<>();

  private static String buildConverterKey(final Class<?> odataAttributeType, final Class<?> jpaAttributeType) {
    return odataAttributeType.getName().concat("<->").concat(jpaAttributeType.getName());
  }

  /**
   * Look for any matching converter, including default implementation for some
   * data type combinations.
   *
   * @param jpaElement
   *            The attribute to look for an assigned converter.
   *
   * @return A found converter or <code>null</code> if no converter is available.
   */
  @SuppressWarnings("unchecked")
  protected final ODataAttributeConverter<Object, Object> determineODataAttributeConverter(final JPATypedElement jpaElement,
      final Class<?> odataAttributeType) throws ODataJPAConversionException {
    final EdmAttributeConversion annoConverter = jpaElement.getAnnotation(EdmAttributeConversion.class);
    if (annoConverter != null) {
      try {
        if (!EdmAttributeConversion.DEFAULT.class.equals(annoConverter.converter())) {
          return (ODataAttributeConverter<Object, Object>) annoConverter.converter().newInstance();
        }
      } catch (InstantiationException | IllegalAccessException e) {
        throw new ODataJPAConversionException(e, ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM, e.getMessage());
      }
    }
    // look for default converter
    return determineDefaultODataAttributeConverter(jpaElement.getType(), odataAttributeType);
  }

  @SuppressWarnings("unchecked")
  private ODataAttributeConverter<Object, Object> determineDefaultODataAttributeConverter(
      final Class<?> jpaAttributeType,
      final Class<?> odataAttributeType) {
    final String key = buildConverterKey(odataAttributeType, jpaAttributeType);
    if (converterLookupCache.containsKey(key)) {
      return converterLookupCache.get(key);
    }
    // lookup...
    final List<ODataAttributeConverter<Object, Object>> matchingConverters = new LinkedList<>();
    for (final ConverterMapping mapping : DEFAULT_ODATA_ATTRIBUTE_CONVERTERS) {
      if (!mapping.isMatching(odataAttributeType, jpaAttributeType)) {
        continue;
      }
      matchingConverters.add(mapping.getConverterInstance());
    }
    if (matchingConverters.size() > 1) {
      log.log(Level.WARNING, "Multiple default converters are matching for " + odataAttributeType.getSimpleName() + "<->"
          + jpaAttributeType.getSimpleName() + ". Will NOT use any of them!");
      return null;
    }
    if (matchingConverters.isEmpty()) {
      converterLookupCache.put(key, null);
      return null;
    }
    final ODataAttributeConverter<Object, Object> converter = matchingConverters.get(0);
    converterLookupCache.put(key, converter);
    return converter;
  }

  /**
   *
   * @param attribute
   *            The metamodel attribute
   * @param jpaValue
   *            The value coming from JPA (aka database)
   * @return The value converted into an type provided from OData
   */
  private Object convertJPA2ODataPrimitiveValue(final JPATypedElement attribute, final Object jpaValue)
      throws ODataJPAConversionException, ODataJPAModelException {

    // use a intermediate conversion to an supported JAVA type in the Olingo library
    final EdmPrimitiveTypeKind kind = TypeMapping.convertToEdmSimpleType(attribute);
    final Class<?> oadataType = EdmPrimitiveTypeFactory.getInstance(kind).getDefaultType();

    final Class<?> javaType = attribute.getType();
    if (javaType.equals(oadataType)) {
      return jpaValue;
    }
    final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(attribute,
        oadataType);
    if (converter != null) {
      return converter.convertToOData(jpaValue);
    }
    // use 'as is' without conversion
    return jpaValue;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected final Property convertJPA2ODataProperty(final JPATypedElement attribute, final String propertyName,
      final Object input,
      final List<Property> properties) throws ODataJPAModelException, ODataJPAConversionException, IllegalArgumentException {
    if (attribute == null) {
      throw new IllegalArgumentException("JPA attribute required for property " + propertyName);
    }
    ValueType valueType = null;
    final Class<?> javaType = attribute.getType();
    if (javaType == null) {
      throw new IllegalArgumentException("Java type required for property " + propertyName);
    }
    if (javaType.isEnum() /* && input != null && !Number.class.isInstance(input) */) {
      if (attribute.isCollection()) {
        valueType = ValueType.COLLECTION_ENUM;
      } else {
        valueType = ValueType.ENUM;
      }
    } else if (attribute.isCollection() && attribute.isPrimitive()) {
      valueType = ValueType.COLLECTION_PRIMITIVE;
    } else if (attribute.isPrimitive()) {
      valueType = ValueType.PRIMITIVE;
    } else {
      throw new IllegalArgumentException(
          "Given value is not of primitive type managable in this method: " + propertyName);
    }
    if (valueType == null) {
      throw new IllegalArgumentException(
          "Given value is not of primitive type managable in this method for property " + propertyName);
    }
    Property property;
    Object convertedValue;
    switch (valueType) {
    case ENUM:
      // for OData we have to convert the value into a number (if not yet)
      convertedValue = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
      property = new Property(null, propertyName);
      property.setValue(valueType, convertedValue);
      properties.add(property);
      return property;
    case COLLECTION_ENUM:
      property = findOrCreateProperty(properties, propertyName);
      convertedValue = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
      if (property.getValueType() == null) {
        // new created property
        final List<Object> list = new LinkedList<>();
        list.add(convertedValue);
        property.setValue(valueType, list);
      } else {
        // add value to existing property
        addSingleValueIfUnique((List) property.getValue(), convertedValue);
      }
      return property;
    case COLLECTION_PRIMITIVE:
      property = findOrCreateProperty(properties, propertyName);
      if (property.getValueType() == null) {
        // new created property
        final List<Object> list = new LinkedList<>();
        if (input != null) {
          if (input instanceof Collection<?>) {
            addCollectionValuesIfUnique(list, (Collection) input);
          } else {
            addSingleValueIfUnique(list, input);
          }
        }
        property.setValue(valueType, list);
      } else {
        // add value to existing property
        if (input instanceof Collection<?>) {
          addCollectionValuesIfUnique((List) property.getValue(), (Collection) input);
        } else {
          addSingleValueIfUnique((List) property.getValue(), input);
        }
      }
      return property;
    default:
      // handle as primitive
      convertedValue = convertJPA2ODataPrimitiveValue(attribute, input);
      property = new Property(null, propertyName);
      property.setValue(valueType, convertedValue);
      properties.add(property);
      return property;
    }
  }

  /**
   * Add values of input collection into target collection if not yet present.
   */
  private void addCollectionValuesIfUnique(final Collection<Object> targetList, final Collection<Object> listToAdd) {
    for (final Object v : listToAdd) {
      addSingleValueIfUnique(targetList, v);
    }
  }

  private void addSingleValueIfUnique(final Collection<Object> targetList, final Object valueToAdd) {
    if (targetList.contains(valueToAdd)) {
      return;
    }
    targetList.add(valueToAdd);
  }

  private Property findOrCreateProperty(final List<Property> properties, final String propertyName) {
    for (final Property p : properties) {
      if (p.getName().equals(propertyName)) {
        return p;
      }
    }
    final Property p = new Property(null, propertyName);
    properties.add(p);
    return p;
  }

  /**
   * Convert a <b>simple</b> OData attribute value into a JPA entity attribute
   * type matching one.
   *
   * @param jpaElement
   *            The affected attribute/parameter description.
   * @param sourceOdataValue
   *            The OData attribute value.
   * @return The JPA attribute type compliant instance value.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public final Object convertOData2JPAValue(final JPATypedElement jpaElement, final Object sourceOdataValue)
      throws ODataJPAConversionException {
    boolean isKey = false;
    if (JPAAttribute.class.isInstance(jpaElement)) {
      final JPAAttribute jpaAttribute = JPAAttribute.class.cast(jpaElement);
      isKey = jpaAttribute.isKey();
      if (sourceOdataValue == null && isKey) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
            jpaAttribute.getInternalName());
      }
    }
    final boolean isGenerated = jpaElement.getAnnotation(GeneratedValue.class) != null;
    // do not allow to set ID attributes if that attributes must be generated
    if (isGenerated && isKey) {
      throw new ODataJPAConversionException(HttpStatusCode.PRECONDITION_FAILED,
          ODataJPAConversionException.MessageKeys.GENERATED_KEY_ATTRIBUTE_IS_NOT_SUPPORTED, jpaElement.toString());
    }

    if (sourceOdataValue == null) {
      return null;
    }

    final Class<?> javaType = jpaElement.getType();
    if (javaType.isEnum() && Number.class.isInstance(sourceOdataValue)) {
      // convert enum ordinal value into enum literal
      return lookupEnum((Class<Enum>) javaType, ((Number) sourceOdataValue).intValue());
    }
    final Class<?> oadataType = sourceOdataValue.getClass();
    if (javaType.equals(oadataType)) {
      return sourceOdataValue;
    }
    final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(jpaElement, oadataType);
    if (converter != null) {
      return converter.convertToJPA(sourceOdataValue);
    }
    // no conversion
    return sourceOdataValue;
  }

  /**
   * Convert a <b>simple</b> OData attribute property value into a JPA entity attribute
   * type matching one.
   *
   * @param jpaElement
   *            The affected attribute/parameter description.
   * @param sourceOdataProperty
   *            The OData attribute value.
   * @return The JPA attribute type compliant instance value.
   */
  public final Object convertOData2JPAPropertyValue(final JPATypedElement jpaElement, final Valuable sourceOdataProperty)
      throws ODataJPAConversionException {
    final Object odataPropertyValue = sourceOdataProperty != null ? sourceOdataProperty.getValue() : null;// assume primitive value
    return convertOData2JPAValue(jpaElement, odataPropertyValue);
  }

  private static <E extends Enum<E>> E lookupEnum(final Class<E> clzz, final int ordinal) {
    final EnumSet<E> set = EnumSet.allOf(clzz);
    if (ordinal < set.size()) {
      final Iterator<E> iter = set.iterator();
      for (int i = 0; i < ordinal; i++) {
        iter.next();
      }
      final E rval = iter.next();
      assert (rval.ordinal() == ordinal);
      return rval;
    }
    throw new IllegalArgumentException("Invalid value " + ordinal + " for " + clzz.getName() + ", must be < " + set.size());
  }

  /**
   *
   * @param targetJPAObject
   *            The (optional) JPA related instance or DTO instance. If given then the converted attribute value will be set on target
   *            object.
   * @param jpaEntityType
   *            The meta model description of JPA related
   *            object.
   * @param jpaAttribute
   *            The JPA related attribute to process.
   * @param odataObjectProperties
   *            The list of all OData related properties as
   *            source for conversion.
   * @return The converted (and optional on target object set) value object.
   * @throws ODataApplicationException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws ODataJPAModelException
   */
  public final Object transferOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
      final JPAAttribute<?> jpaAttribute, final Collection<Property> odataObjectProperties)
          throws ODataJPAConversionException,
          NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAModelException {

    Property sourceOdataProperty;
    switch (jpaAttribute.getAttributeMapping()) {
    case SIMPLE:
      sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
      return transferSimpleOData2JPAProperty(targetJPAObject, (JPASimpleAttribute) jpaAttribute,
          sourceOdataProperty);
    case AS_COMPLEX_TYPE:
      sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
      return transferComplexOData2JPAProperty(targetJPAObject, jpaEntityType, jpaAttribute, sourceOdataProperty);
    case EMBEDDED_ID:
      return transferEmbeddedIdOData2JPAProperty(targetJPAObject, jpaEntityType, jpaAttribute,
          odataObjectProperties);
    case RELATIONSHIP:
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          jpaAttribute.getInternalName(), jpaEntityType.getInternalName());
    default:
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          jpaAttribute.getInternalName(), jpaEntityType.getInternalName());
    }
  }

  /**
   *
   * @param targetJPAObject
   *            Optional target of converted value, if <code>null</code> value will be only converted and returned, but not assigned to
   *            target object.
   */
  private Object transferSimpleOData2JPAProperty(final Object targetJPAObject, final JPATypedElement jpaAttribute,
      final Valuable sourceOdataProperty)
          throws ODataJPAConversionException, ODataJPAModelException {
    final Object jpaPropertyValue = convertOData2JPAPropertyValue(jpaAttribute,
        sourceOdataProperty);
    if (targetJPAObject != null && JPAAttribute.class.isInstance(jpaAttribute)) {
      ((JPAAttribute<?>) jpaAttribute).getAttributeAccessor().setPropertyValue(targetJPAObject, jpaPropertyValue);
    } else if (targetJPAObject != null) {
      log.log(Level.WARNING,
          "transferSimpleOData2JPAProperty() with target JPA object, but not for an attribute, cannot assign value to JPA entity!");
    }
    return jpaPropertyValue;
  }

  /**
   *
   * @param targetJPAObject
   *            Optional target of converted value, if <code>null</code> value will be only converted and returned, but not assigned to
   *            target object.
   *
   * @return The reused or new instance of embedded id.
   */
  private Object transferEmbeddedIdOData2JPAProperty(final Object targetJPAObject,
      final JPAStructuredType jpaEntityType,
      final JPAAttribute<?> jpaAttribute, final Collection<Property> odataObjectProperties)
          throws ODataJPAModelException, ODataJPAConversionException, NoSuchFieldException, IllegalArgumentException,
          IllegalAccessException {
    Object embeddedIdFieldObject = null;
    if (targetJPAObject != null) {
      embeddedIdFieldObject = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
    }
    final JPAStructuredType embeddedIdFieldType = jpaAttribute.getStructuredType();
    boolean newInstance = false;
    if (embeddedIdFieldObject == null) {
      embeddedIdFieldObject = newJPAInstance(embeddedIdFieldType);
      newInstance = true;
    }
    for (final JPAAttribute<?> jpaEmbeddedIdAttribute : embeddedIdFieldType.getAttributes()) {
      final Object conv = transferOData2JPAProperty(embeddedIdFieldObject, embeddedIdFieldType,
          jpaEmbeddedIdAttribute, odataObjectProperties);
      if (conv == null) {
        log.log(Level.SEVERE, "Missing key attribute value: " + jpaEntityType.getExternalName() + "/"
            + jpaAttribute.getExternalName() + "#" + jpaEmbeddedIdAttribute.getExternalName());
        throw new ODataJPAConversionException(HttpStatusCode.BAD_REQUEST,
            ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
            jpaAttribute.getExternalName());
      }
    }
    if (newInstance && targetJPAObject != null) {
      jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, embeddedIdFieldObject);
    }
    return embeddedIdFieldObject;
  }

  private Object transferComplexOData2JPAProperty(final Object targetJPAObject, final JPAStructuredType jpaEntityType,
      final JPAAttribute<?> jpaAttribute, final Property sourceOdataProperty)
          throws ODataJPAModelException, ODataJPAConversionException, NoSuchFieldException, IllegalArgumentException,
          IllegalAccessException {
    if (sourceOdataProperty == null) {
      return null;
    }
    if (!sourceOdataProperty.isComplex()) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          jpaAttribute.getStructuredType().getInternalName(), jpaEntityType.getInternalName());
    }
    Object embeddedFieldObject = null;
    if (targetJPAObject != null) {
      embeddedFieldObject = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
    }
    if (embeddedFieldObject == null) {
      throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
          jpaAttribute.getExternalName());
    }
    final JPAStructuredType embeddedJPAType = jpaAttribute.getStructuredType();
    if (sourceOdataProperty.isCollection()) {
      // manage structured types in a collection
      @SuppressWarnings("unchecked")
      final Collection<Object> collectionOfComplexTypes = (Collection<Object>) embeddedFieldObject;
      if (!collectionOfComplexTypes.isEmpty()) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_ALREADY_CONVERTED,
            jpaAttribute.getExternalName());
      }
      for (final Object entry : sourceOdataProperty.asCollection()) {
        final Object embeddedJPAInstance = newJPAInstance(embeddedJPAType);
        final List<Property> listEmbeddedProperties = ((ComplexValue) entry).getValue();
        for (final Property embeddedProperty : listEmbeddedProperties) {
          final JPAAttribute<?> embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName())
              .getLeaf();
          transferOData2JPAProperty(embeddedJPAInstance, embeddedJPAType, embeddedJPAAttribute,
              listEmbeddedProperties);
        }
        collectionOfComplexTypes.add(embeddedJPAInstance);
      }
    } else {
      // single structured type attribute
      final ComplexValue cv = sourceOdataProperty.asComplex();
      if (cv != null) {
        for (final Property embeddedProperty : cv.getValue()) {
          final JPAAttribute<?> embeddedJPAAttribute = embeddedJPAType.getPath(embeddedProperty.getName())
              .getLeaf();
          transferOData2JPAProperty(embeddedFieldObject, embeddedJPAType, embeddedJPAAttribute,
              Collections.singletonList(embeddedProperty));
        }
      } else if (JPATypedElement.class.isInstance(jpaAttribute) && !JPATypedElement.class.cast(jpaAttribute)
          .isNullable()) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
            jpaAttribute.getExternalName());
      } else {
        // mmmhh above we force the existence of that complex value object and now set it to null?!
        jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, null);
      }
    }
    return embeddedFieldObject;
  }

  /**
   *
   * @return The property of requested name or <code>null</code>.
   */
  private Property selectProperty(final Collection<Property> odataObjectProperties, final String propertyname) {
    for (final Property p : odataObjectProperties) {
      if (propertyname.equals(p.getName())) {
        return p;
      }
    }
    return null;
  }

  /**
   *
   * @param jpaEntityType
   *            The type of object to create instance of.
   * @return The new instance.
   * @throws ODataJPAModelException
   *             If construction of new instance failed.
   */
  protected Object newJPAInstance(final JPAStructuredType jpaEntityType) throws ODataJPAModelException {
    try {
      return jpaEntityType.getTypeClass().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL, e);
    }
  }

}