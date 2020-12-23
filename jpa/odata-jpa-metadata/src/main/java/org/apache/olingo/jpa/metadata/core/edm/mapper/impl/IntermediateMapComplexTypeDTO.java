package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.metamodel.PluralAttribute.CollectionType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPADynamicPropertyContainer;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAParameterizedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class IntermediateMapComplexTypeDTO extends AbstractIntermediateComplexTypeDTO implements JPADynamicPropertyContainer {
  final private EdmPrimitiveTypeKind mapValueKind;
  final private DynamicJPAParameterizedElement dynamicProperty;
  private CsdlComplexType edmComplexType;

  public IntermediateMapComplexTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName,
      final Class<?> mapKeyType, final Class<?> mapValueType, final boolean valueIsCollection)
          throws ODataJPAModelException {
    super(nameBuilder, typeName, true, true);
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
  protected void buildPropertyList() throws ODataJPAModelException {
    // no properties
  }

  @Override
  protected JPAStructuredType getBaseType() throws ODataJPAModelException {
    return null;
  }

  @Override
  CsdlComplexType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmComplexType;
  }

  @Override
  final protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (edmComplexType == null) {
      edmComplexType = new CsdlComplexType();

      edmComplexType.setName(this.getExternalName());
      edmComplexType.setProperties(getAttributes(true).stream().map(attribute -> attribute.getProperty()).collect(
          Collectors
          .toList()));
      edmComplexType.setNavigationProperties(getAssociations().stream().map(association -> association.getProperty())
          .collect(Collectors.toList()));
      edmComplexType.setBaseType(determineBaseType());
      edmComplexType.setAbstract(isAbstract());
      edmComplexType.setOpenType(isOpenType());

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
      edmComplexType.setAnnotations(Arrays.asList(annotationType, annotationCollectionMarker));

      if (determineHasStream()) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_STREAM,
            getInternalName());
      }
    }
  }

  @Override
  public JPAParameterizedElement getDynamicPropertyDescription() {
    return dynamicProperty;
  }

}