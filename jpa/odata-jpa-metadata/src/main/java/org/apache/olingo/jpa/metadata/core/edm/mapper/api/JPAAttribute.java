package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.lang.annotation.Annotation;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAttribute extends JPAElement {

	/**
	 *
	 * @return The type of the attribute represented by the intermediate api.
	 */
	public JPAStructuredType getStructuredType();

	/**
	 *
	 * @return The direct type of simple attributes or the element type if attribute
	 *         is a collection.
	 */
	public Class<?> getType();

	/**
	 *
	 * @return The wrapper to encapsulate reading/writing property values.
	 */
	public JPAAttributeAccessor getAttributeAccessor();

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

	public boolean ignore();

	/**
	 * Wrapper to get annotation from the underlying property representation (field
	 * , method,...).
	 *
	 * @param annotationClass
	 *            The requested annotation class
	 * @return The annotation or <code>null</code>.
	 * @see java.lang.reflect.Field#getAnnotation(Class)
	 */
	@Deprecated
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass);

	/**
	 *
	 * @return TRUE if the property/attribute is of any JAVA simple type (not
	 *         {@link #isComplex()} and not {@link #isAssociation()}), maybe in a
	 *         collection.
	 */
	public boolean isPrimitive();

	public CsdlAbstractEdmItem getProperty() throws ODataJPAModelException;

}
