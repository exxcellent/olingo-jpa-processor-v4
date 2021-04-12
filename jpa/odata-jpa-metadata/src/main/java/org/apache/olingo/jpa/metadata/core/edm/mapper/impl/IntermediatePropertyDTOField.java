package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.PluralAttribute.CollectionType;
import javax.validation.constraints.Size;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * A DTO is mapped as OData entity!
 *
 * @author Ralf Zozmann
 *
 */
class IntermediatePropertyDTOField extends AbstractProperty<CsdlProperty> implements JPAMemberAttribute {

  private final IntermediateServiceDocument serviceDocument;
  private final Field field;
  private final JPAAttributeAccessor accessor;
  private CsdlProperty edmProperty = null;
  private FullQualifiedName propertyTypeName = null;

  public IntermediatePropertyDTOField(final JPAEdmNameBuilder nameBuilder, final Field field,
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

  /**
   *
   * @return TRUE if field has the {@link javax.persistence.Id @Id} annotation.
   */
  @Override
  public boolean isKey() {
    return field.getAnnotation(javax.persistence.Id.class) != null;
  }

  private FullQualifiedName initializePropertyType() throws ODataJPAModelException {
    if (propertyTypeName != null) {
      return propertyTypeName;
    }
    final Class<?> attributeType = getType();
    if (TypeMapping.isTargetingDTO(field)) {
      final JPAStructuredType dtoType = serviceDocument.getStructuredType(attributeType);
      if (dtoType == null) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
            attributeType.getName() + " is not registered as Entity/DTO");
      }
      propertyTypeName = dtoType.getExternalFQN();
    } else if (Map.class.isAssignableFrom(field.getType())) {
      // special handling for java.util.Map
      final Triple<Class<?>, Class<?>, Boolean> typeInfo = checkMapTypeArgumentsMustBeSimple(field.getGenericType(),
          field.getName());
      final AbstractIntermediateComplexTypeDTO jpaMapType = serviceDocument.createDynamicJavaUtilMapType(typeInfo
          .getLeft(), typeInfo.getMiddle(), typeInfo.getRight().booleanValue());
      propertyTypeName = jpaMapType.getExternalFQN();
    } else if (attributeType.isEnum()) {
      @SuppressWarnings("unchecked")
      final IntermediateEnumType jpaEnumType = serviceDocument.findOrCreateEnumType(
          (Class<? extends Enum<?>>) attributeType);
      propertyTypeName = jpaEnumType.getExternalFQN();
    } else if (isTargetingComplexType(field)) {
      final AbstractIntermediateComplexTypeDTO ct = serviceDocument.findOrCreateDTOComplexType(attributeType);
      propertyTypeName = ct.getExternalFQN();
    } else {
      // assume primitive
      // trigger exception if not possible
      propertyTypeName = TypeMapping.convertToEdmSimpleType(field).getFullQualifiedName();
    }
    return propertyTypeName;
  }

  static boolean isTargetingComplexType(final Field field) throws ODataJPAModelException {
    final Class<?> javaType;
    if (Collection.class.isAssignableFrom(field.getType())) {
      javaType = TypeMapping.extractElementTypeOfCollection(field);
    } else {
      javaType = field.getType();
    }
    if (javaType == null) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
          "Java type not available");
    }
    return javaType.getAnnotation(ODataComplexType.class) != null;
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmProperty != null) {
      return;
    }
    edmProperty = new CsdlProperty();
    edmProperty.setName(this.getExternalName());

    edmProperty.setType(initializePropertyType());// trigger exception for unsupported attribute types
    edmProperty.setCollection(Collection.class.isAssignableFrom(field.getType()));

    Integer maxLength = null;
    final Size annotationSize = field.getAnnotation(Size.class);
    if (annotationSize != null) {
      maxLength = Integer.valueOf(annotationSize.max());
    }

    edmProperty.setNullable(true);
    edmProperty.setSrid(IntermediateProperty.getSRID(field));
    // edmProperty.setDefaultValue(determineDefaultValue());
    if (edmProperty.getTypeAsFQNObject().equals(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        || edmProperty.getTypeAsFQNObject().equals(EdmPrimitiveTypeKind.Binary.getFullQualifiedName())) {
      edmProperty.setMaxLength(maxLength);
    }
  }

  @Override
  CsdlProperty getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmProperty;
  }

  @Override
  public JPAStructuredType getStructuredType() {
    try {
      return serviceDocument.getStructuredType(initializePropertyType());
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
  }

  @Override
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
  public CollectionType getCollectionType() {
    if (Set.class.isAssignableFrom(field.getType())) {
      return CollectionType.SET;
    } else if (List.class.isAssignableFrom(field.getType())) {
      return CollectionType.LIST;
    } else if (Collection.class.isAssignableFrom(field.getType())) {
      return CollectionType.COLLECTION;
    } else if (Map.class.isAssignableFrom(field.getType())) {
      return CollectionType.MAP;
    }
    return null;
  }

  @Override
  public boolean isComplex() {
    try {
      if (isTargetingComplexType(field)) {
        return true;
      }
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
    // otherwise only the map is handled as complex
    return (CollectionType.MAP == getCollectionType());
  }

  @Override
  public AttributeMapping getAttributeMapping() {
    if (isComplex()) {
      return AttributeMapping.AS_COMPLEX_TYPE;
    }
    return AttributeMapping.SIMPLE;
  }

  @Override
  public boolean isAssociation() {
    return false;
  }

  @Override
  public boolean isSearchable() {
    // never searchable
    return false;
  }

  @Override
  public Integer getMaxLength() {
    return getProperty().getMaxLength();
  }

  @Override
  public Integer getPrecision() {
    return getProperty().getPrecision();
  }

  @Override
  public Integer getScale() {
    return getProperty().getScale();
  }

  @Override
  public boolean isNullable() {
    return getProperty().isNullable();
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
  public boolean isSimple() {
    return !isComplex();
  }

  @Override
  public CsdlProperty getProperty() throws ODataRuntimeException {
    return getEdmItem();
  }

  @Override
  public String getDBFieldName() {
    return null;
  }

  @Override
  public AnnotatedElement getAnnotatedElement() {
    return field;
  }

  @Override
  boolean isStream() {
    return false;
  }

  @Override
  public boolean isEtag() {
    return false;
  }
}
