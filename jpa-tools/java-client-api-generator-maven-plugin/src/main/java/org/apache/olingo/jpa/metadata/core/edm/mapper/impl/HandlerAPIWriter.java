package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.FilterFactory;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAttributeConversion;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;


class HandlerAPIWriter extends AbstractWriter {

  private static final String METHOD_EXTRACT_ENUMVALUE = "extractEnumValue";

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
  }

  private void generateFilterFactoryMethod() throws IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final " + FilterFactory.class.getCanonicalName() + " getFilterFactory()" + " {");
    write(NEWLINE + "\t" + "\t" + "return getClientInstance().getFilterFactory();");
    write(NEWLINE + "\t" + "}");
  }

  static String determineHandlerName(final JPAStructuredType type) {
    return type.getTypeClass().getSimpleName() + "AbstractHandler";
  }

  public void writeProtocolCode() throws IOException, ODataJPAModelException {
    generateGetEntity();
    generateConvertEntity();
  }

  private boolean entityContainsEnumAttribute() throws ODataJPAModelException {
    for (final JPAMemberAttribute prop : type.getAttributes()) {
      if (prop.getType().isEnum()) {
        return true;
      }
    }
    return false;
  }

  private void generateConvertEntity() throws IOException, ODataJPAModelException {
    if (entityContainsEnumAttribute()) {
      generate_ConvertEnumValue_HelperMethod();
    }
    final Set<String> mapAlreadeGeneratedMethods = new HashSet<>();
    generate_ConvertValue_HelperMethods(type, mapAlreadeGeneratedMethods);

    final String typeDtoName = TypeDtoAPIWriter.determineTypeName(type.getTypeClass().getSimpleName());

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + typeDtoName + " convert(" + ClientEntity.class.getName() + " odataObject"
        + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + typeDtoName + " dtoResult = new " + typeDtoName + "();");
    write(NEWLINE + "\t" + "\t" + "// convert generic response into DTO...");

    generateTypeAttributeConversion(type, true, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return dtoResult;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateGetEntity() throws IOException, ODataJPAModelException {
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
        + "throw new " + IllegalStateException.class.getSimpleName()
        + "(\"Unexpected status: \"+response.getStatusMessage());");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "\t" + ClientEntity.class.getName() + " odataEntity = response.getBody();");

    write(NEWLINE + "\t" + "\t" + "try {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return convert(odataEntity);");
    write(NEWLINE + "\t" + "\t" + "} finally {");
    write(NEWLINE + "\t" + "\t" + "\t" + "response.close();");
    write(NEWLINE + "\t" + "\t" + "}");
    write(NEWLINE + "\t" + "}");
  }

  private void generate_ConvertValue_HelperMethods(final JPAStructuredType ownerType,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    for (final JPAMemberAttribute attribute : ownerType.getAttributes()) {
      if (attribute.isCollection()) {
        generate_ConvertCollectionValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
      } else if (attribute.isComplex()) {
        if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
          // special case for complex key attribute type having nested attributes
          for (final JPAMemberAttribute embeddedIdAttribute : attribute.getStructuredType().getAttributes()) {
            generate_ConvertPrimitiveValue_HelperMethod(embeddedIdAttribute, mapAlreadeGeneratedMethods);
          }
        } else {
          generate_ConvertComplexValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
        }
      } else {
        generate_ConvertPrimitiveValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
      }
    }

    for (final JPAAssociationAttribute asso : ownerType.getAssociations()) {
      if (asso.isCollection()) {
        generate_ConvertAssociationCollection_HelperMethod(asso, mapAlreadeGeneratedMethods);
      } else {
        generate_ConvertAssociation_HelperMethod(asso, mapAlreadeGeneratedMethods);
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
  private String determineConversionMethodName(final JPAAttribute<?> attribute,
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

  private String generate_ConvertAssociation_HelperMethod(final JPAAssociationAttribute association,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodName = determineConversionMethodName(association, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodName);

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(association, false);

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + ClientComplexValue.class.getName()
        + " odataObject" + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + "if (odataObject == null) {");
    write(NEWLINE + "\t" + "\t" + "\t" + "return null;");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + propClientType + " dtoResult = new " + propClientType + "();");

    generateTypeAttributeConversion(association.getTargetEntity(), false, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return dtoResult;");
    write(NEWLINE + "\t" + "}");

    // on demand
    generate_ConvertValue_HelperMethods(association.getTargetEntity(), mapAlreadeGeneratedMethods);

    return methodName;
  }

  private String generate_ConvertComplexValue_HelperMethod(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodName = determineConversionMethodName(attribute, false);
    if (mapAlreadeGeneratedMethods.contains(methodName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodName);

    // on demand
    generate_ConvertValue_HelperMethods(attribute.getStructuredType(), mapAlreadeGeneratedMethods);

    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);

    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + propClientType + " " + methodName + "(" + "String propertyName, "
        + ClientComplexValue.class.getName() + " odataObject" + ") throws "
        + ODataException.class.getName() + " {");

    write(NEWLINE + "\t" + "\t" + propClientType + " dtoResult = new " + propClientType + "();");

    generateTypeAttributeConversion(attribute.getStructuredType(), false, mapAlreadeGeneratedMethods);

    write(NEWLINE + "\t" + "\t" + "return dtoResult;");
    write(NEWLINE + "\t" + "}");

    return methodName;
  }

  /**
   *
   * @return Method signature or <code>null</code> if no method is generated.
   */
  private String generate_ConvertPrimitiveValue_HelperMethod(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    EdmPrimitiveTypeKind odataType;
    try {
      odataType = TypeMapping.convertToEdmSimpleType(attribute);
    } catch (final ODataJPAModelException e) {
      return null;
    }
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false);
    final String methodName = determineConversionMethodName(attribute, false);
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
        generate_ConvertStatementsForDateTimeOffset(attribute.getType(), propClientType);
        break;
      case TimeOfDay:
      case Date:
      case Guid:
        generate_ConvertStatementsForStringBasedDefault(odataType, propClientType);
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

  private void generate_ConvertStatementsForDateTimeOffset(final Class<?> propJpaType, final String propClientType)
      throws IOException {
    if (propJpaType == LocalDateTime.class) {
      // convert into ZonedDateTime (avoiding time zone shifting problems) and that into LocalDateTime
      write(NEWLINE + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
          + EdmPrimitiveTypeFactory.class.getName() + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "."
          + EdmPrimitiveTypeKind.DateTimeOffset.name() + ");");
      write(NEWLINE + "\t" + "\t" + "String sV = propertyValue.toCastValue(String.class);");
      write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
      write(NEWLINE + "\t" + "\t"
          + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, "
          + ZonedDateTime.class.getCanonicalName() + ".class" + ").toLocalDateTime();");
      return;
    }
    // default
    generate_ConvertStatementsForStringBasedDefault(EdmPrimitiveTypeKind.DateTimeOffset, propClientType);
  }

  private void generate_ConvertStatementsForStringBasedDefault(final EdmPrimitiveTypeKind odataType,
      final String propClientType) throws IOException {
    // all these primitive types can be constructed from String
    write(NEWLINE + "\t" + "\t" + "\t" + "\t" + EdmPrimitiveType.class.getName() + " pType = "
        + EdmPrimitiveTypeFactory.class.getName() + ".getInstance(" + EdmPrimitiveTypeKind.class.getName() + "."
        + odataType.name() + ");");
    write(NEWLINE + "\t" + "\t" + "String sV = propertyValue.toCastValue(String.class);");
    write(NEWLINE + "\t" + "\t" + "//always nullable, always unicode");
    write(NEWLINE + "\t" + "\t"
        + "return pType.valueOfString(sV, Boolean.TRUE, maxLength, precision, scale, Boolean.TRUE, " + propClientType
        + ".class" + ");");
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

  private String generate_ConvertAssociationCollection_HelperMethod(final JPAAssociationAttribute relationship,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    if (!relationship.isCollection()) {
      throw new IllegalStateException(relationship.getInternalName()+" is not a collection relationship");
    }
    final String methodCollectionName = determineConversionMethodName(relationship, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);

    // on demand generation
    generate_ConvertAssociation_HelperMethod(relationship, mapAlreadeGeneratedMethods);

    final String resultType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(relationship, true);

    write(NEWLINE);
    write(NEWLINE + "\t" + "private "
        + Collection.class.getName() + "<" + resultType + "> " + methodCollectionName + "("
        + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + "> clientValue) throws "
        + ODataException.class.getName() + " {");

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
    final String methodSingleValueName = determineConversionMethodName(relationship, false);
    write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName + "(it.next().asComplex()));");
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "return result;");

    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private String generate_ConvertCollectionValue_HelperMethod(final JPAMemberAttribute attribute,
      final Set<String> mapAlreadeGeneratedMethods)
          throws ODataJPAModelException, IOException {
    final String methodCollectionName = determineConversionMethodName(attribute, true);
    if (mapAlreadeGeneratedMethods.contains(methodCollectionName)) {
      return null;
    }
    mapAlreadeGeneratedMethods.add(methodCollectionName);
    // on demand generation
    if (attribute.isComplex()) {
      generate_ConvertComplexValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
    } else {
      generate_ConvertPrimitiveValue_HelperMethod(attribute, mapAlreadeGeneratedMethods);
    }

    final String resultType = TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, true);

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
    if (attribute.getType().isEnum()) {
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + METHOD_EXTRACT_ENUMVALUE
          + "(it.next(), " + attribute.getType().getName() + ".class" + "));");
    } else if (attribute.isComplex()) {
      final String methodSingleValueName = determineConversionMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName
          + "(propertyName, it.next().asComplex()));");
    } else {
      final String methodSingleValueName = determineConversionMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + "\t" + "result.add(" + methodSingleValueName
          + "(propertyName, it.next().asPrimitive(), maxLength, precision, scale));");
    }
    write(NEWLINE + "\t" + "\t" + "}");

    write(NEWLINE + "\t" + "\t" + "return result;");

    write(NEWLINE + "\t" + "}");

    return methodCollectionName;
  }

  private void generateTypeAttributeConversion(final JPAStructuredType sType, final boolean ownerISRootEntity,
      final Set<String> mapAlreadeGeneratedMethods) throws ODataJPAModelException,
  IOException {
    for (final JPAAssociationAttribute asso : sType.getAssociations()) {
      generateAssociationConversion(sType, ownerISRootEntity, asso);
    }
    // simple properties
    for (final JPAMemberAttribute attribute : sType.getAttributes()) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        generateSimpleAttributeConversion(sType, ownerISRootEntity, attribute);
        break;
      case EMBEDDED_ID:
        // handle properties of nested complex type (@EmbeddedId) as properties of this type
        for (final JPAMemberAttribute nestedProp : attribute.getStructuredType().getAttributes()) {
          generateSimpleAttributeConversion(sType, ownerISRootEntity, nestedProp);
        }
        break;
      case AS_COMPLEX_TYPE:
        generateSimpleAttributeConversion(sType, ownerISRootEntity, attribute);
        break;
      case RELATIONSHIP:
        throw new UnsupportedOperationException("Relationship '" + attribute.getInternalName()
        + "' must not occur here");
      }
    }
  }

  private void generateAssociationConversion(final JPAStructuredType ownerType, final boolean ownerISRootEntity,
      final JPAAssociationAttribute relationship) throws ODataJPAModelException, IOException {
    final String memberName = qualifiedName2FirstCharacterUppercasedString(relationship.getInternalName());
    final String propClientType = TypeDtoAPIWriter.determineClientSidePropertyJavaTypeName(relationship, false);
    final String odataPropertyGetter = ownerISRootEntity ? "getProperty" : "get";
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(ownerType.getTypeClass().getSimpleName());
    if (relationship.isCollection()) {
      write(NEWLINE + "\t" + "\t" + "// collection relationship attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(relationship.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + ClientCollectionValue.class.getName() + "<" + ClientValue.class.getName() + ">"
          + " ccv" + memberName + " = (prop" + memberName + " == null)? null : prop" + memberName
          + ".getCollectionValue();");
      final String methodName = determineConversionMethodName(relationship, true);
      write(NEWLINE + "\t" + "\t" + propClientType + " c" + memberName + " = " + methodName + "(" + "ccv" + memberName
          + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(relationship)
      + "(" + "c" + memberName + ");");

    } else {
      write(NEWLINE + "\t" + "\t" + "// relationship attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(relationship.getProperty())
          + ");");
      final String methodName = determineConversionMethodName(relationship, false);
      write(NEWLINE + "\t" + "\t" + ClientComplexValue.class.getName() + " cv" + memberName + " = (prop" + memberName
          + " == null)? null : prop" + memberName + ".getComplexValue();");
      write(NEWLINE + "\t" + "\t" + propClientType + " dto" + memberName + " = " + methodName + "(" + "cv"
          + memberName + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(relationship)
      + "("
      + "dto"
      + memberName + ");");
    }
  }

  private void generateSimpleAttributeConversion(final JPAStructuredType ownerType, final boolean ownerISRootEntity,
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
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "." + TypeMetaAPIWriter
          .determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      final String methodName = determineConversionMethodName(attribute, true);
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
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "(" + typeMetaName + "."
          + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty()) + ");");
      write(NEWLINE + "\t" + "\t" + attribute.getType().getName() + " e" + memberName + " = " + METHOD_EXTRACT_ENUMVALUE
          + "(prop"
          + memberName + ".getValue(), " + attribute.getType().getName() + ".class);");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "e" + memberName + ");");
    } else if (attribute.isComplex()) {
      write(NEWLINE + "\t" + "\t" + "// complex type attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      final String methodName = determineConversionMethodName(attribute, false);
      write(NEWLINE + "\t" + "\t" + propClientType + " dto" + memberName + " = " + methodName + "(" + "prop"
          + memberName + ".getName(), " + "prop" + memberName + ".getComplexValue()" + ");");
      write(NEWLINE + "\t" + "\t" + "dtoResult." + TypeDtoAPIWriter.determinePropertySetterMethodName(attribute) + "("
          + "dto"
          + memberName + ");");
    } else if (TypeMapping.convertToEdmSimpleType(attribute) == EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "\t" + "// binary attribute value for '" + attribute.getInternalName() + "' is ignored");
    } else {
      write(NEWLINE + "\t" + "\t" + "// primitive attribute value");
      write(NEWLINE + "\t" + "\t" + ClientProperty.class.getName() + " prop" + memberName
          + " = odataObject." + odataPropertyGetter + "("
          + typeMetaName + "." + TypeMetaAPIWriter.determineTypeMetaPropertyNameConstantName(attribute.getProperty())
          + ");");
      final String methodName = determineConversionMethodName(attribute, true);
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
