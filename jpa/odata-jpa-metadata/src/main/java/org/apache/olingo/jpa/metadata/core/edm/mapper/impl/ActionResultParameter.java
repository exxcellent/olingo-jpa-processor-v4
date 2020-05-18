package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.PluralAttribute.CollectionType;
import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class ActionResultParameter implements JPAOperationResultParameter {

  private final IntermediateAction owner;

  private CsdlReturnType returnType = null;

  public ActionResultParameter(final IntermediateAction owner) {
    this.owner = owner;
  }

  @Override
  public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
    // currently not supported
    return null;
  }

  @Override
  public Class<?> getType() {
    if (isCollection()) {
      return (Class<?>) ((ParameterizedType) owner.getJavaMethod().getGenericReturnType()).getActualTypeArguments()[0];
    }
    return owner.getJavaMethod().getReturnType();
  }

  @Override
  public CollectionType getCollectionType() {
    if (Set.class.isAssignableFrom(owner.getJavaMethod().getReturnType())) {
      return CollectionType.SET;
    } else if (List.class.isAssignableFrom(owner.getJavaMethod().getReturnType())) {
      return CollectionType.LIST;
    } else if (Collection.class.isAssignableFrom(owner.getJavaMethod().getReturnType())) {
      return CollectionType.COLLECTION;
    }
    return null;
  }

  @Override
  public FullQualifiedName getTypeFQN() {
    try {
      lazyBuildEdmItem();
      return returnType.getTypeFQN();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getPrecision() {
    try {
      lazyBuildEdmItem();
      return returnType.getPrecision();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getMaxLength() {
    try {
      lazyBuildEdmItem();
      return returnType.getMaxLength();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isNullable() {
    try {
      lazyBuildEdmItem();
      return returnType.isNullable();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getScale() {
    try {
      lazyBuildEdmItem();
      return returnType.getScale();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isCollection() {
    return Collection.class.isAssignableFrom(owner.getJavaMethod().getReturnType());
  }

  @Override
  public boolean isPrimitive() {
    try {
      return TypeMapping.convertToEdmSimpleType(getType()) != null;
    } catch (final ODataJPAModelException e) {
      return false;
    }
  }

  CsdlReturnType getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return returnType;
  }

  private void lazyBuildEdmItem() throws ODataJPAModelException {
    if (returnType != null) {
      return;
    }
    final Method javaMethod = owner.getJavaMethod();
    final FullQualifiedName fqn = owner.extractGenericTypeQualifiedName(javaMethod.getGenericReturnType());

    returnType = new CsdlReturnType();
    returnType.setType(fqn);
    returnType.setCollection(isCollection());
    returnType.setNullable(!owner.getJavaMethod().isAnnotationPresent(NotNull.class));
    // TODO length, precision, scale
  }

}