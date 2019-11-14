package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class AccessAPIWriter extends AbstractWriter {

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
    write(NEWLINE + "\t" + "\t" + "if(clientInstance == null) {clientInstance = createClient();}");
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
    generateAttributeValueConversionHelperMethod();
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
      final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaType(attribute);
      if (!firstParam) {
        bufferKeyParameters.append(", ");
      }
      bufferKeyParameters.append(propClientType + " " + memberName);
      firstParam = false;
    }

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());
    write(NEWLINE);
    write(NEWLINE + "\t" + "public " + typeDtoName + " retrieve(" + bufferKeyParameters.toString() + ") throws "
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

    final String esName = schema.getNameBuilder().buildEntitySetName(type.getExternalFQN());
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
    write(NEWLINE + "\t" + "\t" + ClientEntity.class.getName() + " odataEntity = response.getBody();");

    write(NEWLINE + "\t" + "\t" + typeDtoName + " dto = new " + typeDtoName + "();");
    generateTypeAttributeConversion();
    write(NEWLINE + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "return dto;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateAttributeValueConversionHelperMethod() throws ODataJPAModelException, IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "private " + "<T extends " + ClientValue.class.getName() + ", R>" + " R extractValue(" + "T"
        + " clientValue, " + EdmPrimitiveTypeKind.class.getName() + " edmType, " + Class.class.getSimpleName()
        + "<R> resultTypeClass) {");

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
      }
    }

  }

  private void generateTypeSimpleAttributeConversion(final JPASimpleAttribute attribute) throws ODataJPAModelException,
  IOException {
    final String memberName = attribute.getInternalName();
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaType(attribute);
    if (attribute.isCollection()) {
      // special handling for collection attributes
      write(NEWLINE + "\t" + "\t" + propClientType + " l" + memberName + " = new " + LinkedList.class.getName() + "<>()"
          + ";");
      write(NEWLINE + "\t" + "\t" + Iterator.class.getName() + "<" + ClientValue.class.getName() + "> it" + memberName
          + " = odataEntity.getProperty(" + typeMetaName + "."
          + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ").getCollectionValue().iterator();");
      write(NEWLINE + "\t" + "\t" + "while(it" + memberName + ".hasNext()) {");
      write(NEWLINE + "\t" + "\t" + "\t" + "l" + memberName + ".add(extractValue(it" + memberName + ".next(), "
          + attribute.getType().getName() + ".class));");
      write(NEWLINE + "\t" + "\t" + "}");
    } else if (attribute.getType().isEnum()) {
      // enums are generated as duplicate on client side (code) so we can reference directly the enum class
      write(NEWLINE + "\t" + "\t" + "String s" + memberName + " = odataEntity.getProperty(" + typeMetaName + "."
          + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ").getEnumValue().getValue();");
      write(NEWLINE + "\t" + "\t" + attribute.getType().getName() + " e" + memberName + " = " + attribute.getType()
      .getName() + ".valueOf("
      + "s" + memberName + ")" + ";");
      write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "e" + memberName + ");");
    } else {
      final EdmPrimitiveTypeKind edmType = TypeMapping.convertToEdmSimpleType(attribute);
      switch (edmType) {
      case String:
        // TODO
      default:
        write(NEWLINE + "\t" + "\t" + propClientType + " r" + memberName + " = odataEntity.getProperty(" + typeMetaName
            + "."
            + TypeMetaAPIWriter
            .determineTypeMetaPropertyNameConstantName(attribute
                .getProperty()) + ").getPrimitiveValue().toCastValue(" + propClientType + ".class" + ");");
        write(NEWLINE + "\t" + "\t" + "dto." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
            + "r" + memberName + ");");
      }
    }
  }

  public void writeProtocolCodeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
