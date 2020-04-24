package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.jpa.generator.api.client.generatorclassloader.LogWrapper;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

import edu.emory.mathcs.backport.java.util.Collections;

class TypeDtoAPIWriter extends AbstractWriter {

  private final JPAStructuredType type;
  private final LogWrapper log;

  public TypeDtoAPIWriter(final File generationBaseDirectory, final JPAStructuredType st, final LogWrapper log) {
    super(generationBaseDirectory, st.getTypeClass().getPackage().getName(), determineTypeName(st.getTypeClass()
        .getSimpleName()));
    this.type = st;
    this.log = log;
  }

  public void writeDtoStart() throws IOException {
    createFile();
    write(HEADER_TEXT);
    final String typeName = determineTypeName(type.getTypeClass().getSimpleName());
    final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(type.getTypeClass().getSimpleName());
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

  private static <T extends JPAAttribute<?>> String determineServerSideTypeName(final T attribute,
      final boolean convertPrimitiveClassToObjectClass)
          throws ODataJPAModelException {
    if (JPASimpleAttribute.class.isInstance(attribute)) {
      switch (attribute.getAttributeMapping()) {
      case SIMPLE:
        if (convertPrimitiveClassToObjectClass) {
          return convertPrimitiveClassToObjectClass(JPASimpleAttribute.class.cast(attribute).getType()).getName();
        }
        final Class<?> clazz = JPASimpleAttribute.class.cast(attribute).getType();
        if (clazz == byte[].class) {
          return "byte[]";
        }
        return clazz.getName();
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

  private static Class<?> convertPrimitiveClassToObjectClass(final Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (long.class == clazz) {
        return Long.class;
      }
      if (int.class == clazz) {
        return Integer.class;
      }
      if (short.class == clazz) {
        return Short.class;
      }
      if (double.class == clazz) {
        return Double.class;
      }
      if (float.class == clazz) {
        return Float.class;
      }
      if (char.class == clazz) {
        return Character.class;
      }
      if (byte.class == clazz) {
        return Byte.class;
      }
      if (boolean.class == clazz) {
        return Boolean.class;
      }
    }
    return clazz;
  }

  static <T extends JPAAttribute<?>> String determineClientSidePropertyRawJavaTypeName(final T attribute,
      final boolean convertPrimitiveClassToObjectClass)
          throws ODataJPAModelException {
    switch (attribute.getAttributeMapping()) {
    case SIMPLE:
      // simple types will have same name on client and server side
      return determineServerSideTypeName(attribute, convertPrimitiveClassToObjectClass);
    default:
      // complex types are handled as 'DTO' on client side
      return determineTypeName(determineServerSideTypeName(attribute, convertPrimitiveClassToObjectClass));
    }
  }

  /**
   * Same as {@link #determineClientSidePropertyRawJavaTypeName(JPAAttribute)} but will wrap the returned type into
   * ...Collection&lt;..&gt; for collections.
   */
  static <T extends JPAAttribute<?>> String determineClientSidePropertyJavaTypeName(final T attribute,
      final boolean convertPrimitiveClassToObjectClass)
          throws ODataJPAModelException {
    final StringBuilder buffer = new StringBuilder();
    if (attribute.isCollection()) {
      buffer.append(Collection.class.getName() + "<");
    }
    buffer.append(determineClientSidePropertyRawJavaTypeName(attribute, convertPrimitiveClassToObjectClass));
    if (attribute.isCollection()) {
      buffer.append('>');
    }
    return buffer.toString();
  }

  public void writeDtoTypeProperties() throws ODataJPAModelException, IOException {
    final List<JPASimpleAttribute> simpleAttributes = type.getAttributes();
    final List<JPAAssociationAttribute> navigationAttributes = type.getAssociations();
    String streamProperty = "";
    if (IntermediateEntityType.class.isInstance(type)) {
      if (IntermediateEntityType.class.cast(type).hasStream()) {
        streamProperty = IntermediateEntityType.class.cast(type).getStreamAttributePath().getLeaf().getInternalName();
      }
    }
    // navigation properties
    for (final JPAAssociationAttribute prop : navigationAttributes) {
      processAttribute(prop);
    }
    // simple properties
    for (final JPASimpleAttribute prop : simpleAttributes) {
      if (streamProperty.equals(prop.getInternalName())) {
        log.debug("Suppress stream property " + type.getExternalName() + "+" + prop.getInternalName() + " in API");
        continue;
      }
      processAttribute(prop);
    }
  }

  @SuppressWarnings("unchecked")
  private void processAttribute(final JPAAttribute<?> attribute) throws IOException, ODataJPAModelException {
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
    final String propClientType = determineClientSidePropertyJavaTypeName(attribute, false);

    // attribute
    if (JPASimpleAttribute.class.isInstance(attribute) && JPASimpleAttribute.class.cast(attribute).getType()
        .isPrimitive()) {
      write(NEWLINE + NEWLINE + "\t" + "private " + propClientType + " " + memberName +";");
    } else if (JPASimpleAttribute.class.isInstance(attribute) && attribute.isCollection()) {
      write(NEWLINE + NEWLINE + "\t" + "private " + propClientType + " " + memberName + " = new " + LinkedList.class
          .getName() + "<>();");
    } else {
      // all not primitive types (including 1:n relationships aka collections) are assigned to null as default
      write(NEWLINE + NEWLINE + "\t" + "private " + propClientType + " " + memberName + " = null;");
    }
    write(NEWLINE);
    // getter method
    if (attribute.getAttributeMapping() == AttributeMapping.RELATIONSHIP) {
      write(NEWLINE + "\t" + "/**");
      write(NEWLINE + "\t" + " * @return Will be <code>null</code> if not loaded via $expand");
      write(NEWLINE + "\t" + " * @see " + determineServerSideTypeName(attribute, false));
      write(NEWLINE + "\t" + " */");
    }
    write(NEWLINE + "\t" + "public " + propClientType + " get" + beanName + "() {");
    write(NEWLINE + "\t" + "\t" + "return " + memberName + ";");
    write(NEWLINE + "\t" + "}");
    // setter method
    write(NEWLINE);
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
