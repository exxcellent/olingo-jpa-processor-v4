package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.metamodel.PluralAttribute.CollectionType;
import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionResult;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class ActionResultParameter implements JPAOperationResultParameter {

  private final IntermediateAction owner;

  private CsdlReturnType returnType = null;
  private final Integer precision;
  private final Integer scale;

  public ActionResultParameter(final IntermediateAction owner) {
    this.owner = owner;
    final EdmActionResult edmResultAnnotation = owner.getJavaMethod().getAnnotation(EdmActionResult.class);
    if (edmResultAnnotation != null) {
      scale = edmResultAnnotation.scale() > -1 ? Integer.valueOf(edmResultAnnotation.scale()) : null;
      precision = edmResultAnnotation.precision() > -1 ? Integer.valueOf(edmResultAnnotation.precision()) : null;

    } else {
      precision = null;
      scale = null;
    }
  }

  @Override
  public AnnotatedElement getAnnotatedElement() {
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
    return precision;
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
    return scale;
  }

  @Override
  public boolean isCollection() {
    return Collection.class.isAssignableFrom(owner.getJavaMethod().getReturnType());
  }

  @Override
  public ValueType getResultValueType() {
    return owner.determineValueType(owner.getIntermediateServiceDocument(), owner.getJavaMethod()
        .getGenericReturnType(), isCollection());
  }

  CsdlReturnType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return returnType;
  }

  private void lazyBuildEdmItem() throws ODataJPAModelException {
    if (returnType != null) {
      return;
    }
    final Method javaMethod = owner.getJavaMethod();
    final FullQualifiedName fqn = IntermediateModelElement.findOrCreateType(owner.getIntermediateServiceDocument(),
        javaMethod.getGenericReturnType());

    returnType = new CsdlReturnType();
    returnType.setType(fqn);
    returnType.setCollection(isCollection());
    returnType.setNullable(!owner.getJavaMethod().isAnnotationPresent(NotNull.class));
    returnType.setPrecision(precision);
    returnType.setScale(scale);
  }

}