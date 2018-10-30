package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.lang.annotation.Annotation;

public interface JPATypedElement {

	/**
	 *
	 * @return The direct type of simple attributes (or parameter or return value)
	 *         or the element type if the element is a collection.
	 */
	public Class<?> getType();

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
