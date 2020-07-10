package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEnumValue;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;


class TypeConverterAPIWriter extends AbstractWriter {

  private static final String METHOD_EXTRACT_ENUMVALUE = "extractEnumValue";

  @SuppressWarnings("unused")
  private final AbstractJPASchema schema;
  private final JPAStructuredType type;

  public TypeConverterAPIWriter(final File generationBaseDirectory, final AbstractJPASchema schema,
      final JPAStructuredType et) {
    super(generationBaseDirectory, et.getTypeClass().getPackage().getName(), determineConverterName(et));
    this.schema = schema;
    this.type = et;
  }

  public void writeProtocolCodeStart() throws IOException {
    createFile();
    final String className = determineConverterName(type);
    write(HEADER_TEXT);
    write(NEWLINE + "public class " + className /* + " implements " + typeMetaName */ + " {");

    // constructor
    write(NEWLINE);
    write(NEWLINE + "\t" + "private final " + ClientObjectFactory.class.getName() + " factory;");
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + className + "(" + ClientObjectFactory.class.getName() + " factory" + ")"
        + " {");
    write(NEWLINE + "\t" + "\t" + "this.factory = factory;");
    write(NEWLINE + "\t" + "}");
  }

  static String determineConverterName(final JPAStructuredType type) {
    return TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName()) + "Converter";
  }

  public void writeProtocolCode() throws IOException, ODataJPAModelException {
    generateConvertEntity2Dto();
    generateConvertDto2Entity();
  }

  private boolean entityContainsEnumAttribute() throws ODataJPAModelException {
    for (final JPAMemberAttribute prop : type.getAttributes()) {
      if (prop.getType().isEnum()) {
        return true;
      }
    }
    return false;
  }

  private void generateConvertDto2Entity() throws IOException, ODataJPAModelException {
    final Set<String> mapAlreadeGeneratedMethods = new HashSet<>();
    generateConvertMethods4Attribute2PropertyValue(type, mapAlreadeGeneratedMethods);

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());

    write(NEWLINE);
    write(NEWLINE + "\t" + "public " + ClientEntity.class.getName() + " toEntity(" + typeDtoName + " dto"
        + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "final " + ClientEntity.class.getName()
        + " propertyContainer = factory.newEntity(new "
        + FullQualifiedName.class.getName() + "(\"" + type.getExternalFQN().getNamespace() + "\", \"" + type
        .getExternalFQN().getName() + "\"));");
    write(NEWLINE + "\t" + "\t" + "// convert DTO into generic entity...");
    generateAllAttribute2PropertyConversion(type, true, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return propertyContainer;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateConvertMethods4Attribute2PropertyValue(final JPAStructuredType ownerType,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    for (final JPAMemberAttribute attribute : ownerType.getAttributes()) {
      if (attribute.isCollection()) {
        generateConvertMethod4Attribute2PropertyCollectionValue(attribute, mapAlreadeGeneratedMethods);
      } else if (attribute.isComplex()) {
        if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
          // special case for complex key attribute type having nested attributes
          for (final JPAMemberAttribute embeddedIdAttribute : attribute.getStructuredType().getAttributes()) {
            generateConvertMethodAttribute2PrimitiveValue(embeddedIdAttribute, mapAlreadeGeneratedMethods);
          }
        } else {
          generateConvertMethod4Dto2ComplexValue(attribute, mapAlreadeGeneratedMethods);
        }
      } else {
        generateConvertMethodAttribute2PrimitiveValue(attribute, mapAlreadeGeneratedMethods);
      }
    }

    for (final JPAAssociationAttribute asso : ownerType.getAssociations()) {
      if (asso.isCollection()) {
        generateConvertMethod4Association2PropertyCollectionValue(asso, mapAlreadeGeneratedMethods);
      } else {
        generateConvertMethod4Dto2ComplexValue(asso, mapAlreadeGeneratedMethods);
      }
    }
  }

  /**
   *
   * @return Method signature or <code>null</code> if no method is generated.
   */
  private String generateConvertMethodAttribute2PrimitiveValue(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    EdmPrimitiveTypeKind odataType;
    try {
      odataType = TypeMapping.convertToEdmSimpleType(attribute);
    } catch (final ODataJPAModelException e) {
      return null;
    }
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    final String methodName = determineConversionAttribute2PropertyValueMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + ClientPrimitiveValue.class.getName() + " " + methodName + "(" + propClientType
        + " attributeValue" + ") throws "
        + ODataException.class.getName() + " {");

    @SuppressWarnings("deprecation")
    final EdmAttributeConversion annoConversionConfiguration = attribute.getAnnotation(EdmAttributeConversion.class);
    if (annoConversionConfiguration != null && !EdmAttributeConversion.DEFAULT.class.equals(annoConversionConfiguration
        .converter())) {
      write(NEWLINE + "\t" + "\t"
          + "throw new UnsupportedOperationException(\"implement converter manually on client side: "
          + annoConversionConfiguration
          .converter().getCanonicalName() + "\");");
    } else {
      if (!attribute.getType().isPrimitive()) {
        // common null value check
        write(NEWLINE + "\t" + "\t" + "if(attributeValue == null) {");
        write(NEWLINE + "\t" + "\t" + "\t" + "return null;");
        write(NEWLINE + "\t" + "\t" + "}");
      }
      switch (odataType) {
      case Boolean:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t"
              + "return factory.newPrimitiveValueBuilder().buildBoolean(Boolean.valueOf(attributeValue));");
        } else {
          write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildBoolean(attributeValue);");
        }
        break;
      case String:
        write(NEWLINE + "\t" + "\t"
            + "return factory.newPrimitiveValueBuilder().buildString(attributeValue.toString());");
        break;
      case Int16:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t"
              + "return factory.newPrimitiveValueBuilder().buildInt16(Short.valueOf(attributeValue));");
        } else {
          write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildInt16(attributeValue);");
        }
        break;
      case Int32:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t"
              + "return factory.newPrimitiveValueBuilder().buildInt32(Integer.valueOf(attributeValue));");
        } else {
          write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildInt32(attributeValue);");
        }
        break;
      case Int64:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t"
              + "return factory.newPrimitiveValueBuilder().buildInt64(Long.valueOf(attributeValue));");
        } else {
          write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildInt64(attributeValue);");
        }
        break;
      case Double:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t"
              + "return factory.newPrimitiveValueBuilder().buildDouble(Double.valueOf(attributeValue));");
        } else {
          write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildDouble(attributeValue);");
        }
        break;
      case Decimal:
        write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildDecimal(attributeValue);");
        break;
      case DateTimeOffset:
      case TimeOfDay:
      case Date:
      case Guid:
        // all types having a string representation
        write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
        write(NEWLINE + "\t" + "\t" + "final String sValue = " + EdmPrimitiveTypeFactory.class.getName()
            + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "." + odataType.name()
            + ").valueToString(attributeValue, Boolean.TRUE, " + integer2CodeString(attribute
                .getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", " + integer2CodeString(
                    attribute.getScale()) + ", Boolean.TRUE" + ");");
        write(NEWLINE + "\t" + "\t" + "return factory.newPrimitiveValueBuilder().buildString(sValue);");
        break;
      case Byte:
        // FIXME
      default:
        write(NEWLINE + "\t" + "\t" + "throw new UnsupportedOperationException();");
      }
    }
    write(NEWLINE + "\t" + "}");

    mapAlreadeGeneratedMethods.add(methodName);
    return methodName;
  }

  private void generateAllAttribute2PropertyConversion(final JPAStructuredType sType, final boolean ownerISRootEntity,
      final Set<String> mapAlreadeGeneratedMethods) throws ODataJPAModelException,
  IOException {
    for (final JPAAssociationAttribute asso : sType.getAssociations()) {
      generateAssociation2PropertyConversion(sType, ownerISRootEntity, asso);
    }
    // simple properties
    for (final JPAMemberAttribute attribute : sType.getAttributes()) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        generateAttribute2PropertyConversion(sType, ownerISRootEntity, attribute);
        break;
      case EMBEDDED_ID:
        // handle properties of nested complex type (@EmbeddedId) as properties of this type
        for (final JPAMemberAttribute nestedProp : attribute.getStructuredType().getAttributes()) {
          generateAttribute2PropertyConversion(sType, ownerISRootEntity, nestedProp);
        }
        break;
      case AS_COMPLEX_TYPE:
        generateAttribute2PropertyConversion(sType, ownerISRootEntity, attribute);
        break;
      case RELATIONSHIP:
        throw new UnsupportedOperationException("Relationship '" + attribute.getInternalName()
        + "' must not occur here");
      }
    }
  }

  private void generateConvertEntity2Dto() throws IOException, ODataJPAModelException {
    if (entityContainsEnumAttribute()) {
      generateConvertMethod4PrimitivePropertyValue2EnumValue();
    }
    final Set<String> mapAlreadeGeneratedMethods = new HashSet<>();
    generateConvertMethods4Property2Attribute(type, mapAlreadeGeneratedMethods);

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());

    write(NEWLINE);
    write(NEWLINE + "\t" + "public " + typeDtoName + " toDto(" + ClientEntity.class.getName() + " odataObject"
        + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "final " + typeDtoName + " dtoResult = new " + typeDtoName + "();");
    write(NEWLINE + "\t" + "\t" + "// convert generic response into DTO...");

    generateAllProperty2AttributeConversion(type, true, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return dtoResult;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateConvertMethods4Property2Attribute(final JPAStructuredType ownerType,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    for (final JPAMemberAttribute attribute : ownerType.getAttributes()) {
      if (attribute.isCollection()) {
        generateConvertMethod4Property2CollectionValue(attribute, mapAlreadeGeneratedMethods);
      } else if (attribute.isComplex()) {
        if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
          // special case for complex key attribute type having nested attributes
          for (final JPAMemberAttribute embeddedIdAttribute : attribute.getStructuredType().getAttributes()) {
            generateConvertMethod4PrimitivePropertyValue2AttributeValue(embeddedIdAttribute, mapAlreadeGeneratedMethods);
          }
        } else {
          generateConvertMethod4ComplexValue2Dto(attribute, mapAlreadeGeneratedMethods);
        }
      } else {
        generateConvertMethod4PrimitivePropertyValue2AttributeValue(attribute, mapAlreadeGeneratedMethods);
      }
    }

    for (final JPAAssociationAttribute asso : ownerType.getAssociations()) {
      if (asso.isCollection()) {
        generateConvertMethod4Property2AssociationCollection(asso, mapAlreadeGeneratedMethods);
      } else {
        generateConvertMethod4ComplexValue2Dto(asso, mapAlreadeGeneratedMethods);
      }
    }
  }

  /**
   * Uppercase every first character of every part in a qualified name.
   */
  private static String qualifiedName2FirstCharacterUppercasedString(final String s) {
    final StringBuffer result = new StringBuffer();
    final Matcher m = Pattern.compile("(?:\\.|^)(.)").matcher(s);
    while (m.find()) {
      m.appendReplacement(result,
          m.group(1).toUpperCase());
    }
    m.appendTail(result);
    return result.toString().replaceAll("\\[\\]", "Array");
  }

  /**
   *
   * @param respectCollection If TRUE and the given attribute describe a collection the resulting method name will
   * contain a 'collection' name part to indicate collection conversion.
   * @return
   * @throws ODataJPAModelException
   */
  private String determineConversionProperty2AttributeValueMethodName(final JPAAttribute<?> attribute,
      final boolean respectCollection) throws ODataJPAModelException {
    if (JPAMemberAttribute.class.isInstance(attribute)) {
      @SuppressWarnings("deprecation")
      final EdmAttributeConversion annoConversionConfiguration = JPAMemberAttribute.class.cast(attribute).getAnnotation(
          EdmAttributeConversion.class);
      if (annoConversionConfiguration != null && !EdmAttributeConversion.DEFAULT.class.equals(annoConversionConfiguration
          .converter())) {
        return "convertOData" + qualifiedName2FirstCharacterUppercasedString(attribute.getExternalName()) + "Via"
            + qualifiedName2FirstCharacterUppercasedString(annoConversionConfiguration.converter().getCanonicalName());
      }
    }
    String fromName;
    if (attribute.isAssociation()) {
      fromName = attribute.getExternalName();
    } else if (attribute.isComplex()) {
      fromName = attribute.getExternalName();
    } else if (JPATypedElement.class.cast(attribute).getType().isEnum()) {
      if (attribute.isCollection()&&respectCollection) {
        fromName = attribute.getExternalName();
      } else {
        throw new IllegalStateException("Call " + METHOD_EXTRACT_ENUMVALUE + " directly");
      }
    } else {
      // simple primitive type conversion
      final EdmPrimitiveTypeKind odataType = TypeMapping.convertToEdmSimpleType(JPATypedElement.class.cast(attribute));
      fromName = odataType.name();
    }

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    return "convertOData" + qualifiedName2FirstCharacterUppercasedString(fromName)
    + "To" + (attribute.isCollection() && respectCollection ? "CollectionOf" : "")
    + qualifiedName2FirstCharacterUppercasedString(
        propClientType);
  }

  private String determineConversionAttribute2PropertyValueMethodName(final JPAAttribute<?> attribute,
      final boolean respectCollection) throws ODataJPAModelException {
    if (JPAMemberAttribute.class.isInstance(attribute)) {
      @SuppressWarnings("deprecation")
      final EdmAttributeConversion annoConversionConfiguration = JPAMemberAttribute.class.cast(attribute).getAnnotation(
          EdmAttributeConversion.class);
      if (annoConversionConfiguration != null && !EdmAttributeConversion.DEFAULT.class.equals(annoConversionConfiguration
          .converter())) {
        return "convert"+ qualifiedName2FirstCharacterUppercasedString(annoConversionConfiguration.converter().getCanonicalName())+"ToOData"+ qualifiedName2FirstCharacterUppercasedString(attribute.getExternalName());
      }
    }
    String fromName;
    if (attribute.isAssociation()) {
      fromName = attribute.getExternalName();
    } else if (attribute.isComplex()) {
      fromName = attribute.getExternalName();
    } else if (JPATypedElement.class.cast(attribute).getType().isEnum()) {
      if (attribute.isCollection()&&respectCollection) {
        fromName = attribute.getExternalName();
      } else {
        throw new IllegalStateException("Call " + METHOD_EXTRACT_ENUMVALUE + " directly");
      }
    } else {
      // simple primitive type conversion
      final EdmPrimitiveTypeKind odataType = TypeMapping.convertToEdmSimpleType(JPATypedElement.class.cast(attribute));
      fromName = odataType.name();
    }

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    return "convert" + (attribute.isCollection() && respectCollection ? "CollectionOf" : "")
        + qualifiedName2FirstCharacterUppercasedString(propClientType) + "ToOData"
        + qualifiedName2FirstCharacterUppercasedString(fromName);
  }

  /**
   * Called for complex attributes and relationship attributes
   */
  private String generateConvertMethod4ComplexValue2Dto(final JPAAttribute<?> attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodName = determineConversionProperty2AttributeValueMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodName);

    // on demand
    generateConvertMethods4Property2Attribute(attribute.getStructuredType(), mapAlreadeGeneratedMethods);

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + "final " + ClientComplexValue.class
        .getName() + " odataObject" + ") throws " + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "final " + propClientType + " dtoResult = new " + propClientType + "();");

    generateAllProperty2AttributeConversion(attribute.getStructuredType(), false, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return dtoResult;");
    write(NEWLINE + "\t" + "}");

    return methodName;
  }

  /**
   * Called for complex attributes and relationship attributes
   */
  private String generateConvertMethod4Dto2ComplexValue(final JPAAttribute<?> attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodName = determineConversionAttribute2PropertyValueMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodName);

    // on demand
    generateConvertMethods4Attribute2PropertyValue(attribute.getStructuredType(), mapAlreadeGeneratedMethods);

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + ClientComplexValue.class.getName() + " " + methodName + "(" + propClientType
        + " dto" + ") throws " + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + ClientComplexValue.class.getName() + " propertyContainer = factory.newComplexValue(\""
        + attribute.getStructuredType().getExternalFQN().getFullQualifiedNameAsString() + "\");");

    generateAllAttribute2PropertyConversion(attribute.getStructuredType(), false, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return propertyContainer;");
    write(NEWLINE + "\t" + "}");

    return methodName;
  }

  /**
   *
   * @return Method signature or <code>null</code> if no method is generated.
   */
  private String generateConvertMethod4PrimitivePropertyValue2AttributeValue(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    EdmPrimitiveTypeKind odataType;
    try {
      odataType = TypeMapping.convertToEdmSimpleType(attribute);
    } catch (final ODataJPAModelException e) {
      return null;
    }
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    final String methodName = determineConversionProperty2AttributeValueMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + "String propertyName, "
        + ClientPrimitiveValue.class.getName() + " propertyValue, "
        + "final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    @SuppressWarnings("deprecation")
    final EdmAttributeConversion annoConversionConfiguration = attribute.getAnnotation(EdmAttributeConversion.class);
    if (annoConversionConfiguration != null && !EdmAttributeConversion.DEFAULT.class.equals(annoConversionConfiguration
        .converter())) {
      write(NEWLINE + "\t" + "\t"
          + "throw new UnsupportedOperationException(\"implement converter manually on client side: "
          + annoConversionConfiguration
          .converter().getCanonicalName() + "\");");
    } else {
      switch (odataType) {
      case Boolean:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Boolean.class).booleanValue();");
        } else {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Boolean.class);");
        }
        break;
      case String:
        write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(" + propClientType + ".class);");
        break;
      case Int16:
      case Int32:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Integer.class).intValue();");
        } else {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Integer.class);");
        }
        break;
      case Int64:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Long.class).longValue();");
        } else {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Long.class);");
        }
        break;
      case Double:
        if (attribute.getType().isPrimitive()) {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Double.class).doubleValue();");
        } else {
          write(NEWLINE + "\t" + "\t" + "return propertyValue.toCastValue(Double.class);");
        }
        break;
      case Decimal:
        write(NEWLINE + "\t" + "\t" + "Double dV = propertyValue.toCastValue(Double.class);");
        write(NEWLINE + "\t" + "\t"
            + BigDecimal.class.getCanonicalName() + " bd = precision != null ? new " + BigDecimal.class
            .getCanonicalName() + "(dV.doubleValue(), new " + MathContext.class.getCanonicalName()
            + "(precision.intValue())) : new " + BigDecimal.class.getCanonicalName() + "(dV.doubleValue());");
        write(NEWLINE + "\t" + "\t" + "bd = scale != null ? bd.setScale(scale.intValue()) : bd;");
        write(NEWLINE + "\t" + "\t" + "return bd;");
        break;
      case DateTimeOffset:
        generateConvertMethod4PrimitivePropertyValueForDateTimeOffset2AttributeValue(attribute.getType(), propClientType);
        break;
      case TimeOfDay:
      case Date:
      case Guid:
        generateConvertMethod4PrimitivePropertyValueForStringBasedDefault2AttributeValue(odataType, propClientType);
        break;
      case Byte:
        // FIXME
      default:
        write(NEWLINE + "\t" + "\t" + "throw new UnsupportedOperationException();");
      }
    }
    write(NEWLINE + "\t" + "}");

    mapAlreadeGeneratedMethods.add(methodName);
    return methodName;
  }

  private void generateConvertMethod4PrimitivePropertyValueForDateTimeOffset2AttributeValue(final Class<?> propJpaType, final String propClientType)
      throws IOException {
    if (propJpaType == LocalDateTime.class) {
      // convert into ZonedDateTime (avoiding time zone shifting problems) and that into LocalDateTime
      write(NEWLINE + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
          + EdmPrimitiveTypeFactory.class.getName() + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "."
          + EdmPrimitiveTypeKind.DateTimeOffset.name() + ");");
      write(NEWLINE + "\t" + "\t" + "final " + "String sV = propertyValue.toCastValue(String.class);");
      write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
      write(NEWLINE + "\t" + "\t"
          + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, "
          + ZonedDateTime.class.getCanonicalName() + ".class" + ").toLocalDateTime();");
      return;
    }
    // default
    generateConvertMethod4PrimitivePropertyValueForStringBasedDefault2AttributeValue(EdmPrimitiveTypeKind.DateTimeOffset, propClientType);
  }

  private void generateConvertMethod4PrimitivePropertyValueForStringBasedDefault2AttributeValue(final EdmPrimitiveTypeKind odataType,
      final String propClientType) throws IOException {
    // all these primitive types can be constructed from String
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
        + EdmPrimitiveTypeFactory.class.getName() + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "."
        + odataType.name() + ");");
    write(NEWLINE + "\t" + "\t" + " final " + "String sV = propertyValue.toCastValue(String.class);");
    write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
    write(NEWLINE + "\t" + "\t"
        + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, " + propClientType
        + ".class" + ");");
  }

  private void generateConvertMethod4PrimitivePropertyValue2EnumValue() throws ODataJPAModelException, IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "@SuppressWarnings({ \"unchecked\", \"rawtypes\" })");
    write(NEWLINE + "\t" + "private " + "<T extends " + ClientValue.class.getName() + ", R>" + " R "
        + METHOD_EXTRACT_ENUMVALUE + "(final " + "T clientValue, " + Class.class.getSimpleName()
        + "<R> resultTypeClass) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue.isEnum()) {");
    // enum handling code
    // enums are generated as duplicate on client side (code) so we can reference directly the enum class
    write(NEWLINE + "\t" + "\t" + "\t" + "final String sV = clientValue.asEnum().getValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return (R)Enum.valueOf((Class<Enum>)resultTypeClass, sV);");
    write(NEWLINE + "\t" + "\t" + "} else if(resultTypeClass.isEnum()) {");
    // if client side type is NOT enum, but JPA type... so it's safe to cast to string
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "final " + "String sV = (String)clientValue.asPrimitive().toValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return (R)Enum.valueOf((Class<Enum>)resultTypeClass, sV);");
    write(NEWLINE + "\t" + "\t" + "} else {");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "throw new " + UnsupportedOperationException.class.getSimpleName()
        + "(resultTypeClass.getName());");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "}");
  }

  private String generateConvertMethod4Property2AssociationCollection(final JPAAssociationAttribute relationship,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    if (!relationship.isCollection()) {
      throw new IllegalStateException(relationship.getInternalName()+" is not a collection relationship");
    }
    final String methodCollectionName = determineConversionProperty2AttributeValueMethodName(relationship, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);

    // on demand generation
    generateConvertMethod4ComplexValue2Dto(relationship, mapAlreadeGeneratedMethods);

    final String resultType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(relationship, true);

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + Collection.class.getName() + "<" + resultType + "> " + methodCollectionName + "("
        + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName()
        + "> clientValue) throws " + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue == null) {");
    //    write(NEWLINE + "\t" + "\t" + "\t" + "return " + Collections.class.getName() + ".emptyList();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return null;");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + Collection.class.getName() + "<" + resultType + "> result = new " + ArrayList.class
        .getName()
        + "<" + resultType + ">(clientValue.size());");
    write(NEWLINE + "\t" + "\t" + Iterator.class.getName() + "<" + ClientValue.class.getName()
        + "> it = clientValue.iterator();");
    write(NEWLINE + "\t" + "\t" + "while(it.hasNext()) {");
    final String methodSingleValueName = determineConversionProperty2AttributeValueMethodName(relationship, false);
    write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName + "(it.next().asComplex()));");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "return result;");

    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private String generateConvertMethod4Association2PropertyCollectionValue(final JPAAssociationAttribute relationship,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    if (!relationship.isCollection()) {
      throw new IllegalStateException(relationship.getInternalName() + " is not a collection relationship");
    }
    final String methodCollectionName = determineConversionAttribute2PropertyValueMethodName(relationship, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);

    // on demand generation
    generateConvertMethod4Dto2ComplexValue(relationship, mapAlreadeGeneratedMethods);

    final String attrType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(relationship, true);

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + ">" + " " + methodCollectionName
        + "("
        + "final " + Collection.class.getName() + "<" + attrType + "> " + "values "
        + ") throws " + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(values == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return null;");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName()
        + ">" + " result = factory.newCollectionValue(\"" + relationship.getProperty().getType() + "\");");
    write(NEWLINE + "\t" + "\t" + "for(" + attrType + " entry: values) {");
    final String methodSingleValueName = determineConversionAttribute2PropertyValueMethodName(relationship, false);
    write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName + "(entry)" + ");");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return result;");
    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private String generateConvertMethod4Property2CollectionValue(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodCollectionName = determineConversionProperty2AttributeValueMethodName(attribute, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);
    // on demand generation
    if (attribute.isComplex()) {
      generateConvertMethod4ComplexValue2Dto(attribute, mapAlreadeGeneratedMethods);
    } else {
      generateConvertMethod4PrimitivePropertyValue2AttributeValue(attribute, mapAlreadeGeneratedMethods);
    }

    final String resultType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, true);

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + Collection.class.getName() + "<" + resultType + "> " + methodCollectionName + "(final "
        + "String propertyName, "
        + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + "> clientValue, "
        + "final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return " + Collections.class.getName() + ".emptyList();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + Collection.class.getName() + "<" + resultType + "> result = new " + ArrayList.class
        .getName()
        + "<" + resultType + ">(clientValue.size());");
    write(NEWLINE + "\t" + "\t" + Iterator.class.getName() + "<" + ClientValue.class.getName()
        + "> it = clientValue.iterator();");
    write(NEWLINE + "\t" + "\t" + "while(it.hasNext()) {");
    if (attribute.getType().isEnum()) {
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + METHOD_EXTRACT_ENUMVALUE
          + "(it.next(), " + attribute.getType().getName() + ".class" + "));");
    } else if (attribute.isComplex()) {
      final String methodSingleValueName = determineConversionProperty2AttributeValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName
          + "(it.next().asComplex()));");
    } else {
      final String methodSingleValueName = determineConversionProperty2AttributeValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName
          + "(propertyName, it.next().asPrimitive(), maxLength, precision, scale));");
    }
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return result;");
    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private String generateConvertMethod4Attribute2PropertyCollectionValue(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodCollectionName = determineConversionAttribute2PropertyValueMethodName(attribute, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);
    // on demand generation
    if (attribute.isComplex()) {
      generateConvertMethod4Dto2ComplexValue(attribute, mapAlreadeGeneratedMethods);
    } else {
      generateConvertMethodAttribute2PrimitiveValue(attribute, mapAlreadeGeneratedMethods);
    }

    final String attrType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, true);

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + ">" + " " + methodCollectionName
        + "(" + "final " + Collection.class.getName() + "<" + attrType + "> " + "values, "
        + "final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(values == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return null;");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName()
        + ">" + " result = factory.newCollectionValue(\"" + attribute.getProperty().getType() + "\");");
    write(NEWLINE + "\t" + "\t" + "for(" + attrType + " entry: values) {");
    if (attribute.getType().isEnum()) {
      write(NEWLINE + "\t" + "\t" + "\t" + "final " + ClientEnumValue.class.getName() + " enumValue = "
          + "(entry != null) ? " + "factory.newEnumValue(\"" + attribute.getType().getName() + "\", " + "entry.name()"
          + ") : null" + ";");
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(enumValue);");
    } else {
      final String methodSingleValueName = determineConversionAttribute2PropertyValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName + "(entry)" + ");");
    }
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return result;");
    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private void generateAllProperty2AttributeConversion(final JPAStructuredType sType, final boolean ownerISRootEntity,
      final Set<String> mapAlreadeGeneratedMethods) throws ODataJPAModelException,
  IOException {
    for (final JPAAssociationAttribute asso : sType.getAssociations()) {
      generateProperty2AssociationConversion(sType, ownerISRootEntity, asso);
    }
    // simple properties
    for (final JPAMemberAttribute attribute : sType.getAttributes()) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        generateProperty2AttributeConversion(sType, ownerISRootEntity, attribute);
        break;
      case EMBEDDED_ID:
        // handle properties of nested complex type (@EmbeddedId) as properties of this type
        for (final JPAMemberAttribute nestedProp : attribute.getStructuredType().getAttributes()) {
          generateProperty2AttributeConversion(sType, ownerISRootEntity, nestedProp);
        }
        break;
      case AS_COMPLEX_TYPE:
        generateProperty2AttributeConversion(sType, ownerISRootEntity, attribute);
        break;
      case RELATIONSHIP:
        throw new UnsupportedOperationException("Relationship '" + attribute.getInternalName()
        + "' must not occur here");
      }
    }
  }

  private void generateProperty2AssociationConversion(final JPAStructuredType ownerType, final boolean ownerISRootEntity,
      final JPAAssociationAttribute relationship) throws ODataJPAModelException, IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(relationship.getInternalName());
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(relationship, false);
    final String odataPropertyGetter = ownerISRootEntity ? "getProperty" : "get";
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(ownerType.getTypeClass().getSimpleName());
    if (relationship.isCollection()) {
      write(NEWLINE + "\t" + "\t" + "// collection relationship attribute value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(relationship.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + ">"
          + " ccv" + memberName + " = (prop" + memberName + " == null)? null : prop" + memberName
          + ".getCollectionValue();");
      final String methodName = determineConversionProperty2AttributeValueMethodName(relationship, true);
      write(NEWLINE + "\t" + "\t" + "final " + propClientType + " c" + memberName + " = " + methodName + "(" + "ccv"
          + memberName
          + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(relationship)
      + "(" + "c" + memberName + ");");

    } else {
      write(NEWLINE + "\t" + "\t" + "// relationship attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(relationship.getProperty())
          + ");");
      final String methodName = determineConversionProperty2AttributeValueMethodName(relationship, false);
      write(NEWLINE + "\t" + "\t" + "final " + ClientComplexValue.class.getName() + " cv" + memberName + " = (prop"
          + memberName + " == null) ? null : prop" + memberName + ".getComplexValue();");
      write(NEWLINE + "\t" + "\t" + propClientType + " dto" + memberName + " = " + methodName + "(" + "cv" + memberName
          + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(relationship)
      + "("
      + "dto"
      + memberName + ");");
    }
  }

  private void generateAssociation2PropertyConversion(final JPAStructuredType ownerType,
      final boolean ownerISRootEntity,
      final JPAAssociationAttribute relationship) throws ODataJPAModelException, IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(relationship.getInternalName());
    final String propertyContainerAdd = ownerISRootEntity ? ".getProperties()" : "";
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(ownerType.getTypeClass().getSimpleName());
    if (relationship.isCollection()) {
      write(NEWLINE + "\t" + "\t" + "// collection relationship property value");
      final String methodName = determineConversionAttribute2PropertyValueMethodName(relationship, true);
      write(NEWLINE + "\t" + "\t" + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName()
          + ">" + " prop" + memberName + "Value = " + methodName + "(" + "dto." + TypeDtoAPIWriter
          .determinePropertyGetterMethodName(relationship) + "()" + ");");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newCollectionProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(relationship.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    } else {
      write(NEWLINE + "\t" + "\t" + "// relationship property value");
      final String methodName = determineConversionAttribute2PropertyValueMethodName(relationship, false);
      write(NEWLINE + "\t" + "\t" + "final " + ClientComplexValue.class.getName() + " prop" + memberName + "Value"
          + " = "
          + methodName + "(" + "dto." + TypeDtoAPIWriter
          .determinePropertyGetterMethodName(relationship) + "()" + ");");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newComplexProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(relationship.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    }
  }

  /**
   * DTO Attribute -> Olingo Entity Property
   */
  private void generateAttribute2PropertyConversion(final JPAStructuredType ownerType, final boolean ownerISRootEntity,
      final JPAMemberAttribute attribute)
          throws ODataJPAModelException,
          IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(attribute.getInternalName());
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(ownerType.getTypeClass().getSimpleName());
    final String propertyContainerAdd = ownerISRootEntity ? ".getProperties()" : "";
    if (attribute.isCollection()) {
      // special handling for collection attributes
      write(NEWLINE + "\t" + "\t" + "// collection (" + (attribute.getType().isEnum() ? "enum" : attribute.isComplex()
          ? "complex" : "primitive") + ") property value");
      final String methodName = determineConversionAttribute2PropertyValueMethodName(attribute, true);
      write(NEWLINE + "\t" + "\t" + "final " + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName()
          + ">"
          + " prop" + memberName + "Value = "
          + methodName + "(" + "dto." + TypeDtoAPIWriter
          .determinePropertyGetterMethodName(attribute) + "()" + ", " + integer2CodeString(
              attribute.getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", "
              + integer2CodeString(attribute.getScale()) + ");");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newCollectionProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    } else if (attribute.getType().isEnum()) {
      write(NEWLINE + "\t" + "\t" + "// enum property value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientEnumValue.class.getName() + " prop" + memberName
          + "Value = " + "(dto." + TypeDtoAPIWriter.determinePropertyGetterMethodName(attribute) + "()" + "!= null) ? "
          + "factory.newEnumValue(\"" + attribute.getType().getName() + "\", " + "dto." + TypeDtoAPIWriter
          .determinePropertyGetterMethodName(attribute) + "()" + ".name()" + ") : null" + ";");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newEnumProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    } else if (attribute.isComplex()) {
      write(NEWLINE + "\t" + "\t" + "//complex value");
      final String methodName = determineConversionAttribute2PropertyValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "final " + ClientComplexValue.class.getName() + " prop" + memberName + "Value"
          + " = "
          + methodName + "(" + "dto." + TypeDtoAPIWriter
          .determinePropertyGetterMethodName(attribute) + "()" + ");");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newComplexProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    } else if (TypeMapping.convertToEdmSimpleType(attribute) == EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "\t" + "// binary property value for '" + attribute.getInternalName() + "' is ignored");
    } else {
      write(NEWLINE + "\t" + "\t" + "// primitive property value");
      final String methodName = determineConversionAttribute2PropertyValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "final " + ClientPrimitiveValue.class.getName() + " prop" + memberName
          + "Value = " + methodName + "(" + "dto." + TypeDtoAPIWriter.determinePropertyGetterMethodName(attribute)
          + "()" + ")" + ";");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = factory.newPrimitiveProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ", " + " prop" + memberName
          + "Value" + ");");
      write(NEWLINE + "\t" + "\t" + "propertyContainer" + propertyContainerAdd + ".add(" + " prop" + memberName + ");");
    }
  }

  /**
   * Olingo Entity Property -> DTO Attribute
   */
  private void generateProperty2AttributeConversion(final JPAStructuredType ownerType, final boolean ownerISRootEntity,
      final JPAMemberAttribute attribute)
          throws ODataJPAModelException,
          IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(attribute.getInternalName());
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(ownerType.getTypeClass().getSimpleName());
    final String odataPropertyGetter = ownerISRootEntity ? "getProperty" : "get";
    if (attribute.isCollection()) {
      // special handling for collection attributes
      write(NEWLINE + "\t" + "\t" + "// collection (" + (attribute.getType().isEnum() ? "enum" : attribute.isComplex()
          ? "complex" : "primitive") + ") attribute value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      final String methodName = determineConversionProperty2AttributeValueMethodName(attribute, true);
      write(NEWLINE + "\t" + "\t" + propClientType + " c" + memberName + " = " + methodName + "(" + "prop" + memberName
          + ".getName(), " + "prop" + memberName + ".getCollectionValue(), " + integer2CodeString(
              attribute.getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", "
              + integer2CodeString(attribute.getScale()) + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "c"
          + memberName + ");");
    } else if (attribute.getType().isEnum()) {
      // enums are generated as duplicate on client side (code) so we can reference directly the enum class
      write(NEWLINE + "\t" + "\t" + "// enum attribute value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "."
          + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + attribute.getType().getName() + " e" + memberName + " = " + METHOD_EXTRACT_ENUMVALUE
          + "(prop"
          + memberName + ".getValue(), " + attribute.getType().getName() + ".class);");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "e" + memberName + ");");
    } else if (attribute.isComplex()) {
      write(NEWLINE + "\t" + "\t" + "// complex type attribute value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      final String methodName = determineConversionProperty2AttributeValueMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "final " + propClientType + " dto" + memberName + " = " + methodName + "(" + "prop"
          + memberName + ".getComplexValue()" + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "dto"
          + memberName + ");");
    } else if (TypeMapping.convertToEdmSimpleType(attribute) == EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "\t" + "// binary attribute value for '" + attribute.getInternalName() + "' is ignored");
    } else {
      write(NEWLINE + "\t" + "\t" + "// primitive attribute value");
      write(NEWLINE + "\t" + "\t" + "final " + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      final String methodName = determineConversionProperty2AttributeValueMethodName(attribute, true);
      write(NEWLINE + "\t" + "\t" + propClientType + " r" + memberName + " = " + methodName + "(" + "prop" + memberName
          + ".getName(), " + "prop"
          + memberName + ".getPrimitiveValue(), " + integer2CodeString(attribute
              .getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", " + integer2CodeString(
                  attribute.getScale()) + ")" + ";");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "r" + memberName + ");");
    }
  }

  private String integer2CodeString(final Integer value) {
    if (value == null) {
      return "null";
    }
    return "Integer.valueOf(" + value.intValue() + ")";
  }

  public void writeProtocolCodeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
