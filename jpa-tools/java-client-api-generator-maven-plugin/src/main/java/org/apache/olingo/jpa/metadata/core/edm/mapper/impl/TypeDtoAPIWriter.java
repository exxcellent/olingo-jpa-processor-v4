package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

import edu.emory.mathcs.backport.java.util.Collections;

class TypeDtoAPIWriter extends AbstractWriter {

  private final String typeName;
  private final String typeMetaName;

  public TypeDtoAPIWriter(final File generationBaseDirectory, final String packageName, final String typeName) {
    super(generationBaseDirectory, packageName, determineTypeName(typeName));
    this.typeName = determineTypeName(typeName);
    this.typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(typeName);
  }

  public void writeDtoStart() throws IOException {
    createFile();
    write(HEADER_TEXT);
    write(NEWLINE + "public class " + typeName + " implements " + typeMetaName + " {");
  }

  static String determineTypeName(final String typeName) {
    return typeName + "Dto";
  }

  private static String determineBeanPropertyName(final JPAAttribute<?> attribute) {
    return attribute.getInternalName().substring(0, 1).toUpperCase(Locale.ENGLISH).concat(attribute.getInternalName()
        .substring(1));
  }

  static String determinePropertySetterMethodName(final JPAAttribute<?> attribute) {
    return "set" + determineBeanPropertyName(attribute);
  }

  private static <T extends JPAAttribute<?>> String determineServerSideTypeName(final T attribute)
      throws ODataJPAModelException {
    if (JPASimpleAttribute.class.isInstance(attribute)) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        return (JPASimpleAttribute.class.cast(attribute).getType().getName());
      case AS_COMPLEX_TYPE:
        return attribute.getStructuredType().getInternalName();
      default:
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
            JPASimpleAttribute.class.cast(attribute).getType().getName(),
            attribute.getInternalName());
      }
    } else {
      // relationship to another entity
      return attribute.getStructuredType().getInternalName();
    }
  }

  static <T extends JPAAttribute<?>> String determineClientSidePropertyJavaType(final T attribute)
      throws ODataJPAModelException {
    final StringBuilder buffer = new StringBuilder();
    if (attribute.isCollection()) {
      buffer.append(Collection.class.getName() + "<");
    }
    switch (attribute.getAttributeMapping()) {
    case SIMPLE:
      // simple types will have same name on client and server side
      buffer.append(JPASimpleAttribute.class.cast(attribute).getType().getName());
      break;
    default:
      // complex types are handled as 'DTO' on client side
      buffer.append(determineTypeName(determineServerSideTypeName(attribute)));
      break;
    }
    if (attribute.isCollection()) {
      buffer.append('>');
    }
    return buffer.toString();
  }

  @SuppressWarnings("unchecked")
  public void writeDtoTypeProperty(final JPAAttribute<?> attribute) throws IOException, ODataJPAModelException {
    List<JPAAttribute<?>> listProcessAttributes;
    if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
      // special case for complex key attribute type having nested attributes
      listProcessAttributes = new ArrayList<JPAAttribute<?>>(attribute.getStructuredType().getAttributes());
    } else {
      listProcessAttributes = Collections.singletonList(attribute);
    }

    for (final JPAAttribute<?> a : listProcessAttributes) {
      internalWriteDtoTypeProperty(a);
    }
  }

  private void internalWriteDtoTypeProperty(final JPAAttribute<?> attribute) throws IOException,
  ODataJPAModelException {
    final String memberName = attribute.getInternalName();
    final String beanName = determineBeanPropertyName(attribute);
    final String propClientType = determineClientSidePropertyJavaType(attribute);

    // attribute
    if (JPASimpleAttribute.class.isInstance(attribute) && JPASimpleAttribute.class.cast(attribute).getType()
        .isPrimitive()) {
      write(NEWLINE + NEWLINE + "\t" + "private " + propClientType + " " + memberName +";");
    } else {
      write(NEWLINE + NEWLINE + "\t" + "private " + propClientType + " " + memberName + " = null;");
    }
    // getter method
    if (attribute.getAttributeMapping() == AttributeMapping.RELATIONSHIP) {
      write(NEWLINE + "\t" + "/**");
      write(NEWLINE + "\t" + " * @return Will be <code>null</code> if not loaded via $expand");
      write(NEWLINE + "\t" + " * @see " + determineServerSideTypeName(attribute));
      write(NEWLINE + "\t" + " */");
    }
    write(NEWLINE + "\t" + "public " + propClientType + " get" + beanName + "() {");
    write(NEWLINE + "\t" + "\t" + "return " + memberName + ";");
    write(NEWLINE + "\t" + "}");
    // setter method
    write(NEWLINE + "\t" + "public void " + determinePropertySetterMethodName(attribute) + "(" + propClientType + " "
        + memberName + ") {");
    write(NEWLINE + "\t" + "\t" + "this." + memberName + " = " + memberName + ";");
    write(NEWLINE + "\t" + "}");
  }

  public void writeDtoEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
