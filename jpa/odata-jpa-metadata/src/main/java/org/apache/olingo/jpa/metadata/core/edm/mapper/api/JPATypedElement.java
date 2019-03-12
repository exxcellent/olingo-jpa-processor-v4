package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.lang.annotation.Annotation;

public interface JPATypedElement {

	/**
	 *
	 * @return The direct type of simple attributes (or parameter or return value)
	 *         or the element type if the element is a collection.
	 */
	public Class<?> getType();

	public Integer getMaxLength();

	public Integer getPrecision();

	public Integer getScale();

	public boolean isNullable();

	public boolean isCollection();

	/**
	 *
	 * @return TRUE if the property/attribute is of any JAVA simple type (not
	 *         {@link #isComplex()} and not {@link #isAssociation()}), maybe in a
	 *         collection.
	 */
	public boolean isPrimitive();

	/**
	 * Wrapper to get annotation from the underlying property representation (field
	 * , method,...).
	 *
	 * @param annotationClass The requested annotation class
	 * @return The annotation or <code>null</code>.
	 * @see java.lang.reflect.Field#getAnnotation(Class)
	 */
	@Deprecated
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass);

}
