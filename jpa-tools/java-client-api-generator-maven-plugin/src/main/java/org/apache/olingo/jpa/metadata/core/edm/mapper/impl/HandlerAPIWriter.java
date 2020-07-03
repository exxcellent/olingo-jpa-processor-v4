package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.request.cud.UpdateType;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.uri.FilterFactory;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;


class HandlerAPIWriter extends AbstractWriter {

  private static final String METHOD_CREATECONVERTER = "createConverter";

  private static class KeySorter implements Comparator<JPAMemberAttribute> {
    @Override
    public int compare(final JPAMemberAttribute o1, final JPAMemberAttribute o2) {
      return o1.getInternalName().compareTo(o2.getInternalName());
    }
  }

  @SuppressWarnings("unused")
  private final AbstractJPASchema schema;
  private final JPAStructuredType type;

  public HandlerAPIWriter(final File generationBaseDirectory, final AbstractJPASchema schema,
      final JPAStructuredType et) {
    super(generationBaseDirectory, et.getTypeClass().getPackage().getName(), determineHandlerName(et));
    this.schema = schema;
    this.type = et;
  }

  public void writeProtocolCodeStart() throws IOException {
    createFile();
    final String className = determineHandlerName(type);
    //    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(type.getTypeClass().getSimpleName());
    write(HEADER_TEXT);
    write(NEWLINE + "public abstract class " + className /* + " implements " + typeMetaName */ + " {");

    write(NEWLINE + "\t" + "private " + ODataClient.class.getName() + " clientInstance = null;");
    write(NEWLINE + "\t" + "private " + TypeConverterAPIWriter.determineConverterName(type) + " converter = null;");

    write(NEWLINE);
    write(NEWLINE + "\t" + "/**" + NEWLINE + "\t"
        + " * @return The service root URL as base part of the backend destination URL" + NEWLINE + "\t" + " **/");
    write(NEWLINE + "\t" + "protected abstract " + URI.class.getName() + " getServiceRootUrl();");

    write(NEWLINE);
    write(NEWLINE + "\t" + "/**" + NEWLINE + "\t"
        + " * @return The authorization header value for client request or <code>null</code> if no authorization is required"
        + NEWLINE + "\t" + " */");
    write(NEWLINE + "\t" + "protected abstract " + String.class.getSimpleName()
        + " determineAuthorizationHeaderValue();");

    write(NEWLINE);
    write(NEWLINE + "\t" + "/**" + NEWLINE + "\t"
        + " * Overwrite to create a more specialized client"
        + NEWLINE + "\t" + " */");
    write(NEWLINE + "\t" + "protected " + ODataClient.class.getName() + " createClient() {");
    write(NEWLINE + "\t" + "\t" + "return " + ODataClientFactory.class.getName() + ".getClient();");
    write(NEWLINE + "\t" + "}");

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected final " + ODataClient.class.getName() + " getClientInstance() {");
    write(NEWLINE + "\t" + "\t" + "if(clientInstance == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "clientInstance = createClient();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return clientInstance;");
    write(NEWLINE + "\t" + "}");

    final String builderName = URIBuilderWriter.determineAccessBuilderName(type);
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + builderName + " defineEndpoint() {");
    write(NEWLINE + "\t" + "\t" + "return new " + builderName + "(this);");
    write(NEWLINE + "\t" + "}");

    generateFilterFactoryMethod();
    generateConverterFactoryMethod();
    generateConverterGetterMethod();
  }

  private void generateFilterFactoryMethod() throws IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + FilterFactory.class.getCanonicalName() + " getFilterFactory()" + " {");
    write(NEWLINE + "\t" + "\t" + "return getClientInstance().getFilterFactory();");
    write(NEWLINE + "\t" + "}");
  }

  private void generateConverterFactoryMethod() throws IOException {
    final String converterName = TypeConverterAPIWriter.determineConverterName(type);
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + converterName + " " + METHOD_CREATECONVERTER + "()" + " {");
    write(NEWLINE + "\t" + "\t" + " return new " + converterName + "(getClientInstance().getObjectFactory());");
    write(NEWLINE + "\t" + "}");
  }

  private void generateConverterGetterMethod() throws IOException {
    final String converterName = TypeConverterAPIWriter.determineConverterName(type);
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected final " + converterName + " getConverter()" + " {");
    write(NEWLINE + "\t" + "\t" + "if(converter == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "converter = " + METHOD_CREATECONVERTER + "();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "return converter;");
    write(NEWLINE + "\t" + "}");
  }

  static String determineHandlerName(final JPAStructuredType type) {
    return type.getTypeClass().getSimpleName() + "AbstractHandler";
  }

  public void writeProtocolCode() throws IOException, ODataJPAModelException {
    generateGETEntity();
    generatePUTEntity();
  }

  /**
   * Generate method to send/save an modified entity
   */
  private void generatePUTEntity() throws IOException, ODataJPAModelException {
    if (type.isAbstract()) {
      return;
    }
    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + typeDtoName + " update(" + typeDtoName + " dto) throws "
        + ODataException.class.getName()
        + " {");
    write(NEWLINE + "\t" + "\t" + "final " + ClientEntity.class.getName() + " entity = getConverter().toEntity(dto);");

    // build entity uri
    boolean firstParam = true;
    final List<JPAMemberAttribute> keys = type.getKeyAttributes(true);
    keys.sort(new KeySorter());// sort always by name to have deterministic order
    final StringBuilder bufferKeyParameters = new StringBuilder();
    for (final JPAMemberAttribute attribute : keys) {
      final String beanName = TypeDtoAPIWriter.determineBeanPropertyName(attribute);

      if (!firstParam) {
        bufferKeyParameters.append(", ");
      }
      bufferKeyParameters.append("dto.get" + beanName + "()");
      firstParam = false;
    }
    write(NEWLINE + "\t" + "\t" + "final " + URI.class.getName() + " targetURI = defineEndpoint().appendKeySegment("
        + bufferKeyParameters.toString() + ").build();");
    // create/execute request
    write(NEWLINE + "\t" + "\t" + "final " + ODataEntityUpdateRequest.class.getName() + "<" + ClientEntity.class
        .getName()
        + "> request = getClientInstance().getCUDRequestFactory().getEntityUpdateRequest(targetURI, " + UpdateType.class
        .getName()
        + "." + UpdateType.REPLACE.name() + ", entity" + ");");
    write(NEWLINE + "\t" + "\t" + "final " + "String authHeader = determineAuthorizationHeaderValue();");
    write(NEWLINE + "\t" + "\t" + "if (authHeader != null && !authHeader.isEmpty()) {" + NEWLINE + "\t" + "\t" + "\t"
        + "request.addCustomHeader(" + HttpHeader.class.getName() + ".AUTHORIZATION" + ", authHeader);"
        + NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "final " + ODataEntityUpdateResponse.class.getName() + "<" + ClientEntity.class
        .getName()
        + "> response = request.execute();");

    write(NEWLINE + "\t" + "\t" + "if(response.getStatusCode() != 200) {");
    write(NEWLINE + "\t" + "\t" + "\t"
        + "throw new " + IllegalStateException.class.getSimpleName()
        + "(\"Unexpected status: \"+response.getStatusMessage());");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + ClientEntity.class.getName() + " odataEntity = response.getBody();");

    write(NEWLINE + "\t" + "\t" + "try {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return getConverter().toDto(odataEntity);");
    write(NEWLINE + "\t" + "\t" + "} finally {");
    write(NEWLINE + "\t" + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "}");
  }

  /**
   * Generate method to retrieve/load an entity
   */
  private void generateGETEntity() throws IOException, ODataJPAModelException {
    if (type.isAbstract()) {
      return;
    }
    boolean firstParam = true;
    final List<JPAMemberAttribute> keys = type.getKeyAttributes(true);
    keys.sort(new KeySorter());// sort always by name to have deterministic order
    final StringBuilder bufferKeyParameters = new StringBuilder();
    for (final JPAMemberAttribute attribute : keys) {
      final String memberName = attribute.getInternalName();
      final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(attribute, false);
      if (!firstParam) {
        bufferKeyParameters.append(", ");
      }
      bufferKeyParameters.append(propClientType + " " + memberName);
      firstParam = false;
    }

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());
    final String builderName = URIBuilderWriter.determineAccessBuilderName(type);
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + typeDtoName + " retrieve(" + builderName + " uriBuilder) throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if(!uriBuilder." + URIBuilderWriter.METHODNAME_SINGLEENTITYCALL + "()) {");
    write(NEWLINE + "\t" + "\t" + "\t"
        + "throw new " + IllegalArgumentException.class.getSimpleName()
        + "(\"URI builder represents not an single entity call\");");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "final " + ODataEntityRequest.class.getName() + "<" + ClientEntity.class.getName()
        + "> request = getClientInstance().getRetrieveRequestFactory().getEntityRequest(uriBuilder.build());");
    write(NEWLINE + "\t" + "\t" + "final " + "String authHeader = determineAuthorizationHeaderValue();");
    write(NEWLINE + "\t" + "\t" + "if (authHeader != null && !authHeader.isEmpty()) {" + NEWLINE + "\t" + "\t" + "\t"
        + "request.addCustomHeader(" + HttpHeader.class.getName() + ".AUTHORIZATION" + ", authHeader);"
        + NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "final " + ODataRetrieveResponse.class.getName() + "<" + ClientEntity.class.getName()
        + "> response = request.execute();");
    write(NEWLINE + "\t" + "\t" + "if(response.getStatusCode() != 200) {");
    write(NEWLINE + "\t" + "\t" + "\t"
        + "throw new " + IllegalStateException.class.getSimpleName()
        + "(\"Unexpected status: \"+response.getStatusMessage());");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + "final " + ClientEntity.class.getName() + " odataEntity = response.getBody();");

    write(NEWLINE + "\t" + "\t" + "try {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return getConverter().toDto(odataEntity);");
    write(NEWLINE + "\t" + "\t" + "} finally {");
    write(NEWLINE + "\t" + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "}");
  }

  public void writeProtocolCodeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
