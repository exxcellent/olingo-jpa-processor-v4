package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class URIBuilderWriter extends AbstractWriter {

  private static class KeySorter implements Comparator<JPASimpleAttribute> {
    @Override
    public int compare(final JPASimpleAttribute o1, final JPASimpleAttribute o2) {
      return o1.getInternalName().compareTo(o2.getInternalName());
    }
  }

  private final static String MEMBERNAME_SINGLEENTITYCALL = "representSingleEntityCall";
  final static String METHODNAME_SINGLEENTITYCALL = "isSingleEntityCall";

  private final AbstractJPASchema schema;
  private final JPAStructuredType type;

  public URIBuilderWriter(final File generationBaseDirectory, final AbstractJPASchema schema,
      final JPAStructuredType et) {
    super(generationBaseDirectory, et.getTypeClass().getPackage().getName(), determineAccessBuilderName(et));
    this.schema = schema;
    this.type = et;
  }

  public void writeProtocolCodeStart() throws IOException {
    createFile();
    final String className = determineAccessBuilderName(type);
    write(HEADER_TEXT);
    write(NEWLINE + "public final class " + className /* + " implements " + typeMetaName */ + " {");

    generateGlobalVariables();
    generateConstructor();
    generateRepresentsEntityCallMethod();
  }

  static String determineAccessBuilderName(final JPAStructuredType type) {
    return type.getTypeClass().getSimpleName() + "URIBuilder";
  }

  public void writeProtocolCode() throws IOException, ODataJPAModelException {
    generateSpecificAppendKeysMethod();
    generateBuilderMethods();
    generateBuildMethod();
  }

  private void generateGlobalVariables() throws IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "private " + URIBuilder.class.getName() + " builder = null;");
    write(NEWLINE + "\t" + "private boolean " + MEMBERNAME_SINGLEENTITYCALL + " = false;");
  }

  private void generateConstructor() throws IOException {
    final String className = determineAccessBuilderName(type);
    final String accessClassName = HandlerAPIWriter.determineHandlerName(type);
    final String esName;
    if (JPAEntityType.class.isInstance(type)) {
      esName = JPAEntityType.class.cast(type).getEntitySetName();
    } else {
      esName = schema.getNameBuilder().buildEntitySetName(type.getExternalName());
    }
    write(NEWLINE);
    write(NEWLINE + "\t" + "protected " + className + "(" + accessClassName + " access" + ") {");
    write(NEWLINE + "\t" + "\t"
        + "this.builder = access.getClientInstance().newURIBuilder(access.getServiceRootUrl().toString()).appendEntitySetSegment(\""
        + esName + "\");");
    write(NEWLINE + "\t" + "}");

  }

  private void generateRepresentsEntityCallMethod() throws IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "boolean " + METHODNAME_SINGLEENTITYCALL + "()" + " {");
    write(NEWLINE + "\t" + "\t" + "return " + MEMBERNAME_SINGLEENTITYCALL + ";");
    write(NEWLINE + "\t" + "}");
  }

  private void generateBuildMethod() throws IOException {
    write(NEWLINE);
    write(NEWLINE + "\t" + "public " + URI.class.getCanonicalName() + " build()" + " {");
    write(NEWLINE + "\t" + "\t" + "return builder.build();");
    write(NEWLINE + "\t" + "}");
  }

  private void generateSpecificAppendKeysMethod() throws IOException, ODataJPAModelException {
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
    write(NEWLINE);
    write(NEWLINE + "\t" + "public " + determineAccessBuilderName(type) + " appendKeySegment(" + bufferKeyParameters
        .toString() + ") throws "
        + ODataException.class.getName() + " {");
    if (keys.size() > 1) {
      // multiple keys
      write(NEWLINE + "\t" + "\t" + Map.class.getName() + "<String, Object> keys = new " + HashMap.class.getName()
          + "<>();");
      final String typeMetaName = TypeMetaAPIWriter.determineTypeMetaName(type.getTypeClass().getSimpleName());
      for (final JPASimpleAttribute attribute : keys) {
        final String memberName = attribute.getInternalName();
        write(NEWLINE + "\t" + "\t" + "keys.put(" + typeMetaName + "." + TypeMetaAPIWriter
            .determineTypeMetaPropertyNameConstantName(attribute
                .getProperty()) + ", " + memberName + ");");
      }
      write(NEWLINE + "\t" + "\t" + "builder = builder.appendKeySegment(keys)" + ";");
    } else {
      // single key
      write(NEWLINE + "\t" + "\t" + "builder = builder.appendKeySegment(" + keys.get(0).getInternalName() + ")" + ";");
    }
    write(NEWLINE + "\t" + "\t" + MEMBERNAME_SINGLEENTITYCALL + " = true;");
    write(NEWLINE + "\t" + "\t" + "return this;");
    write(NEWLINE + "\t" + "}");
  }

  private void generateBuilderMethods() throws IOException, ODataJPAModelException {
    final String className = determineAccessBuilderName(type);
    for (final Method m : URIBuilder.class.getDeclaredMethods()) {
      if (isSuppressedMethod(m)) {
        continue;
      }

      final StringBuilder javadocParams = new StringBuilder();
      final StringBuilder delegatorParams = new StringBuilder();
      final StringBuilder methodParams = new StringBuilder();
      final StringBuilder genericParamTypes = new StringBuilder();
      boolean firstParam = true;
      for (final Parameter p : m.getParameters()) {
        if (!firstParam) {
          methodParams.append(", ");
          delegatorParams.append(", ");
          javadocParams.append(", ");
        }
        javadocParams.append(p.getType().getTypeName());

        final ParameterizedType pType = (p.getParameterizedType() instanceof ParameterizedType) ? (ParameterizedType) p
            .getParameterizedType() : null;
            if (pType != null && pType.getActualTypeArguments() != null &&
                pType.getActualTypeArguments().length > 0) {
              genericParamTypes.append("<");
              boolean first = true;
              for (final Type t : pType.getActualTypeArguments()) {
                if (!first) {
                  genericParamTypes.append(", ");
                }
                genericParamTypes.append(t.getTypeName());
                first = false;
              }
              genericParamTypes.append(">");
            }
            final String tName;
            if (p.isVarArgs()) {
              tName = p.getType().getTypeName().replaceFirst("\\[\\]$", "...") + genericParamTypes.toString();
            } else {
              tName = p.getType().getTypeName() + genericParamTypes.toString();
            }

            final String pName = p.isNamePresent() ? p.getName() : (m.getParameterCount() == 1 ? m.getName() + "Parameter"
                : p.getName());
            methodParams.append(tName + " " + pName);
            delegatorParams.append(pName);
            firstParam = false;
      }
      write(NEWLINE);
      write(NEWLINE + "\t" + "/**");
      write(NEWLINE + "\t" + " * @see " + URIBuilder.class.getCanonicalName() + "#" + m.getName() + "(" + javadocParams
          .toString() + ")");
      write(NEWLINE + "\t" + "*/");

      write(NEWLINE + "\t" + "public " + className + " " + m.getName() + "(" + methodParams.toString() + ")" + " {");
      write(NEWLINE + "\t" + "\t" + "builder = builder." + m.getName() + "(" + delegatorParams.toString() + ")" + ";");
      write(NEWLINE + "\t" + "\t" + "return this;");
      write(NEWLINE + "\t" + "}");

    }
  }

  private boolean isSuppressedMethod(final Method m) {
    if (m.getName().equals("appendEntitySetSegment")) {
      return true;
    }
    if (m.getName().equals("appendKeySegment")) {
      return true;
    }
    if (m.getName().equals("appendMetadataSegment")) {
      return true;
    }
    if (m.getName().equals("build")) {
      return true;
    }
    return false;
  }

  public void writeProtocolCodeEnd() throws IOException {
    write(NEWLINE + "}");
    closeFile();
  }
}
