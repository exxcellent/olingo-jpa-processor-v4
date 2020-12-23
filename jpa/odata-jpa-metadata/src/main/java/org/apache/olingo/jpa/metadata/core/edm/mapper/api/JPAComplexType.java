package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

public interface JPAComplexType extends JPAStructuredType {
  /**
   * Identifier of annotation term, the qualifier will contain the type.<br/>
   * Annotation value will be one of the simple types as defined in
   * {@link org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind}
   */
  final public static String OPEN_TYPE_ANNOTATION_NAME_VALUE_TYPE = "openType.ValueType";

  /**
   * Identifier of annotation term, the qualifier will contain the flag.<br/>
   * Annotation value will be <i>true</i> or <i>false</i>.
   */
  final public static String OPEN_TYPE_ANNOTATION_NAME_VALUE_COLLECTION_FLAG = "openType.ValueCollectionFlag";
}
