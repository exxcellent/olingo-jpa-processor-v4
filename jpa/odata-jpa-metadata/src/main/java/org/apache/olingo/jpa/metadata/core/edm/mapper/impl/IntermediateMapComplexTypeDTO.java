package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.persistence.metamodel.PluralAttribute.CollectionType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADynamicPropertyContainer;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAParameterizedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class IntermediateMapComplexTypeDTO extends AbstractIntermediateComplexTypeDTO implements JPADynamicPropertyContainer {
  final private EdmPrimitiveTypeKind mapValueKind;
  final private DynamicJPAParameterizedElement dynamicProperty;

  public IntermediateMapComplexTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName,
      final Class<?> mapKeyType, final Class<?> mapValueType, final boolean valueIsCollection,
      final IntermediateServiceDocument serviceDocument)
          throws ODataJPAModelException {
    super(nameBuilder, typeName, true, true, serviceDocument);
    this.setExternalName(typeName);
    if (!String.class.isAssignableFrom(mapKeyType)) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER,
          "Map key parameter " + mapKeyType.getTypeName() + " must be a String");
    }
    try {
      // provoke exception for not simple types
      this.mapValueKind = TypeMapping.convertToEdmSimpleType(mapValueType);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER,
          "Map value parameter " + mapValueType.getTypeName() + " must be a simple type");
    }

    dynamicProperty = new DynamicJPAParameterizedElement().setCollection(valueIsCollection).setNullable(true).setType(
        mapValueType);
    if (valueIsCollection) {
      // the currently only supported collection type
      dynamicProperty.setCollectionType(CollectionType.COLLECTION);
    }
  }

  @Override
  public Class<?> getTypeClass() {
    return Map.class;
  }

  @Override
  protected JPAStructuredType getBaseType() throws ODataJPAModelException {
    return null;
  }

  @Override
  protected void enrichCsdlComplexType(final CsdlComplexType justCreatedCdslType) {
    // use annotation to transport informations about value type
    // be aware: 'term' is an identifier later used to take it from the schema... but we do not store data there...
    // but we need an global unique annotation identifier for our dynamic type
    final CsdlAnnotation annotationType = new CsdlAnnotation();
    annotationType.setTerm(getExternalFQN().getFullQualifiedNameAsString() + "."
        + JPAComplexType.OPEN_TYPE_ANNOTATION_NAME_VALUE_TYPE);
    annotationType.setQualifier(mapValueKind.name());
    final CsdlAnnotation annotationCollectionMarker = new CsdlAnnotation();
    annotationCollectionMarker.setTerm(getExternalFQN().getFullQualifiedNameAsString() + "."
        + JPAComplexType.OPEN_TYPE_ANNOTATION_NAME_VALUE_COLLECTION_FLAG);
    annotationCollectionMarker.setQualifier(Boolean.toString(dynamicProperty.isCollection()));
    justCreatedCdslType.setAnnotations(Arrays.asList(annotationType, annotationCollectionMarker));
  }

  @Override
  public JPAParameterizedElement getDynamicPropertyDescription() {
    return dynamicProperty;
  }

  @Override
  protected Collection<Field> getPropertyFields() {
    // no properties
    return Collections.emptyList();
  }
}