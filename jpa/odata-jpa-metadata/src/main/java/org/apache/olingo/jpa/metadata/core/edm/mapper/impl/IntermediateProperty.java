package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Version;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.validation.constraints.Size;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.geo.SRID;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmGeospatial;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmMediaStream;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;

/**
 * A Property is described on the one hand by its Name and Type and on the other hand by its Property Facets. The
 * type is a qualified name of either a primitive type, a complex type or a enumeration type. Primitive types are mapped
 * by {@link TypeMapping}.
 *
 * <p>
 * For details about Property metadata see:
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397954"
 * >OData Version 4.0 Part 3 - 6 Structural Property </a>
 *
 *
 * @author Oliver Grande
 *
 */
class IntermediateProperty extends IntermediateModelElement implements IntermediatePropertyAccess, JPASimpleAttribute {

  private final static Logger LOG = Logger.getLogger(IntermediateProperty.class.getName());
  private static final String DB_FIELD_NAME_PATTERN = "\"&1\"";
  // TODO Store a type @Convert
  protected final Attribute<?, ?> jpaAttribute;
  protected final IntermediateServiceDocument serviceDocument;
  protected CsdlProperty edmProperty;
  private JPAStructuredType type = null;
  private String dbFieldName;
  private boolean searchable;
  private boolean isVersion = false;
  private EdmMediaStream streamInfo;
  private final boolean isComplex;
  private final JPAAttributeAccessor accessor;
  private final Member javaMember;

  IntermediateProperty(final JPAEdmNameBuilder nameBuilder, final Attribute<?, ?> jpaAttribute,
      final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {

    super(nameBuilder, jpaAttribute.getName());
    this.jpaAttribute = jpaAttribute;
    this.serviceDocument = serviceDocument;

    isComplex = (jpaAttribute.getPersistentAttributeType() == PersistentAttributeType.EMBEDDED)
        || TypeMapping.isCollectionTypeOfEmbeddable(jpaAttribute);
    javaMember = determineJavaMemberOfAttribute(jpaAttribute);
    accessor = new FieldAttributeAccessor((Field) javaMember);
    buildProperty(nameBuilder);
  }

  Member getJavaMember() {
    return javaMember;
  }

  @Override
  public JPAAttributeAccessor getAttributeAccessor() {
    return accessor;
  }

  @Override
  public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
    if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
      return ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(annotationClass);
    }
    return null;
  }

  @Override
  public JPAStructuredType getStructuredType() {
    return type;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<?> getType() {
    if (isCollection()) {
      return ((PluralAttribute) jpaAttribute).getElementType().getJavaType();
    }
    return jpaAttribute.getJavaType();
  }

  @Override
  public boolean isComplex() {
    return isComplex;
  }

  @Override
  public boolean isKey() {
    if (jpaAttribute instanceof SingularAttribute<?, ?>) {
      return ((SingularAttribute<?, ?>) jpaAttribute).isId();
    }
    return false;
  }

  @Override
  public AttributeMapping getAttributeMapping() {
    if (isComplex) {
      return AttributeMapping.AS_COMPLEX_TYPE;
    }
    return AttributeMapping.SIMPLE;
  }

  @Override
  public boolean isPrimitive() {
    if (isComplex()) {
      return false;
    }
    if (isCollection()) {
      return TypeMapping.isCollectionTypeOfPrimitive(jpaAttribute);
    }
    return TypeMapping.isPrimitiveType(jpaAttribute);
  }

  @Override
  public boolean isCollection() {
    return jpaAttribute.isCollection();
  }

  boolean isStream() {
    return (streamInfo != null);
  }

  private FullQualifiedName createTypeName() throws ODataJPAModelException {
    if (isStream()) {
      return EdmPrimitiveTypeKind.Stream.getFullQualifiedName();
    }
    switch (jpaAttribute.getPersistentAttributeType()) {
    case BASIC:
      if (jpaAttribute.getJavaType().isEnum()) {
        // register enum type
        @SuppressWarnings("unchecked")
        final IntermediateEnumType jpaEnumType = serviceDocument
        .findOrCreateEnumType((Class<? extends Enum<?>>) jpaAttribute.getJavaType());
        return jpaEnumType.getExternalFQN();
      } else {
        return TypeMapping.convertToEdmSimpleType(jpaAttribute.getJavaType(), jpaAttribute).getFullQualifiedName();
      }
    case EMBEDDED:
      return getNameBuilder().buildFQN(type.getExternalName());
    case ELEMENT_COLLECTION:
      final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) jpaAttribute;
      if (pa.getElementType().getJavaType().isEnum()) {
        // register enum type
        @SuppressWarnings("unchecked")
        final IntermediateEnumType jpaEnumType = serviceDocument
            .findOrCreateEnumType((Class<? extends Enum<?>>) pa.getElementType().getJavaType());
        return jpaEnumType.getExternalFQN();
      } else if (TypeMapping.isCollectionTypeOfPrimitive(jpaAttribute)) {
        return TypeMapping.convertToEdmSimpleType(pa.getElementType().getJavaType(), pa).getFullQualifiedName();
      } else if (TypeMapping.isCollectionTypeOfEmbeddable(jpaAttribute)) {
        return serviceDocument.getStructuredType(jpaAttribute).getExternalFQN();
      } else {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
            pa.getElementType().getJavaType().getName(), (javaMember != null ? javaMember.getName() : null));
      }
    default:
      // trigger exception if not possible
      return TypeMapping.convertToEdmSimpleType(jpaAttribute.getJavaType(), jpaAttribute).getFullQualifiedName();
    }
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmProperty == null) {
      edmProperty = new CsdlProperty();
      edmProperty.setName(this.getExternalName());

      edmProperty.setType(createTypeName());// trigger exception for unsupported attribute types
      edmProperty.setCollection(jpaAttribute.isCollection());

      if (javaMember instanceof AnnotatedElement) {
        final AnnotatedElement annotatedMember = (AnnotatedElement) javaMember;
        Integer maxLength = null;
        final Size annotationSize = annotatedMember.getAnnotation(Size.class);
        if (annotationSize != null) {
          maxLength = Integer.valueOf(annotationSize.max());
        }

        final Column annotationColumn = annotatedMember.getAnnotation(Column.class);
        if (annotationColumn != null) {
          if (maxLength == null && annotationColumn.length() > 0) {
            maxLength = Integer.valueOf(annotationColumn.length());
          }
          edmProperty.setNullable(annotationColumn.nullable());
          edmProperty.setSrid(getSRID(javaMember));
          edmProperty.setDefaultValue(determineDefaultValue());
          if (edmProperty.getTypeAsFQNObject().equals(EdmPrimitiveTypeKind.String.getFullQualifiedName())
              || edmProperty.getTypeAsFQNObject().equals(EdmPrimitiveTypeKind.Binary.getFullQualifiedName())) {
            edmProperty.setMaxLength(maxLength);
          } else if (edmProperty.getType().equals(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName().toString())
              || edmProperty.getType().equals(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName().toString())
              || edmProperty.getType().equals(EdmPrimitiveTypeKind.TimeOfDay.getFullQualifiedName().toString())) {
            // For a decimal property the value of this attribute specifies the maximum number of digits allowed in the
            // properties value; it MUST be a positive integer. If no value is specified, the decimal property has
            // unspecified precision.
            // For a temporal property the value of this attribute specifies the number of decimal places allowed in the
            // seconds portion of the property's value; it MUST be a non-negative integer
            // between zero and twelve. If no
            // value is specified, the temporal property has a precision of zero.
            if (annotationColumn.precision() > 0) {
              edmProperty.setPrecision(Integer.valueOf(annotationColumn.precision()));
            }
            if (edmProperty.getType().equals(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName().toString())
                && annotationColumn.scale() > 0) {
              edmProperty.setScale(Integer.valueOf(annotationColumn.scale()));
            }
          }
        }
      }
    }
  }

  static SRID getSRID(final Member member) {
    SRID result = null;
    if (member instanceof AnnotatedElement) {
      final AnnotatedElement annotatedElement = (AnnotatedElement) member;
      final EdmGeospatial spatialDetails = annotatedElement.getAnnotation(EdmGeospatial.class);
      if (spatialDetails != null) {
        final String srid = spatialDetails.srid();
        if (srid.isEmpty()) {
          result = SRID.valueOf(null);
        } else {
          result = SRID.valueOf(srid);
        }
        result.setDimension(spatialDetails.dimension());
      }
    }
    return result;
  }

  private String determineDefaultValue() throws ODataJPAModelException {
    if (javaMember instanceof Field && jpaAttribute.getPersistentAttributeType() == PersistentAttributeType.BASIC) {
      final Object value = accessor.getDefaultPropertyValue();
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  CsdlProperty getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmProperty;
  }

  /**
   * This is a workaround for the buggy Hibernate meta model: if a entity is using
   * an @IdClass then only the field/method in that IdClass is given by Hibernate
   * instead of the field/method in the entity class.
   *
   * @return The field or method used to describe the attribute, taken from JPA
   *         provider or <code>null</code>
   */
  private static Member determineJavaMemberOfAttribute(final Attribute<?, ?> jpaAttribute) {
    final Member member = jpaAttribute.getJavaMember();
    if (member == null) {
      throw new IllegalStateException("Cannot determine the member for '" + jpaAttribute.getName() + "' in "
          + jpaAttribute.getDeclaringType().getJavaType().getName() + ". Wrong inheritance used?");
    }
    if (member.getDeclaringClass() == jpaAttribute.getDeclaringType().getJavaType()
        || member.getDeclaringClass().isAssignableFrom(jpaAttribute.getDeclaringType().getJavaType())) {
      return member;
    }
    // workaround needed...
    if (jpaAttribute.getClass().getName().startsWith("org.hibernate") && LOG.isLoggable(Level.INFO)) {
      LOG.log(Level.INFO,
          "invalid metamodel of Hibernate found for "
              + jpaAttribute.getDeclaringType().getJavaType().getSimpleName() + "#" + jpaAttribute.getName()
              + "... use workaround");
    }
    if (Field.class.isInstance(member)) {
      for (final Field field : jpaAttribute.getDeclaringType().getJavaType().getDeclaredFields()) {
        if (field.getName().equals(jpaAttribute.getName())) {
          return field;
        }
      }
      // fallback
      LOG.log(Level.FINE, "Couldn't find matching (correct) field found for "
          + jpaAttribute.getDeclaringType().getJavaType().getSimpleName() + "#" + jpaAttribute.getName());
      return member;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private void buildProperty(final JPAEdmNameBuilder nameBuilder) throws ODataJPAModelException {
    // Set element specific attributes of super type
    this.setExternalName(nameBuilder.buildPropertyName(getInternalName()));

    type = serviceDocument.getStructuredType(jpaAttribute);

    if (javaMember instanceof AnnotatedElement) {
      final AnnotatedElement annotatedMember = (AnnotatedElement) javaMember;
      final EdmIgnore jpaIgnore = annotatedMember.getAnnotation(EdmIgnore.class);
      if (jpaIgnore != null) {
        this.setIgnore(true);
      }
      final Column jpaColumnDetails = annotatedMember.getAnnotation(Column.class);
      if (jpaColumnDetails != null) {
        dbFieldName = jpaColumnDetails.name();
        if (dbFieldName.isEmpty()) {
          final StringBuffer s = new StringBuffer(DB_FIELD_NAME_PATTERN);
          s.replace(1, 3, getInternalName());
          dbFieldName = s.toString();
        }
      } else {
        dbFieldName = getInternalName();
      }
      // TODO @Transient -> e.g. Calculated fields like formated name
      final EdmSearchable jpaSearchable = annotatedMember.getAnnotation(EdmSearchable.class);
      if (jpaSearchable != null) {
        searchable = true;
      }

      streamInfo = annotatedMember.getAnnotation(EdmMediaStream.class);
      if (streamInfo != null) {
        if ((streamInfo.contentType() == null || streamInfo.contentType().isEmpty())
            && (streamInfo.contentTypeAttribute() == null || streamInfo.contentTypeAttribute().isEmpty())) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.ANNOTATION_STREAM_INCOMPLETE,
              getInternalName());
        }
      }
      final Version jpaVersion = annotatedMember.getAnnotation(Version.class);
      if (jpaVersion != null) {
        isVersion = true;
      }
    }
  }

  @Override
  public boolean isAssociation() {
    return false;
  }

  @Override
  public String getDBFieldName() {
    return dbFieldName;
  }

  @Override
  public CsdlProperty getProperty() throws ODataJPAModelException {
    return getEdmItem();
  }

  @Override
  public Integer getMaxLength() {
    try {
      return getProperty().getMaxLength();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getPrecision() {
    try {
      return getProperty().getPrecision();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Integer getScale() {
    try {
      return getProperty().getScale();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isNullable() {
    try {
      return getProperty().isNullable();
    } catch (final ODataJPAModelException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean isSearchable() {
    return searchable;
  }

  String getContentType() {
    return streamInfo.contentType();
  }

  String getContentTypeProperty() {
    return streamInfo.contentTypeAttribute();
  }

  @Override
  public boolean isEtag() {
    return isVersion;
  }

  @Override
  public String toString() {
    return "IntermediateProperty [jpaAttribute=" + jpaAttribute + ", serviceDocument=" + serviceDocument + ", edmProperty="
        + edmProperty + ", type=" + type + ", dbFieldName=" + dbFieldName + ", searchable=" + searchable + ", isVersion="
        + isVersion + ", streamInfo=" + streamInfo + ", internalName=" + getInternalName() + "]";
  }

}
