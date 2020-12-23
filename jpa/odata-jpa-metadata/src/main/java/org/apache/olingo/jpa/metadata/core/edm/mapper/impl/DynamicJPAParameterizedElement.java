package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;

import javax.persistence.metamodel.PluralAttribute.CollectionType;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAParameterizedElement;

class DynamicJPAParameterizedElement extends DynamicJPADescribedElement implements JPAParameterizedElement {

  private boolean isCollection = false;
  private boolean isNullable = true;
  private CollectionType collectionType = null;

  @Override
  public CollectionType getCollectionType() {
    return collectionType;
  }

  @Override
  public Integer getMaxLength() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Integer getPrecision() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Integer getScale() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNullable() {
    return isNullable;
  }

  @Override
  public boolean isCollection() {
    return isCollection;
  }

  DynamicJPAParameterizedElement setCollection(final boolean isCollection) {
    this.isCollection = isCollection;
    return this;
  }

  DynamicJPAParameterizedElement setNullable(final boolean isNullable) {
    this.isNullable = isNullable;
    return this;
  }

  DynamicJPAParameterizedElement setCollectionType(final CollectionType collectionType) {
    this.collectionType = collectionType;
    return this;
  }

  @Override
  public DynamicJPAParameterizedElement setType(final Class<?> type) {
    super.setType(type);
    return this;
  }

  @Override
  public DynamicJPAParameterizedElement setAnnotatedElement(final AnnotatedElement annotatedElement) {
    super.setAnnotatedElement(annotatedElement);
    return this;
  }
}
