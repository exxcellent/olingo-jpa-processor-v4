package org.apache.olingo.server.core;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmAnnotation;
import org.apache.olingo.commons.api.edm.EdmMapping;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;

public class DynamicEdmProperty implements EdmProperty {

  final private EdmType type;
  final private boolean isCollection;
  final private String name;

  public DynamicEdmProperty(final String name, final EdmPrimitiveTypeKind kind, final boolean isCollection) {
    super();
    type = EdmPrimitiveTypeFactory.getInstance(kind);
    this.isCollection = isCollection;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public EdmType getType() {
    return type;
  }

  @Override
  public boolean isCollection() {
    return isCollection;
  }

  @Override
  public EdmMapping getMapping() {
    return null;
  }

  @Override
  public EdmAnnotation getAnnotation(final EdmTerm term, final String qualifier) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<EdmAnnotation> getAnnotations() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getMimeType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isPrimitive() {
    // per definition
    return true;
  }

  @Override
  public boolean isNullable() {
    return true;
  }

  @Override
  public Integer getMaxLength() {
    // no value available
    return null;
  }

  @Override
  public Integer getPrecision() {
    // no value available
    return null;
  }

  @Override
  public Integer getScale() {
    // no value available
    return null;
  }

  @Override
  public SRID getSrid() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isUnicode() {
    return true;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public EdmType getTypeWithAnnotations() {
    throw new UnsupportedOperationException();
  }

}
