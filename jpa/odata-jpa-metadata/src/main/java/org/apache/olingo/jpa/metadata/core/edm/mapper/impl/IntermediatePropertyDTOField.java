package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.validation.constraints.Size;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * A DTO is mapped as OData entity!
 *
 * @author rzozmann
 *
 */
class IntermediatePropertyDTOField extends IntermediateModelElement implements JPASimpleAttribute {

	private final Field field;
	private final JPAAttributeAccessor accessor;
	private CsdlProperty edmProperty = null;

	public IntermediatePropertyDTOField(final JPAEdmNameBuilder nameBuilder, final Field field,
			final IntermediateServiceDocument serviceDocument) {
		super(nameBuilder, field.getName());
		this.field = field;
		this.setExternalName(nameBuilder.buildPropertyName(field.getName()));
		accessor = new FieldAttributeAccessor(field);
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

	private FullQualifiedName createTypeName() throws ODataJPAModelException {
		if (isCollection()) {
			final Class<?> elementType = TypeMapping.extractElementTypeOfCollection(field);
			return TypeMapping.convertToEdmSimpleType(elementType).getFullQualifiedName();
		} else {
			// trigger exception if not possible
			return TypeMapping.convertToEdmSimpleType(field).getFullQualifiedName();
		}
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmProperty != null) {
			return;
		}
		edmProperty = new CsdlProperty();
		edmProperty.setName(this.getExternalName());

		edmProperty.setType(createTypeName());// trigger exception for unsupported attribute types
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

	@SuppressWarnings("unchecked")
	@Override
	CsdlProperty getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmProperty;
	}

	@Override
	public JPAStructuredType getStructuredType() {
		throw new UnsupportedOperationException();
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
	public boolean isComplex() {
		return !isPrimitive();
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
	public boolean isCollection() {
		if (Collection.class.isAssignableFrom(field.getType())) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return TypeMapping.isPrimitiveType(field);
	}

	@Override
	public CsdlProperty getProperty() throws ODataJPAModelException {
		return getEdmItem();
	}

	@Override
	public String getDBFieldName() {
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		return field.getAnnotation(annotationClass);
	}
}
