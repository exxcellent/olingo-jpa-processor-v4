package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;

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
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;

/**
 * A Property is described on the one hand by its Name and Type and on the other hand by its Property Facets. The
 * type is a qualified name of either a primitive type, a complex type or a enumeration type. Primitive types are mapped
 * by {@link JPATypeConvertor}.
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

	IntermediateProperty(final JPAEdmNameBuilder nameBuilder, final Attribute<?, ?> jpaAttribute,
	        final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {

		super(nameBuilder, jpaAttribute.getName());
		this.jpaAttribute = jpaAttribute;
		this.serviceDocument = serviceDocument;

		isComplex = (jpaAttribute.getPersistentAttributeType() == PersistentAttributeType.EMBEDDED)
		        || JPATypeConvertor.isCollectionTypeOfEmbeddable(jpaAttribute);
		buildProperty(nameBuilder);
		accessor = new FieldAttributeAccessor((Field) jpaAttribute.getJavaMember());
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
	public boolean isPrimitive() {
		if (isComplex()) {
			return false;
		}
		if (isCollection()) {
			return JPATypeConvertor.isCollectionTypeOfPrimitive(jpaAttribute);
		}
		return JPATypeConvertor.isPrimitiveType(jpaAttribute);
	}

	@Override
	public boolean isCollection() {
		return jpaAttribute.isCollection();
	}

	boolean isStream() {
		return streamInfo == null ? false : streamInfo.stream();
	}

	@SuppressWarnings("unchecked")
	private FullQualifiedName createTypeName() throws ODataJPAModelException {
		switch (jpaAttribute.getPersistentAttributeType()) {
		case BASIC:
			if (jpaAttribute.getJavaType().isEnum()) {
				// register enum type
				final IntermediateEnumType jpaEnumType = serviceDocument
				        .createEnumType((Class<? extends Enum<?>>) jpaAttribute.getJavaType());
				return jpaEnumType.getExternalFQN();
			} else {
				return JPATypeConvertor.convertToEdmSimpleType(jpaAttribute.getJavaType(), jpaAttribute).getFullQualifiedName();
			}
		case EMBEDDED:
			return nameBuilder.buildFQN(type.getExternalName());
		case ELEMENT_COLLECTION:
			final PluralAttribute<?, ?, ?> pa = (PluralAttribute<?, ?, ?>) jpaAttribute;
			if (JPATypeConvertor.isCollectionTypeOfPrimitive(jpaAttribute)) {
				return JPATypeConvertor.convertToEdmSimpleType(pa.getElementType().getJavaType(), pa).getFullQualifiedName();
			} else if (JPATypeConvertor.isCollectionTypeOfEmbeddable(jpaAttribute)) {
				return serviceDocument.getStructuredType(jpaAttribute).getExternalFQN();
			}
		default:
			// trigger exception if not possible
			return JPATypeConvertor.convertToEdmSimpleType(jpaAttribute.getJavaType(), jpaAttribute).getFullQualifiedName();
		}
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmProperty == null) {
			edmProperty = new CsdlProperty();
			edmProperty.setName(this.getExternalName());

			edmProperty.setType(createTypeName());// trigger exception for unsupported attribute types
			edmProperty.setCollection(jpaAttribute.isCollection());

			if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
				Integer maxLength = null;
				final Size annotationSize = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(Size.class);
				if (annotationSize != null) {
					maxLength = Integer.valueOf(annotationSize.max());
				}

				final Column annotationColumn = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(Column.class);
				if (annotationColumn != null) {
					if (maxLength == null && annotationColumn.length() > 0) {
						maxLength = Integer.valueOf(annotationColumn.length());
					}
					edmProperty.setNullable(annotationColumn.nullable());
					edmProperty.setSrid(getSRID(jpaAttribute.getJavaMember()));
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
							edmProperty.setPrecision(annotationColumn.precision());
						}
						if (edmProperty.getType().equals(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName().toString())
						        && annotationColumn.scale() > 0) {
							edmProperty.setScale(annotationColumn.scale());
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
		String valueString = null;
		if (jpaAttribute.getJavaMember() instanceof Field && jpaAttribute.getPersistentAttributeType() == PersistentAttributeType.BASIC) {
			// It is not possible to get the default value directly from the Field,
			// only from an instance field.get(Object obj).toString();
			try {
				final Field field = (Field) jpaAttribute.getJavaMember();
				final Constructor<?> constructor = jpaAttribute.getDeclaringType().getJavaType().getConstructor();
				final Object pojo = constructor.newInstance();
				field.setAccessible(true);
				final Object value = field.get(pojo);
				if (value != null) {
					valueString = value.toString();
				}
			} catch (final InstantiationException | NoSuchMethodException e) {
				// Class could not be instantiated e.g. abstract class like Business Partner=> default could not be determined
				// and will be ignored
			} catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.PROPERTY_DEFAULT_ERROR, e, jpaAttribute.getName());
			}
		}
		return valueString;
	}

	@Override
	CsdlProperty getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmProperty;
	}

	private void buildProperty(final JPAEdmNameBuilder nameBuilder) throws ODataJPAModelException {
		// Set element specific attributes of super type
		this.setExternalName(nameBuilder.buildPropertyName(internalName));

		type = serviceDocument.getStructuredType(jpaAttribute);

		if (this.jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
			final EdmIgnore jpaIgnore = ((AnnotatedElement) this.jpaAttribute.getJavaMember()).getAnnotation(EdmIgnore.class);
			if (jpaIgnore != null) {
				this.setIgnore(true);
			}
			final Column jpaColumnDetails = ((AnnotatedElement) this.jpaAttribute.getJavaMember()).getAnnotation(Column.class);
			if (jpaColumnDetails != null) {
				dbFieldName = jpaColumnDetails.name();
				if (dbFieldName.isEmpty()) {
					final StringBuffer s = new StringBuffer(DB_FIELD_NAME_PATTERN);
					s.replace(1, 3, internalName);
					dbFieldName = s.toString();
				}
			} else {
				dbFieldName = internalName;
			}
			// TODO @Transient -> e.g. Calculated fields like formated name
			final EdmSearchable jpaSearchable = ((AnnotatedElement) this.jpaAttribute.getJavaMember()).getAnnotation(EdmSearchable.class);
			if (jpaSearchable != null) {
				searchable = true;
			}

			streamInfo = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(EdmMediaStream.class);
			if (streamInfo != null) {
				if ((streamInfo.contentType() == null || streamInfo.contentType().isEmpty())
				        && (streamInfo.contentTypeAttribute() == null || streamInfo.contentTypeAttribute().isEmpty())) {
					throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.ANNOTATION_STREAM_INCOMPLETE, internalName);
				}
			}
			final Version jpaVersion = ((AnnotatedElement) this.jpaAttribute.getJavaMember()).getAnnotation(Version.class);
			if (jpaVersion != null) {
				isVersion = true;
			}
		}
		postProcessor.processProperty(this, jpaAttribute.getDeclaringType().getJavaType().getCanonicalName());
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
		        + isVersion + ", streamInfo=" + streamInfo + ", internalName=" + internalName + "]";
	}

}
