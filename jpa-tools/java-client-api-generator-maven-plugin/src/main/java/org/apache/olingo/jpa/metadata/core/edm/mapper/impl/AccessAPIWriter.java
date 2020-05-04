package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;


class AccessAPIWriter extends AbstractWriter {

  private static final String METHOD_EXTRACT_ENUMVALUE = "extractEnumValue";
  private static final List<EdmPrimitiveTypeKind> SUPPORTED_PRIMITIVE_TYPES = Arrays.asList(
      EdmPrimitiveTypeKind.Boolean, EdmPrimitiveTypeKind.Byte, EdmPrimitiveTypeKind.DateTimeOffset,
      EdmPrimitiveTypeKind.Decimal, EdmPrimitiveTypeKind.Double, EdmPrimitiveTypeKind.Int16, EdmPrimitiveTypeKind.Int32,
      EdmPrimitiveTypeKind.Int64, EdmPrimitiveTypeKind.String);

  private static class KeySorter implements Comparator<JPASimpleAttribute> {
    @Override
    public int compare(final JPASimpleAttribute o1, final JPASimpleAttribute o2) {
      return o1.getInternalName().compareTo(o2.getInternalName());
    }
  }

  private final AbstractJPASchema schema;
  private final JPAStructuredType type;
  private final String typeMetaName;

  public AccessAPIWriter(final File generationBaseDirectory, final AbstractJPASchema schema,
      final JPAStructuredType et) {
    super(generationBaseDirectory, et.getTypeClass().getPackage()
        .getName(), determineAccessName(et.getTypeClass().getSimpleName()));
    this.schema = schema;
    this.type = et;
    typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(type.getTypeClass().getSimpleName());
  }

  public void writeProtocolCodeStart() throws IOException {
    createFile();
    final String className = determineAccessName(type.getTypeClass().getSimpleName());
    //    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(type.getTypeClass().getSimpleName());
    write(HEADER_TEXT);
    write(NEWLINE + "public abstract class " + className /* + " implements " + typeMetaName */ + " {");

    write(NEWLINE + "\t" + "private " + ODataClient.class.getName() + " clientInstance = null;");

    write(NEWLINE + "\t" + "/**" + NEWLINE + "\t"
        + " * @return The service root URL as base part of the backend destination URL" + NEWLINE + "\t" + " **/");
    write(NEWLINE + "\t" + "protected abstract " + URI.class.getName() + " getServiceRootUrl();");
    write(NEWLINE);

    write(NEWLINE + "\t" + "/**" + NEWLINE + "\t"
        + " * @return The authorization header value for client request or <code>null</code> if no authorization is required"
        + NEWLINE + "\t" + " **/");
    write(NEWLINE + "\t" + "protected abstract " + String.class.getSimpleName()
        + " determineAuthorizationHeaderValue();");
    write(NEWLINE);

    write(NEWLINE + "\t" + "protected " + ODataClient.class.getName() + " createClient() {");
    write(NEWLINE + "\t" + "\t" + "return " + ODataClientFactory.class.getName() + ".getClient();");
    write(NEWLINE + "\t" + "}");
    write(NEWLINE);

    write(NEWLINE + "\t" + "protected " + ODataClient.class.getName() + " getClientInstance() {");
    write(NEWLINE + "\t" + "\t" + "if(clientInstance == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "clientInstance = createClient();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return clientInstance;");
    write(NEWLINE + "\t" + "}");
    write(NEWLINE);

    write(NEWLINE + "\t" + "protected " + URIBuilder.class.getName() + " defineEndpoint() {");
    write(NEWLINE + "\t" + "\t" + "return getClientInstance().newURIBuilder(getServiceRootUrl().toString());");
    write(NEWLINE + "\t" + "}");
  }

  private static String determineAccessName(final String typeName) {
    return "Abstract" + typeName + "Access";
  }


  public void writeProtocolCode() throws IOException, ODataJPAModelException {
    if (entityContainsEnumAttribute()) {
      generate_ConvertEnumValue_HelperMethod();
    }
    generate_ConvertValue_HelperMethods();
    generateGetEntity();
    generateConvertEntity();
  }

  private boolean entityContainsEnumAttribute() throws ODataJPAModelException {
    for (final JPASimpleAttribute prop : type.getAttributes()) {
      if (prop.getType().isEnum()) {
        return true;
      }
    }
    return false;
  }

  private void generateConvertEntity() throws IOException, ODataJPAModelException {
    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + typeDtoName + " convert(" + ClientEntity.class.getName() + " odataEntity"
        + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + typeDtoName + " dto = new " + typeDtoName + "();");
    write(NEWLINE + "\t" + "\t" + "// convert generic response into DTO...");

    generateTypeAttributeConversion();

    write(NEWLINE + "\t" + "\t" + "return dto;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateGetEntity() throws IOException, ODataJPAModelException {
    if (type.isAbstract()) {
      return;
    }
    boolean firstParam = true;
    final List<JPASimpleAttribute> keys = type.getKeyAttributes(true);
    keys.sort(new KeySorter());// sort always by name to have deterministic order
    final StringBuilder bufferKeyParameters = new StringBuilder();
    for (final JPASimpleAttribute attribute : keys) {
      final String memberName = attribute.getInternalName();
      final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
      if (!firstParam) {
        bufferKeyParameters.append(", ");
      }
      bufferKeyParameters.append(propClientType + " " + memberName);
      firstParam = false;
    }

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + typeDtoName + " retrieve(" + bufferKeyParameters.toString() + ") throws "
        + ODataException.class.getName() + " {");
    write(NEWLINE + "\t" + "\t" + Map.class.getName() + "<String, Object> keys = new " + HashMap.class.getName()
        + "<>();");
    for (final JPASimpleAttribute attribute : keys) {
      final String memberName = attribute.getInternalName();
      //      write(NEWLINE + "\t" + "\t" + "keys.put(\"" + attribute.getExternalName() + "\", " + memberName + ");");
      write(NEWLINE + "\t" + "\t" + "keys.put(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute
              .getProperty()) + ", " + memberName + ");");
    }

    final String esName;
    if (JPAEntityType.class.isInstance(type)) {
      esName = JPAEntityType.class.cast(type).getEntitySetName();
    } else {
      esName = schema.getNameBuilder().buildEntitySetName(type.getExternalName());
    }
    write(NEWLINE + "\t" + "\t" + URIBuilder.class.getName() + " uriBuilder = "
        + " defineEndpoint().appendEntitySetSegment(\""
        + esName + "\").appendKeySegment(keys)" + ";");

    write(NEWLINE + "\t" + "\t" + ODataEntityRequest.class.getName() + "<" + ClientEntity.class.getName()
        + "> request = getClientInstance().getRetrieveRequestFactory().getEntityRequest(uriBuilder.build());");
    write(NEWLINE + "\t" + "\t" + "String authHeader = determineAuthorizationHeaderValue();");
    write(NEWLINE + "\t" + "\t" + "if (authHeader != null && !authHeader.isEmpty()) {" + NEWLINE + "\t" + "\t" + "\t"
        + "request.addCustomHeader(" + HttpHeader.class.getName() + ".AUTHORIZATION" + ", authHeader);"
        + NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + ODataRetrieveResponse.class.getName() + "<" + ClientEntity.class.getName()
        + "> response = request.execute();");
    write(NEWLINE + "\t" + "\t" + "if(response.getStatusCode() != 200) {");
    write(NEWLINE + "\t" + "\t" + "\t"
        + "throw new IllegalStateException(\"Unexpected status: \"+response.getStatusMessage());");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + ClientEntity.class.getName() + " odataEntity = response.getBody();");

    write(NEWLINE + "\t" + "\t" + "try {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return convert(odataEntity);");
    write(NEWLINE + "\t" + "\t" + "} finally {");
    write(NEWLINE + "\t" + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "}");
  }

  private void generate_ConvertValue_HelperMethods() throws ODataJPAModelException, IOException {
    final Set<String> mapAlreadeGeneratedMethods = new HashSet<>();
    for (final JPASimpleAttribute attribute : type.getAttributes()) {
      if (attribute.isCollection()) {
        generate_ConvertCollectionValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
      } else if (attribute.isComplex()) {
        if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
          // special case for complex key attribute type having nested attributes
          for (final JPASimpleAttribute embeddedIdAttribute : attribute.getStructuredType().getAttributes()) {
            generate_ConvertPrimitiveValue_HelperMethod(embeddedIdAttribute, mapAlreadeGeneratedMethods);
          }
        } else {
          generate_ConvertComplexValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
        }

      } else {
        generate_ConvertPrimitiveValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
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
  private String determineConversionMethodName(final JPASimpleAttribute attribute,
      final boolean respectCollection) throws ODataJPAModelException {
    String fromName;
    if (attribute.isComplex()) {
      fromName = attribute.getExternalName();
    } else {
      final EdmPrimitiveTypeKind odataType = TypeMapping.convertToEdmSimpleType(attribute);
      fromName = odataType.name();
    }
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    return "convertOData" + (attribute.isCollection() && respectCollection ? "CollectionOf" : "") + fromName
        + "To"
        + qualifiedName2FirstCharacterUppercasedString(
            propClientType);
  }

  private String generate_ConvertComplexValue_HelperMethod(final JPASimpleAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
    final String methodName = determineConversionMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + "String propertyName, "
        + ClientPrimitiveValue.class.getName() + " propertyValue, "
        + "final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    // FIXME

    write(NEWLINE + "\t" + "}");

    mapAlreadeGeneratedMethods.add(methodName);
    return methodName;

  }

  /**
   *
   * @return Method signature or <code>null</code> if no method is generated.
   */
  private String generate_ConvertPrimitiveValue_HelperMethod(final JPASimpleAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    EdmPrimitiveTypeKind odataType;
    try {
      odataType = TypeMapping.convertToEdmSimpleType(attribute);
    } catch (final ODataJPAModelException e) {
      return null;
    }
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
    final String methodName = determineConversionMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + "String propertyName, "
        + ClientPrimitiveValue.class.getName() + " propertyValue, "
        + "final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");
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
    case DateTimeOffset:
      write(NEWLINE + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
          + EdmPrimitiveTypeFactory.class
          .getName() + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "."
          + EdmPrimitiveTypeKind.DateTimeOffset.name() + ");");
      write(NEWLINE + "\t" + "\t" + "String sV = (String)propertyValue.toValue();");
      write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
      write(NEWLINE + "\t" + "\t"
          + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, " + propClientType
          + ".class" + ");");
      break;
    default:
      write(NEWLINE + "\t" + "\t" + "throw new UnsupportedOperationException();");
    }

    write(NEWLINE + "\t" + "}");

    mapAlreadeGeneratedMethods.add(methodName);
    return methodName;
  }

  private void generate_ConvertEnumValue_HelperMethod() throws ODataJPAModelException, IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "@SuppressWarnings({ \"unchecked\", \"rawtypes\" })");
    write(NEWLINE + "\t" + "private " + "<T extends " + ClientValue.class.getName() + ", R>" + " R "
        + METHOD_EXTRACT_ENUMVALUE + "(" + "T clientValue, " + Class.class.getSimpleName()
        + "<R> resultTypeClass) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue.isEnum()) {");
    // enum handling code
    // enums are generated as duplicate on client side (code) so we can reference directly the enum class
    write(NEWLINE + "\t" + "\t" + "\t" + "final String sV = clientValue.asEnum().getValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return (R)Enum.valueOf((Class<Enum>)resultTypeClass, sV);");
    write(NEWLINE + "\t" + "\t" + "} else if(resultTypeClass.isEnum()) {");
    // if client side type is NOT enum, but JPA type... so it's safe to cast to string
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "String sV = (String)clientValue.asPrimitive().toValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return (R)Enum.valueOf((Class<Enum>)resultTypeClass, sV);");
    write(NEWLINE + "\t" + "\t" + "} else {");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "throw new " + UnsupportedOperationException.class.getSimpleName()
        + "(resultTypeClass.getName());");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "}");
  }

  private String generate_ConvertCollectionValue_HelperMethod(final JPASimpleAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodCollectionName = determineConversionMethodName(attribute, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    final String methodSingleValueName = determineConversionMethodName(attribute, false);
    final String resultType = TypeDtoAPIWriter
        .determineClientSidePropertyRawJavaTypeName(attribute, true);

    // on demand generation
    if (attribute.isComplex()) {
      generate_ConvertComplexValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
    } else {
      generate_ConvertPrimitiveValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
    }

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + Collection.class.getName() + "<" + resultType + "> " + methodCollectionName + "(" + "String propertyName, "
        + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + "> clientValue, "
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
    write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName
        + "(propertyName, it.next().asPrimitive(), maxLength, precision, scale));");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "return result;");

    write(NEWLINE + "\t" + "}");

    mapAlreadeGeneratedMethods.add(methodCollectionName);
    return methodCollectionName;
  }

  private void generateTypeAttributeConversion() throws ODataJPAModelException, IOException {
    for (final JPAAssociationAttribute asso : type.getAssociations()) {
      // TODO
      System.out.println("Support REL " + type.getInternalName() + " : " + asso.getInternalName());// TODO
    }
    // simple properties
    for (final JPASimpleAttribute attribute : type.getAttributes()) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        generateAttributeConversion(attribute);
        break;
      case EMBEDDED_ID:
        for (final JPASimpleAttribute nestedProp : attribute.getStructuredType().getAttributes()) {
          generateAttributeConversion(nestedProp);
        }
        break;
      case AS_COMPLEX_TYPE:
        System.out.println("Support CT " + type.getInternalName() + " : " + attribute.getInternalName());// TODO
        generateAttributeConversion(attribute);
        break;
      case RELATIONSHIP:
        throw new UnsupportedOperationException("Relationship '" + attribute.getInternalName()
        + "' must not occur here");
      }
    }
  }

  private void generateAttributeConversion(final JPASimpleAttribute attribute) throws ODataJPAModelException,
  IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(attribute.getInternalName());
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
    if (attribute.isCollection()) {
      // special handling for collection attributes
      write(NEWLINE + "\t" + "\t" + "// collection (" + (attribute.getType().isEnum() ? "enum" : attribute.isComplex()
          ? "complex" : "primitive") + ") attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      final String methodName = determineConversionMethodName(attribute, true);
      write(NEWLINE + "\t" + "\t" + propClientType + " c" + memberName + " = " + methodName + "(" + "prop" + memberName
          + ".getName(), " + "prop" + memberName + ".getCollectionValue(), " + integer2CodeString(
              attribute.getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", "
              + integer2CodeString(attribute.getScale()) + ");");
      write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "(" + "c"
          + memberName + ");");
    } else if (attribute.getType().isEnum()) {
      // enums are generated as duplicate on client side (code) so we can reference directly the enum class
      write(NEWLINE + "\t" + "\t" + "// enum attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty(" + typeMetaName + "."
          + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + attribute.getType().getName() + " e" + memberName + " = " + METHOD_EXTRACT_ENUMVALUE
          + "(prop"
          + memberName + ".getValue(), " + attribute.getType().getName() + ".class);");
      write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "e" + memberName + ");");
    } else if (attribute.isComplex()) {
      write(NEWLINE + "\t" + "\t" + "// complex type attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
    } else if (TypeMapping.convertToEdmSimpleType(attribute) == EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "\t" + "// binary attribute value for '" + attribute.getInternalName() + "' is ignored");
    } else {
      write(NEWLINE + "\t" + "\t" + "// primitive attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      final String methodName = determineConversionMethodName(attribute, true);
      write(NEWLINE + "\t" + "\t" + propClientType + " r" + memberName + " = " + methodName + "(" + "prop" + memberName
          + ".getName(), " + "prop"
          + memberName + ".getPrimitiveValue(), " + integer2CodeString(attribute
              .getMaxLength()) + ", " + integer2CodeString(attribute.getPrecision()) + ", " + integer2CodeString(
                  attribute.getScale()) + ")" + ";");
      write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
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
