package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADescribedElement;

public class DynamicJPADescribedElement implements JPADescribedElement {

  private Class<?> type = null;
  private AnnotatedElement annotatedElement = null;

  @Override
  public Class<?> getType() {
    return type;
  }

  @Override
  public AnnotatedElement getAnnotatedElement() {
    return annotatedElement;
  }

  public DynamicJPADescribedElement setType(final Class<?> type) {
    this.type = type;
    return this;
  }

  public DynamicJPADescribedElement setAnnotatedElement(final AnnotatedElement annotatedElement) {
    this.annotatedElement = annotatedElement;
    return this;
  }

}
