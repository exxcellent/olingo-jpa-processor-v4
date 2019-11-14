package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.provider.CsdlNamed;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
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
    if (IntermediateStructuredType.class.isInstance(type)) {
      final JPAStructuredType base = IntermediateStructuredType.class.cast(type).getBaseType();
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

  public void writeMetaTypeProperty(final CsdlNavigationProperty prop) throws IOException {
    write(NEWLINE + NEWLINE + "\t");
    write("public final static String " + determineTypeMetaPropertyNameConstantName(prop) + " = \"" + prop.getName()
    + "\";");
  }

  public void writeMetaTypeProperty(final CsdlProperty prop) throws IOException {
    final String propNameUpperCase = determineTypeMetaPropertyConstantName(prop);
    write(NEWLINE + NEWLINE + "\t");
    write("public final static String " + determineTypeMetaPropertyNameConstantName(prop) + " = \"" + prop.getName()
    + "\";");
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

  public void writeMetaEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
