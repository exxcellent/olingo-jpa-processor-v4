package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;

import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;

class EnumAPIWriter extends AbstractWriter {

  private final String enumName;
  private boolean isFirstElement = true;

  public EnumAPIWriter(final File generationBaseDirectory, final String packageName, final String enumName) {
    super(generationBaseDirectory, packageName, enumName);
    this.enumName = enumName;
  }

  public void writeStart() throws IOException {
    createFile();
    write(HEADER_TEXT);
    write(NEWLINE + "public enum " + enumName + " {");
    isFirstElement = true;
  }

  public void writeLiteral(final CsdlEnumMember literal) throws IOException {
    if (!isFirstElement) {
      write(", ");
    } else {
      write(NEWLINE + "\t");
    }
    write(literal.getName());
    isFirstElement = false;
  }

  public void writeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
