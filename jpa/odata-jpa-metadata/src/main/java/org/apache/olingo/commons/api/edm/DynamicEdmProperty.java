package org.apache.olingo.commons.api.edm;

import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.edm.geo.SRID;

public class DynamicEdmProperty implements EdmProperty {

  private final String name;
  private final EdmType type;
  private final boolean isCollection;
  
  public DynamicEdmProperty(String name, EdmType type, boolean isCollection) {
    this.name = name;
    this.type = type;
    this.isCollection = isCollection;
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
  public EdmAnnotation getAnnotation(EdmTerm term, String qualifier) {
    return null;
  }

  @Override
  public List<EdmAnnotation> getAnnotations() {
    return Collections.emptyList();
  }

  @Override
  public String getMimeType() {
    return null;
  }

  @Override
  public boolean isPrimitive() {
    return type.getKind() == EdmTypeKind.PRIMITIVE;
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public Integer getMaxLength() {
    return null;
  }

  @Override
  public Integer getPrecision() {
    return null;
  }

  @Override
  public Integer getScale() {
    return null;
  }

  @Override
  public String getScaleAsString() {
    return null;
  }

  @Override
  public SRID getSrid() {
    return null;
  }

  @Override
  public boolean isUnicode() {
    return false;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public EdmType getTypeWithAnnotations() {
    return type;
  }

}
