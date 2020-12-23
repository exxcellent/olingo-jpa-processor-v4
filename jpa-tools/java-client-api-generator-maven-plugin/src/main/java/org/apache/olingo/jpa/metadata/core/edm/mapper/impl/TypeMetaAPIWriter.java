package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlNamed;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAParameterizedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class TypeMetaAPIWriter extends AbstractWriter {

  private final JPAStructuredType type;

  public TypeMetaAPIWriter(final File generationBaseDirectory, final JPAStructuredType st) {
    super(generationBaseDirectory, st.getTypeClass().getPackage()
        .getName(), determineTypeMetaName(st.getTypeClass().getSimpleName()));
    this.type = st;
  }

  public void writeMetaStart() throws IOException, ODataJPAModelException {
    createFile();
    write(HEADER_TEXT);
    final String typeMetaName = determineTypeMetaName(type.getTypeClass().getSimpleName());
    String extendsBaseClass = "";
    if (AbstractStructuredTypeJPA.class.isInstance(type)) {
      final JPAStructuredType base = AbstractStructuredTypeJPA.class.cast(type).getBaseType();
      if (base != null) {
        extendsBaseClass = " extends " + determineTypeMetaName(base.getTypeClass().getSimpleName());
      }
    }
    write(NEWLINE + "public interface " + typeMetaName + extendsBaseClass + " {");
  }

  static String determineTypeMetaName(final String typeName) {
    return typeName + "Meta";
  }

  private static String determineTypeMetaPropertyConstantName(final CsdlNamed prop) {
    return prop.getName().toUpperCase(Locale.ENGLISH);
  }

  static String determineTypeMetaPropertyNameConstantName(final CsdlNamed prop) {
    final String propNameUpperCase = determineTypeMetaPropertyConstantName(prop);
    return propNameUpperCase + "_NAME";
  }

  public void writePropertiesMetaInformations() throws ODataJPAModelException, IOException {
    final List<JPAMemberAttribute> simpleAttributes = type.getAttributes(false);
    final List<JPAAssociationAttribute> navigationAttributes = type.getAssociations();
    // navigation properties
    for (final JPAAssociationAttribute prop : navigationAttributes) {
      writeMetaTypeProperty(prop);
    }
    // simple properties
    for (final JPAMemberAttribute prop : simpleAttributes) {
      writeMetaTypeProperty(prop);
    }

  }

  private void writeMetaTypeProperty(final JPAAssociationAttribute attribute) throws IOException,
  ODataJPAModelException {
    final CsdlNavigationProperty prop = attribute.getProperty();
    write(NEWLINE + NEWLINE + "\t");
    write("public final static String " + determineTypeMetaPropertyNameConstantName(prop) + " = \"" + prop.getName()
    + "\";");
  }

  private void writeMetaTypeProperty(final JPAMemberAttribute attribute) throws IOException, ODataJPAModelException {
    if (attribute.getAttributeMapping() == AttributeMapping.EMBEDDED_ID) {
      // write attributes of embedded key type directly in that entity meta
      for (final JPAMemberAttribute prop : attribute.getStructuredType().getAttributes(false)) {
        writeMetaTypeProperty(prop);
      }
      return;
    }
    final CsdlProperty prop = attribute.getProperty();
    final String propNameUpperCase = determineTypeMetaPropertyConstantName(prop);
    write(NEWLINE);
    write(NEWLINE + "\t" + "public final static String " + determineTypeMetaPropertyNameConstantName(prop) + " = \""
        + prop.getName() + "\";");
    if (attribute.getAttributeMapping() == AttributeMapping.SIMPLE && !attribute.getType().isPrimitive()
        && determineEmdPrimitiveType(attribute) != EdmPrimitiveTypeKind.Binary) {
      write(NEWLINE + "\t" + "public final static Class<" + TypeDtoAPIWriter
          .determineClientSidePropertyRawJavaTypeName(attribute, false) + "> " + propNameUpperCase + "_DATATYPE = "
          + TypeDtoAPIWriter.determineClientSidePropertyRawJavaTypeName(attribute, false) + ".class;");
    }
    if (prop.getMaxLength() != null) {
      write(NEWLINE + "\t");
      write("public final static int " + propNameUpperCase + "_MAXLENGTH = " + prop.getMaxLength().intValue() + ";");
    }
    if (prop.getPrecision() != null) {
      write(NEWLINE + "\t");
      write("public final static int " + propNameUpperCase + "_PRECISION = " + prop.getPrecision().intValue() + ";");
    }
    if (prop.getScale() != null) {
      write(NEWLINE + "\t");
      write("public final static int " + propNameUpperCase + "_SCALE = " + prop.getScale().intValue() + ";");
    }
  }

  private EdmPrimitiveTypeKind determineEmdPrimitiveType(final JPAParameterizedElement attribute) {
    try {
      return TypeMapping.convertToEdmSimpleType(attribute);
    } catch (final ODataJPAModelException e) {
      return null;
    }
  }
  public void writeMetaEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
