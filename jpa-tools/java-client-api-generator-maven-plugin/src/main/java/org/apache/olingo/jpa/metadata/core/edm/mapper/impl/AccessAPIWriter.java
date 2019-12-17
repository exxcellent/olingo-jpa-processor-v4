package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;


class AccessAPIWriter extends AbstractWriter {

  private static final String METHOD_EXTRACT_VALUE = "extractValue";
  private static final String METHOD_EXTRACT_COLLECTIONVALUE = "extractCollectionValue";

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
    // TODO
    generate_ExtractCollectionValue_HelperMethod();
    generate_ExtractValue_HelperMethod();
    generateGetEntity();
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

    write(NEWLINE + "\t" + "\t" + typeDtoName + " dto = new " + typeDtoName + "();");
    write(NEWLINE + "\t" + "\t" + "// convert generic response into DTO...");

    generateTypeAttributeConversion();

    write(NEWLINE + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "return dto;");
    write(NEWLINE + "\t" + "}");
  }

  private void generate_ExtractValue_HelperMethod() throws ODataJPAModelException, IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "@SuppressWarnings({ \"unchecked\", \"rawtypes\" })");
    write(NEWLINE + "\t" + "private " + "<T extends " + ClientValue.class.getName() + ", R>" + " R extractValue("
        + "T clientValue, " + Class.class.getSimpleName()
        + "<R> resultTypeClass, final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue.isEnum()) {");
    // enum handling code
    // enums are generated as duplicate on client side (code) so we can reference directly the enum class
    write(NEWLINE + "\t" + "\t" + "\t" + "final String sV = clientValue.asEnum().getValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "return (R)Enum.valueOf((Class<Enum>)resultTypeClass, sV);");
    write(NEWLINE + "\t" + "\t" + "} else {");
    // primitive type handling code
    write(NEWLINE + "\t" + "\t" + "\t" + "// primitive attribute value");
    write(NEWLINE + "\t" + "\t" + "\t" + EdmPrimitiveTypeKind.class.getName()
        + " edmType = clientValue.asPrimitive().getTypeKind();");
    write(NEWLINE + "\t" + "\t" + "\t" + "switch(edmType) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "case " + EdmPrimitiveTypeKind.DateTimeOffset.name() + ":");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
        + EdmPrimitiveTypeFactory.class
        .getName() + ".getInstance(edmType);");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "\t" + "String sV = (String)clientValue.asPrimitive().toValue();");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "\t" + "//always nullable, always unicode");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "\t"
        + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, resultTypeClass);");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "default:");
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + "\t"
        + "return clientValue.asPrimitive().toCastValue(resultTypeClass);");
    write(NEWLINE + "\t" + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "}");
  }

  private void generate_ExtractCollectionValue_HelperMethod() throws ODataJPAModelException, IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "private " + "<R> "
        + Collection.class.getName() + "<R> extractCollectionValue("
        + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + "> clientValue, " + Class.class
        .getSimpleName()
        + "<R> resultTypeClass, final Integer maxLength, final Integer precision, final Integer scale) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(clientValue == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return " + Collections.class.getName() + ".emptyList();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + Collection.class.getName() + "<R> result = new " + ArrayList.class.getName()
        + "<R>(clientValue.size());");
    write(NEWLINE + "\t" + "\t" + Iterator.class.getName() + "<" + ClientValue.class.getName()
        + "> it = clientValue.iterator();");
    write(NEWLINE + "\t" + "\t" + "while(it.hasNext()) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + METHOD_EXTRACT_VALUE
        + "(it.next(), resultTypeClass, maxLength, precision, scale));");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "return result;");

    write(NEWLINE + "\t" + "}");

  }

  private void generateTypeAttributeConversion() throws ODataJPAModelException, IOException {
    for (final JPAAssociationAttribute prop : type.getAssociations()) {
      // TODO
    }
    // simple properties
    for (final JPASimpleAttribute attribute : type.getAttributes()) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        generateTypeSimpleAttributeConversion(attribute);
        break;
      case EMBEDDED_ID:
        for (final JPASimpleAttribute nestedProp : attribute.getStructuredType().getAttributes()) {
          generateTypeSimpleAttributeConversion(nestedProp);
        }
        break;
      case AS_COMPLEX_TYPE:
        System.out.println("Support CT " + attribute.getInternalName());
        break;
      case RELATIONSHIP:
        throw new UnsupportedOperationException("Relationship '" + attribute.getInternalName()
        + "' must not occur here");
      }
    }

  }

  private void generateTypeSimpleAttributeConversion(final JPASimpleAttribute attribute) throws ODataJPAModelException,
  IOException {
    final String memberName = attribute.getInternalName();
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
    final String propClientTypePrimitiveAsObject = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute,
        true);
    if (attribute.isCollection()) {
      // special handling for collection attributes
      write(NEWLINE + "\t" + "\t" + "// collection attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + propClientType + " c" + memberName + " = " + METHOD_EXTRACT_COLLECTIONVALUE + "("
          + "prop" + memberName + ".getCollectionValue(), " + TypeDtoAPIWriter
          .determineClientSidePropertyRawJavaTypeName(attribute, true) + ".class" + ", " + integer2CodeString(
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
      write(NEWLINE + "\t" + "\t" + attribute.getType().getName() + " e" + memberName + " = " + METHOD_EXTRACT_VALUE
          + "(prop"
          + memberName + ".getValue(), " + attribute.getType().getName() + ".class, null, null, null);");
      write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "e" + memberName + ");");
    } else if (TypeMapping.convertToEdmSimpleType(attribute) == EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "\t" + "// binary attribute value for '" + attribute.getInternalName() + "' is ignored");
    } else {
      write(NEWLINE + "\t" + "\t" + "// primitive attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataEntity.getProperty("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      write(NEWLINE + "\t" + "\t" + propClientType + " r" + memberName + " = " + METHOD_EXTRACT_VALUE + "(prop"
          + memberName
          + ".getValue(), " + propClientTypePrimitiveAsObject + ".class, " + integer2CodeString(attribute
              .getMaxLength()) + ", "
              + integer2CodeString(attribute.getPrecision()) + ", " + integer2CodeString(attribute.getScale()) + ")"
              + determinePrimitiveTypeCastSuffix(attribute) + ";");
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

  private String determinePrimitiveTypeCastSuffix(final JPASimpleAttribute attribute) {
    final Class<?> clazz = attribute.getType();
    if (clazz.isPrimitive()) {
      if (long.class == clazz) {
        return ".longValue()";
      }
      if (int.class == clazz) {
        return ".intValue()";
      }
      if (short.class == clazz) {
        return ".shortValue()";
      }
      if (double.class == clazz) {
        return ".doubleValue()";
      }
      if (float.class == clazz) {
        return ".floatValue()";
      }
      if (char.class == clazz) {
        return ".charValue()";
      }
      if (byte.class == clazz) {
        return ".byteValue()";
      }
      if (boolean.class == clazz) {
        return ".booleanValue()";
      }
    }
    // default
    return "";
  }

  public void writeProtocolCodeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
