package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

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
    final Class<?> clazzType = determineClassType(type);
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

  static Class<?> determineClassType(final Type type) {
    if (Class.class.isInstance(type)) {
      // simply use the argument self without further inspection
      return (Class<?>) type;
    } else if (ParameterizedType.class.isInstance(type)) {
      final ParameterizedType pType = (ParameterizedType) type;
      if (pType.getActualTypeArguments().length == 1) {
        final Type genericType = pType.getActualTypeArguments()[0];
        if (Class.class.isInstance(genericType)) {
          return (Class<?>) genericType;
        }
      }
    }
    throw new UnsupportedOperationException(type.getTypeName());
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
  // FIXME remove checked exception ,because method is called at runtime and not at startup, so we have to avoid checked
  // exception for that late calls
  @Deprecated
  abstract CDSLType getEdmItem() throws ODataRuntimeException;
}
