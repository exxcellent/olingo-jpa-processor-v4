package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class IntermediateModelElement<CDSLType extends CsdlAbstractEdmItem> implements JPAElement {

  protected static enum InitializationState {
    NotInitialized, InProgress, Initialized;
  }

  private final JPAEdmNameBuilder nameBuilder;
  private final String internalName;

  private boolean toBeIgnored = false;
  private String externalName;

  public IntermediateModelElement(final JPAEdmNameBuilder nameBuilder, final String internalName) {
    super();
    this.nameBuilder = nameBuilder;
    this.internalName = internalName;
  }

  protected JPAEdmNameBuilder getNameBuilder() {
    return nameBuilder;
  }

  @Override
  public String getExternalName() {
    return externalName;
  }

  @Override
  public FullQualifiedName getExternalFQN() {
    return nameBuilder.buildFQN(getExternalName());
  }

  @Override
  public String getInternalName() {
    return internalName;
  }

  public boolean ignore() {
    return toBeIgnored;
  }

  public void setExternalName(final String externalName) {
    this.externalName = externalName;
  }

  public void setIgnore(final boolean ignore) {
    this.toBeIgnored = ignore;
  }

  protected abstract void lazyBuildEdmItem() throws ODataJPAModelException;

  /**
   * @deprecated User direct transformations
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  protected static <T> List<?> extractEdmModelElements(
      final Map<String, ? extends IntermediateModelElement<?>> mappingBuffer) throws ODataJPAModelException {
    final List<T> extractionTarget = new ArrayList<T>(mappingBuffer.size());
    for (final String externalName : mappingBuffer.keySet()) {
      if (!((IntermediateModelElement<?>) mappingBuffer.get(externalName)).toBeIgnored) {
        final IntermediateModelElement<?> element = mappingBuffer.get(externalName);
        final CsdlAbstractEdmItem edmElement = element.getEdmItem();
        if (!element.ignore()) {
          extractionTarget.add((T) edmElement);
        }
      }
    }
    return extractionTarget;
    // return returnNullIfEmpty(extractionTarget);
  }

  protected static <T> List<T> returnNullIfEmpty(final List<T> list) {
    return list == null || list.isEmpty() ? null : list;
  }

  ValueType determineValueType(final IntermediateServiceDocument isd, final Type type, final boolean isCollection) {
    final Class<?> clazzType = TypeMapping.determineClassType(type);
    if (clazzType.isEnum()) {
      return (isCollection ? ValueType.COLLECTION_ENUM : ValueType.ENUM);
    }
    final JPAEntityType entityType= isd.getEntityType(clazzType);
    if(entityType != null) {
      return (isCollection?ValueType.COLLECTION_ENTITY:ValueType.ENTITY);
    }
    final JPAStructuredType complexType = isd.getComplexType(clazzType);
    if(complexType != null) {
      return (isCollection?ValueType.COLLECTION_COMPLEX:ValueType.COMPLEX);
    }
    // at least GEOSPATIAL or PRMITIVE
    try {
      final EdmPrimitiveTypeKind pT =TypeMapping.convertToEdmSimpleType(clazzType);
      if (pT.isGeospatial()) {
        return (isCollection ? ValueType.COLLECTION_GEOSPATIAL : ValueType.GEOSPATIAL);
      }
      return (isCollection ? ValueType.COLLECTION_PRIMITIVE : ValueType.PRIMITIVE);
    } catch (final ODataJPAModelException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @see #findOrCreateType(IntermediateServiceDocument, Field)
   * @see #findOrCreateType(IntermediateServiceDocument, Parameter)
   */
  static FullQualifiedName findOrCreateType(final IntermediateServiceDocument serviceDocument, final Type type)
      throws ODataJPAModelException {
    return findOrCreateType(serviceDocument, null, null, type);
  }

  /**
   * Find or create the type identified by the {@link Field#getType() field type}. Convenience to extract the type from
   * a {@link ParameterizedType} in case of an collection field.
   */
  static FullQualifiedName findOrCreateType(final IntermediateServiceDocument serviceDocument, final Field field)
      throws ODataJPAModelException {
    return findOrCreateType(serviceDocument, field, field.getName(), field.getGenericType());
  }

  static FullQualifiedName findOrCreateType(final IntermediateServiceDocument serviceDocument,
      final Parameter parameter) throws ODataJPAModelException {
    return findOrCreateType(serviceDocument, parameter, parameter.getName(), parameter.getParameterizedType());
  }

  /**
   * a) Detect source of requested type: JPA (entity, embeddable), DTO (entity, complex type), enum, primitive type<br/>
   * b) Create type on demand or reuse existing type declaration...
   *
   * @param typeOwner Optional argument
   * @param typeOwnerName Optional argument
   */
  private static FullQualifiedName findOrCreateType(final IntermediateServiceDocument serviceDocument,
      final AnnotatedElement typeOwner, final String typeOwnerName, final Type type)
          throws ODataJPAModelException {
    final Type collectionUnwrappedType = TypeMapping.unwrapCollection(type);
    final Class<?> typeClass = TypeMapping.determineClassType(collectionUnwrappedType);
    if (Map.class.isAssignableFrom(typeClass)) {
      // map as dynamic type?
      final Triple<Class<?>, Class<?>, Boolean> typeInfo = checkMapTypeArgumentsMustBeSimple(type, typeOwnerName);
      final IntermediateMapComplexTypeDTO jpaMapType = serviceDocument.createDynamicJavaUtilMapType(typeInfo
          .getLeft(), typeInfo.getMiddle(), typeInfo.getRight().booleanValue());
      jpaMapType.setAnnotatedElement(typeOwner);
      return jpaMapType.getExternalFQN();
    } else if (typeClass.getAnnotation(ODataDTO.class) != null || isTargetingJPA(serviceDocument, typeClass)) {
      // is targeting a class to use as (already registered) @ODataDTO or as JPA element (entity, @Embeddable and maybe
      // including @ODataComplexType)
      final JPAStructuredType predefinedType = serviceDocument.getStructuredType(typeClass);
      if (predefinedType == null) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.RUNTIME_PROBLEM,
            typeClass.getName() + " is not registered as Entity/DTO");
      }
      return predefinedType.getExternalFQN();
    } else if (typeClass.isEnum()) {
      @SuppressWarnings("unchecked")
      final IntermediateEnumType jpaEnumType = serviceDocument.findOrCreateEnumType(
          (Class<? extends Enum<?>>) typeClass);
      return jpaEnumType.getExternalFQN();
    } else if (typeClass.getAnnotation(ODataComplexType.class) != null) {
      // is targeting a class to use as @ODataComplexType and is here no JPA metamodel element
      final AbstractIntermediateComplexTypeDTO ct = serviceDocument.findOrCreateDTOComplexType(typeClass);
      return ct.getExternalFQN();
    }
    // assume primitive, trigger exception if not mappable
    else if (typeOwner instanceof Field) {
      return TypeMapping.convertToEdmSimpleType((Field) typeOwner).getFullQualifiedName();
    } else {
      return TypeMapping.convertToEdmSimpleType(typeClass).getFullQualifiedName();
    }
  }

  /**
   *
   * @return TRUE if given class is an JPA entity or an JPA embeddable.
   */
  private static boolean isTargetingJPA(final IntermediateServiceDocument serviceDocument, final Class<?> typeClass) {
    final IntermediateMetamodelSchema schema = (IntermediateMetamodelSchema) serviceDocument.getJPASchemas().stream()
        .filter(s -> (s instanceof IntermediateMetamodelSchema)).findFirst().get();
    return schema.getStructuredType(typeClass) != null;
  }

  static Triple<Class<?>, Class<?>, Boolean> checkMapTypeArgumentsMustBeSimple(final Type theType,
      final String elementNameForErrorMessages)
          throws ODataJPAModelException {
    if (!ParameterizedType.class.isInstance(theType)) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE, theType
          .getTypeName() + " [must be generic]", elementNameForErrorMessages);
    }
    final java.lang.reflect.Type[] typeArguments = ParameterizedType.class.cast(theType).getActualTypeArguments();
    if (typeArguments.length != 2) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
          elementNameForErrorMessages,
          "Map<x,y>, having two type arguments expected");
    }
    final Type keyType = extractTypeOfGenericType(typeArguments[0]);
    final Type valueType = extractTypeOfGenericType(typeArguments[1]);
    final boolean isCollection = Class.class.isInstance(typeArguments[1]) && Collection.class.isAssignableFrom(
        Class.class.cast(typeArguments[1])) || ParameterizedType.class.isInstance(typeArguments[1]) && Collection.class
        .isAssignableFrom((Class<?>) ParameterizedType.class.cast(typeArguments[1]).getRawType());
    return new Triple<Class<?>, Class<?>, Boolean>(Class.class.cast(keyType), Class.class.cast(valueType), Boolean
        .valueOf(isCollection));
  }

  private static Class<?> extractTypeOfGenericType(final Type type) throws ODataJPAModelException {
    if (ParameterizedType.class.isInstance(type)) {
      final java.lang.reflect.Type[] types = ((ParameterizedType) type)
          .getActualTypeArguments();
      if (types.length == 1) {
        return (Class<?>) types[0];
      } else {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER,
            "Only one type parameter acceptable");
      }
    }
    return (Class<?>) type;
  }

  /**
   * @deprecated Use more specialized methods...
   */
  @Deprecated
  abstract CDSLType getEdmItem() throws ODataRuntimeException;
}
