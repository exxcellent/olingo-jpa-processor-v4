package org.apache.olingo.jpa.processor.core.query;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.GeneratedValue;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADescribedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADynamicPropertyContainer;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAParameterizedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.TypeMapping;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException.MessageKeys;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDateTime2SqlTimestampODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalDateTime2ZonedDateTimeODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.LocalTime2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.SqlDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.SqlTime2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.UtilDate2UtilCalendarODataAttributeConverter;
import org.apache.olingo.jpa.processor.core.mapping.converter.Character2StringODataAttributeConverter;
import org.apache.olingo.server.api.ODataApplicationException;

import javassist.util.proxy.ProxyFactory;

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
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.time.ZonedDateTime.class,
        java.time.LocalDateTime.class, true, new LocalDateTime2ZonedDateTimeODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(java.sql.Timestamp.class,
        java.time.LocalDateTime.class, true, new LocalDateTime2SqlTimestampODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(String.class,
        Character.class, true, new Character2StringODataAttributeConverter()));
    DEFAULT_ODATA_ATTRIBUTE_CONVERTERS.add(new ConverterMapping(String.class,
        char.class, true, new Character2StringODataAttributeConverter()));
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
   * The attribute to look for an assigned converter.
   * @param odataAttributeType If <code>null</code> then no
   * {@link #determineDefaultODataAttributeConverter(Class, Class) default converter} can be used and only attributes
   * annotated with {@link EdmAttributeConversion @EdmAttributeConversion} can handle <code>null</code> values in a
   * different way.
   *
   * @return A found converter or <code>null</code> if no converter is available.
   */
  protected final ODataAttributeConverter<Object, Object> determineODataAttributeConverter(
      final JPADescribedElement jpaElement,
      final Class<?> odataAttributeType) throws ODataJPAConversionException {
    final ODataAttributeConverter<Object, Object> converter = determineCustomODataAttributeConverter(jpaElement);
    if (converter != null) {
      return converter;
    }
    // look for default converter
    return determineDefaultODataAttributeConverter(jpaElement.getType(), odataAttributeType);
  }

  @SuppressWarnings("unchecked")
  private ODataAttributeConverter<Object, Object> determineCustomODataAttributeConverter(
      final JPADescribedElement jpaElement) throws ODataJPAConversionException {
    if (jpaElement.getAnnotatedElement() == null) {
      return null;
    }
    final EdmAttributeConversion annoConversionConfiguration = jpaElement.getAnnotatedElement().getAnnotation(
        EdmAttributeConversion.class);
    if (annoConversionConfiguration != null) {
      try {
        if (!EdmAttributeConversion.DEFAULT.class.equals(annoConversionConfiguration.converter())) {
          return (ODataAttributeConverter<Object, Object>) annoConversionConfiguration.converter().newInstance();
        }
      } catch (InstantiationException | IllegalAccessException e) {
        throw new ODataJPAConversionException(e, ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM, e.getMessage());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private ODataAttributeConverter<Object, Object> determineDefaultODataAttributeConverter(
      final Class<?> jpaAttributeType,
      final Class<?> odataAttributeType) {
    if (jpaAttributeType == null || odataAttributeType == null) {
      return null;
    }
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
  protected Object convertJPA2ODataPrimitiveValue(final JPADescribedElement attribute, final Object jpaValue)
      throws ODataJPAConversionException, ODataJPAModelException {

    ODataAttributeConverter<Object, Object> converter = determineCustomODataAttributeConverter(attribute);
    if (converter != null) {
      return converter.convertToOData(jpaValue);
    }

    final Class<?> javaType = attribute.getType();
    final Class<?> oadataType;
    if (javaType.isEnum()) {
      oadataType = null;
    } else {
      // use a intermediate conversion to an supported JAVA type in the Olingo library
      oadataType = TypeMapping.determineODataRepresentationtype(attribute.getType(), attribute.getAnnotatedElement());
    }
    if (javaType.equals(oadataType)) {
      return jpaValue;
    }
    converter = determineDefaultODataAttributeConverter(javaType, oadataType);
    if (converter != null) {
      return converter.convertToOData(jpaValue);
    }
    // use 'as is' without conversion
    return jpaValue;
  }

  /**
   * This method is called for different scenarios:
   * <ol>
   * <li>The <i>input</i> is a complete collection for an collection attribute containing all values -&gt; called for
   * attributes in JPA entity</li>
   * <li>The <i>input</i> is single element in a collection attribute -&gt; called for every query result in
   * a @ElementCollection query</li>
   * <li>The <i>input</i> the single attribute value -&gt; called for attributes in JPA entity</li>
   * </ol>
   */
  protected final Property convertJPA2ODataProperty(final JPAMemberAttribute attribute, final String propertyName,
      final Object input,
      final List<Property> properties) throws ODataJPAModelException, ODataJPAConversionException, IllegalArgumentException {
    if (!attribute.isSimple()) {
      throw new IllegalArgumentException("attribute is not simple... wrong method call");
    }
    return convertJPA2ODataProperty(attribute, attribute.isCollection(), propertyName, input, properties);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected final Property convertJPA2ODataProperty(final JPADescribedElement attribute, final boolean isCollection,
      final String propertyName,
      final Object input,
      final List<Property> properties) throws ODataJPAModelException, ODataJPAConversionException,
  IllegalArgumentException {

    final Class<?> javaType = attribute.getType();
    if (javaType == null) {
      throw new IllegalArgumentException("Java type required for property " + propertyName);
    }
    final ValueType valueType;
    if (isCollection) {
      if (javaType.isEnum()) {
        valueType = ValueType.COLLECTION_ENUM;
      } else {
        valueType = ValueType.COLLECTION_PRIMITIVE;
      }
    } else {
      if (javaType.isEnum()) {
        valueType = ValueType.ENUM;
      } else {
        valueType = ValueType.PRIMITIVE;
      }
    }
    Property property;
    Object convertedInput;
    // as default use input 'as is'
    convertedInput = input;
    switch (valueType) {
    case COLLECTION_ENUM:
      // special case: convert input into enum values
      if (input instanceof Collection<?>) {
        final Collection<Object> vList = new LinkedList<>();
        for (final Enum v : (Collection<Enum>) input) {
          final Integer cV = v != null ? Integer.valueOf(v.ordinal()) : null;
          vList.add(cV);
        }
        convertedInput = vList;
      } else {
        convertedInput = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
      }
      // fall through
    case COLLECTION_PRIMITIVE:
      property = findOrCreateProperty(properties, propertyName);
      if (property.getValueType() == null) {
        // new created property
        final Collection<Object> list = new LinkedList<>();
        if (convertedInput instanceof Collection<?>) {
          addCollectionValuesIfUnique(list, (Collection) convertedInput);
        } else {
          // handle null input as 'null' collection element
          addSingleValueIfUnique(list, convertedInput);
        }
        property.setValue(valueType, list);
      } else {
        // add value to existing property
        if (convertedInput instanceof Collection<?>) {
          addCollectionValuesIfUnique((List) property.getValue(), (Collection) convertedInput);
        } else {
          // handle null input as 'null' collection element
          addSingleValueIfUnique((List) property.getValue(), convertedInput);
        }
      }
      return property;
    case ENUM:
      // for OData we have to convert the value into a number (if not yet)
      convertedInput = input != null ? Integer.valueOf(((Enum<?>) input).ordinal()) : null;
      // fall through
    default:
      // handle as primitive
      final Object result = convertJPA2ODataPrimitiveValue(attribute, convertedInput);
      property = findOrCreateProperty(properties, propertyName);
      property.setValue(valueType, result);
      return property;
    }
  }

  /**
   * @see org.apache.olingo.jpa.metadata.core.edm.mapper.impl.FieldAttributeAccessor#writeJPAFieldValue
   */
  private Collection<Object> createCorrectJPACollectionType(final JPAParameterizedElement attribute) {
    switch (attribute.getCollectionType()) {
    case MAP:
      throw new UnsupportedOperationException("Map cannot be handled");
    case SET:
      return new HashSet<>();
    default:
      return new LinkedList<>();
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

  /**
   *
   * @return The property of requested name or <code>null</code>.
   */
  private Property selectProperty(final Collection<Property> properties, final String propertyName) {
    for (final Property p : properties) {
      if (p.getName().equals(propertyName)) {
        return p;
      }
    }
    return null;
  }

  private Property findOrCreateProperty(final List<Property> properties, final String propertyName) {
    Property p = selectProperty(properties, propertyName);
    if (p != null) {
      return p;
    }
    p = new Property(null, propertyName);
    properties.add(p);
    return p;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object convertSingleOData2JPAValue(final Class<?> jpaType, final Class<?> oadataType,
      final ODataAttributeConverter<Object, Object> converter, final Object sourceOdataValue) {
    if (Collection.class.isInstance(sourceOdataValue)) {
      throw new IllegalStateException("Collection is not allowed here");
    }
    final Object toConvertValue;
    if (jpaType.equals(oadataType)) {
      // no conversion
      return sourceOdataValue;
    } else if (jpaType.isEnum() && Number.class.isInstance(sourceOdataValue)) {
      // convert enum ordinal value into enum literal
      toConvertValue = lookupEnum((Class<Enum>) jpaType, ((Number) sourceOdataValue).intValue());
    } else {
      toConvertValue = sourceOdataValue;
    }
    if (converter != null) {
      return converter.convertToJPA(toConvertValue);
    }
    // no conversion
    return toConvertValue;

  }

  protected static Class<?> detectValueType(final Object value) {
    if (value == null) {
      return null;
    }
    if (Collection.class.isInstance(value)) {
      final Collection<?> coll = (Collection<?>) value;
      if (coll.isEmpty()) {
        return null;
      }
      final Object firstElement = coll.iterator().next();
      if (firstElement == null) {
        return null;
      }
      return firstElement.getClass();
    }
    return value.getClass();
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
  public final Object convertOData2JPAValue(final JPAParameterizedElement jpaElement, final Object sourceOdataValue)
      throws ODataJPAConversionException {
    final boolean isGenerated = jpaElement.getAnnotatedElement() != null ? jpaElement.getAnnotatedElement()
        .getAnnotation(GeneratedValue.class) != null : false;
    boolean isKey = false;
    if (JPAAttribute.class.isInstance(jpaElement)) {
      final JPAAttribute<?> jpaAttribute = JPAAttribute.class.cast(jpaElement);
      isKey = jpaAttribute.isKey();
      if (sourceOdataValue == null && isKey && !isGenerated) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
            jpaAttribute.getInternalName());
      }
    }

    final Class<?> oadataType = detectValueType(sourceOdataValue);
    final ODataAttributeConverter<Object, Object> converter = determineODataAttributeConverter(jpaElement, oadataType);
    final Class<?> jpaType = jpaElement.getType();

    if (jpaElement.isCollection()) {
      if (sourceOdataValue == null) {
        return null;
      }
      if (!Collection.class.isInstance(sourceOdataValue)) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM,
            "Value is not a collection");
      }
      final Collection<Object> resultCollection = createCorrectJPACollectionType(jpaElement);
      for (final Object v : (Collection<?>) sourceOdataValue) {
        final Object r = convertSingleOData2JPAValue(jpaType, oadataType, converter, v);
        resultCollection.add(r);
      }
      return resultCollection;
    } else {
      // single value
      return convertSingleOData2JPAValue(jpaType, oadataType, converter, sourceOdataValue);
    }
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
  public final Object convertOData2JPAPropertyValue(final JPAParameterizedElement jpaElement, final Valuable sourceOdataProperty)
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
  public final Object transferOData2JPAProperty(final Object targetJPAObject, final JPAAttribute<?> jpaAttribute,
      final Collection<Property> odataObjectProperties)
          throws ODataJPAConversionException,
          NoSuchFieldException, IllegalArgumentException, IllegalAccessException, ODataJPAModelException {

    Property sourceOdataProperty;
    switch (jpaAttribute.getAttributeMapping()) {
    case SIMPLE:
      sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
      return transferODataSimple2JPAProperty(targetJPAObject, (JPAMemberAttribute) jpaAttribute,
          sourceOdataProperty);
    case AS_COMPLEX_TYPE:
      sourceOdataProperty = selectProperty(odataObjectProperties, jpaAttribute.getExternalName());
      return transferODataComplex2JPAProperty(targetJPAObject, jpaAttribute, sourceOdataProperty);
    case EMBEDDED_ID:
      return transferODataEmbeddedId2JPAProperty(targetJPAObject, jpaAttribute, odataObjectProperties);
    case RELATIONSHIP:
      // fall through
    default:
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          jpaAttribute.getInternalName(), jpaAttribute.getInternalName());
    }
  }

  /**
   *
   * @param targetJPAObject
   *            Optional target of converted value, if <code>null</code> value will be only converted and returned, but not assigned to
   *            target object.
   */
  private Object transferODataSimple2JPAProperty(final Object targetJPAObject, final JPAParameterizedElement jpaAttribute,
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
  private Object transferODataEmbeddedId2JPAProperty(final Object targetJPAObject,
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
    for (final JPAAttribute<?> jpaEmbeddedIdAttribute : embeddedIdFieldType.getAttributes(false)) {
      final Object conv = transferOData2JPAProperty(embeddedIdFieldObject, jpaEmbeddedIdAttribute,
          odataObjectProperties);
      if (conv == null) {
        log.log(Level.SEVERE, "Missing key attribute value: " + jpaAttribute.getExternalName() + "#"
            + jpaEmbeddedIdAttribute.getExternalName());
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

  /**
   *
   * @return The resulting property value from JPA entity instance
   */
  private Object transferODataComplex2JPAProperty(final Object targetJPAObject,
      final JPAAttribute<?> jpaAttribute, final Property sourceOdataProperty)
          throws ODataJPAModelException, ODataJPAConversionException, NoSuchFieldException, IllegalArgumentException,
          IllegalAccessException {
    if (sourceOdataProperty == null) {
      return null;
    }
    if (!sourceOdataProperty.isComplex()) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          jpaAttribute.getStructuredType().getInternalName(), jpaAttribute.getInternalName());
    }
    Object targetJPAObjectMemberValue = null;
    if (targetJPAObject != null) {
      targetJPAObjectMemberValue = jpaAttribute.getAttributeAccessor().getPropertyValue(targetJPAObject);
    }
    if (targetJPAObjectMemberValue == null) {
      throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
          jpaAttribute.getExternalName());
    }
    final JPAStructuredType embeddedJPAMemberType = jpaAttribute.getStructuredType();
    if (sourceOdataProperty.isCollection()) {
      // manage structured types in a collection
      @SuppressWarnings("unchecked")
      final Collection<Object> collectionOfStructuredTypeInstances = (Collection<Object>) targetJPAObjectMemberValue;
      if (!collectionOfStructuredTypeInstances.isEmpty()) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_ALREADY_CONVERTED,
            jpaAttribute.getExternalName());
      }
      for (final Object entry : sourceOdataProperty.asCollection()) {
        final Object embeddedCollectionMemberJPAInstance = newJPAInstance(embeddedJPAMemberType);
        final ComplexValue cv = (ComplexValue) entry;
        final List<Property> listEmbeddedProperties = cv.getValue();
        transferODataSingleComplexValue2JPAProperty(embeddedJPAMemberType, embeddedCollectionMemberJPAInstance,
            listEmbeddedProperties);
        collectionOfStructuredTypeInstances.add(embeddedCollectionMemberJPAInstance);
      }
    } else {
      // single structured type attribute
      final ComplexValue cv = sourceOdataProperty.asComplex();
      if (cv != null) {
        final List<Property> listEmbeddedProperties = cv.getValue();
        transferODataSingleComplexValue2JPAProperty(embeddedJPAMemberType, targetJPAObjectMemberValue,
            listEmbeddedProperties);
      } else if (JPAParameterizedElement.class.isInstance(jpaAttribute) && !JPAParameterizedElement.class.cast(jpaAttribute)
          .isNullable()) {
        throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.ATTRIBUTE_MUST_NOT_BE_NULL,
            jpaAttribute.getExternalName());
      } else {
        // if complex value is null, force java member also to be null
        jpaAttribute.getAttributeAccessor().setPropertyValue(targetJPAObject, null);
        targetJPAObjectMemberValue = null;
      }
    }
    return targetJPAObjectMemberValue;
  }

  final public void transferODataSingleComplexValue2JPAProperty(final JPAStructuredType embeddedJPAMemberType,
      final Object targetJPAObjectMemberValue, final List<Property> listEmbeddedProperties)
      throws ODataJPAModelException, ODataJPAConversionException {
    final Set<String> unprocessedPropertyNames = listEmbeddedProperties.stream().map(p -> p.getName()).collect(
        Collectors.toSet());
    // 1. normal attributes
    for (final JPAMemberAttribute embeddedJPAAttribute : embeddedJPAMemberType.getAttributes(false)) {
      unprocessedPropertyNames.remove(embeddedJPAAttribute.getExternalName());
      try {
        transferOData2JPAProperty(targetJPAObjectMemberValue, embeddedJPAAttribute, listEmbeddedProperties);
      } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
        throw new ODataJPAConversionException(HttpStatusCode.EXPECTATION_FAILED, e, MessageKeys.RUNTIME_PROBLEM,
            "problem converting attribute value");
      }
    }
    // 2. dynamic properties for open type
    if (embeddedJPAMemberType.isOpenType()) {
      final JPADynamicPropertyContainer openType = JPADynamicPropertyContainer.class.cast(embeddedJPAMemberType);
      if (Map.class.isInstance(targetJPAObjectMemberValue)) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> map = Map.class.cast(targetJPAObjectMemberValue);
        // create OData properties as key/values in a map
        final JPAParameterizedElement dynamicAttribute = openType.getDynamicPropertyDescription();
        for (final Property odataProperty : listEmbeddedProperties) {
          final Object result = convertOData2JPAPropertyValue(dynamicAttribute, odataProperty);
          // be aware: no name conversion (no naming strategy)
          map.put(odataProperty.getName(), result);
          unprocessedPropertyNames.remove(odataProperty.getName());
        }
      }
    }
    if (!unprocessedPropertyNames.isEmpty()) {
      throw new ODataJPAConversionException(HttpStatusCode.EXPECTATION_FAILED, MessageKeys.RUNTIME_PROBLEM,
          "Unknown OData properties present: " + unprocessedPropertyNames.stream().collect(Collectors.joining(", ")));
    }
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

    if (Modifier.isAbstract(jpaEntityType.getTypeClass().getModifiers())) {
      // special code to create instance of abstract class
      final ProxyFactory factory = new ProxyFactory();
      factory.setSuperclass(jpaEntityType.getTypeClass());
      //      factory.setFilter(handler);
      try {
        return factory.create(new Class[0], new Object[0]);
      } catch (NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
          | InvocationTargetException e) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL, e);
      }
    }
    try {
      return jpaEntityType.getTypeClass().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL, e);
    }
  }

}