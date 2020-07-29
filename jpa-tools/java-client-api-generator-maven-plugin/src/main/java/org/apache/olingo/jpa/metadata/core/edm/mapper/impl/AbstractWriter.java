package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractWriter implements TextConstants {

  private final File generationBaseDirectory;
  private final String packageName;
  private final String fileName;
  private FileWriter writer = null;

  /**
   *
   * @param javaBaseName The name of file without extension ('.java' will be append automatically)
   */
  public AbstractWriter(final File generationBaseDirectory, final String packageName, final String javaBaseName) {
    this.generationBaseDirectory = generationBaseDirectory;
    this.packageName = packageName;
    this.fileName = javaBaseName + ".java";
  }

  private File createPackageDirectory() {
    final File packageDir = new File(generationBaseDirectory, packageName.replace('.', '/'));
    packageDir.mkdirs();
    return packageDir;
  }

  protected void createFile() throws IOException {
    if (writer != null) {
      throw new IllegalStateException("Close already open file before");
    }
    final File baseDir = createPackageDirectory();
    final File file = new File(baseDir, fileName);
    writer = new FileWriter(file, false);
    write("package " + packageName + ";" + NEWLINE);
  }

  protected void write(final String text) throws IOException {
    if (writer == null) {
      throw new IllegalStateException("Open file before");
    }
    writer.write(text);
  }

  protected void closeFile() throws IOException {
    if (writer == null) {
      throw new IllegalStateException("Open file before");
    }
    writer.close();
    writer = null;
  }
}
