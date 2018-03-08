package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import javax.persistence.AttributeConverter;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAttribute extends JPAElement {

	public AttributeConverter<?, ?> getConverter();

	public JPAStructuredType getStructuredType();

	/**
	 *
	 * @return The direct type of simple attributes or the element type if attribute
	 *         is a collection.
	 */
	public Class<?> getType();

	/**
	 *
	 * @return TRUE if the type of the property is a complex type, having embedded
	 *         properties.
	 */
	public boolean isComplex();

	public boolean isKey();

	public boolean isAssociation();

	public boolean isSearchable();

	public boolean isCollection();

	/**
	 *
	 * @return TRUE if the property/attribute is of any JAVA simple type (not
	 *         {@link #isComplex()} and not {@link #isAssociation()}), maybe in a
	 *         collection.
	 */
	public boolean isPrimitive();

	public CsdlAbstractEdmItem getProperty() throws ODataJPAModelException;

}
