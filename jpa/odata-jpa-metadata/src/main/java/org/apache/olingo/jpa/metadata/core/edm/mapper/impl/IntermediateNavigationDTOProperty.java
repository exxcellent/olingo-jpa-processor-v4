package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Implements navigation to another DTO.
 *
 * @author Ralf Zozmann
 *
 */
class IntermediateNavigationDTOProperty extends IntermediateModelElement implements JPAAssociationAttribute {

  private CsdlNavigationProperty edmNaviProperty;
  private JPAStructuredType targetType;
  private final IntermediateServiceDocument serviceDocument;
  private final JPAAttributeAccessor accessor;
  private final Field field;
  private InitializationState initStateEdm = InitializationState.NotInitialized;

  IntermediateNavigationDTOProperty(final JPAEdmNameBuilder nameBuilder, final Field field,
      final IntermediateServiceDocument serviceDocument) {
    super(nameBuilder, field.getName());
    this.serviceDocument = serviceDocument;
    this.field = field;
    this.setExternalName(nameBuilder.buildPropertyName(field.getName()));
    accessor = new FieldAttributeAccessor(field);

    // do not wait with setting this important property
    setIgnore(field.isAnnotationPresent(EdmIgnore.class));
  }

  @Override
  public JPAAttributeAccessor getAttributeAccessor() {
    return accessor;
  }

  public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
    return field.getAnnotation(annotationClass);
  }

  @Override
  public CsdlNavigationProperty getProperty() throws ODataJPAModelException {
    return getEdmItem();
  }

  @Override
  public JPAStructuredType getStructuredType() {
    try {
      return getTargetEntity();
    } catch (final ODataJPAModelException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public JPAStructuredType getTargetEntity() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return targetType;
  }

  public Class<?> getType() {
    if (isCollection()) {
      try {
        return TypeMapping.extractElementTypeOfCollection(field);
      } catch (final ODataJPAModelException e) {
        throw new RuntimeException(e);
      }
    } else {
      return field.getType();
    }
  }

  @Override
  public boolean isAssociation() {
    return true;
  }

  @Override
  public boolean isCollection() {
    if (Collection.class.isAssignableFrom(field.getType())) {
      return true;
    }
    return false;
  }

  @Override
  public boolean isJoinCollection() {
    return isCollection();
  }

  @Override
  public boolean isComplex() {
    // navigation properties are targeting always a non primitive object
    return true;
  }

  @Override
  public AttributeMapping getAttributeMapping() {
    return AttributeMapping.RELATIONSHIP;
  }

  public boolean isPrimitive() {
    // navigation properties are targeting always a non primitive object
    return false;
  }

  @Override
  public boolean isKey() {
    return field.getAnnotation(javax.persistence.Id.class) != null;
  }

  @Override
  public boolean isSearchable() {
    return false;
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    switch (initStateEdm) {
    case Initialized:
      return;
    case InProgress:
      throw new IllegalStateException("Initialization already in progress, circular dependency problem!");
    default:
      break;
    }

    if (edmNaviProperty == null) {
      try {
        initStateEdm = InitializationState.InProgress;

        if (!TypeMapping.isFieldTargetingDTO(field)) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
              "Java type not supported");
        }
        final Class<?> attributeType = getType();
        targetType = serviceDocument.getEntityType(attributeType);
        if (targetType == null) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
              attributeType.getName() + " is not registered as Entity/DTO");
        }
        edmNaviProperty = new CsdlNavigationProperty();
        edmNaviProperty.setName(getExternalName());
        edmNaviProperty.setType(getNameBuilder().buildFQN(targetType.getExternalName()));
        edmNaviProperty.setCollection(isCollection());
        edmNaviProperty.setNullable(Boolean.TRUE);
      } finally {
        initStateEdm = InitializationState.Initialized;
      }
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  CsdlNavigationProperty getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmNaviProperty;
  }

}
