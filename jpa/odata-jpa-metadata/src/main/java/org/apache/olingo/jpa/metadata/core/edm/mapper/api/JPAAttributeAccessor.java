package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Abtract representation to read/write property values (via
 * {@link java.lang.reflect.Field field} or {@link java.lang.reflect.Method
 * method}).
 *
 * @author Ralf Zozmann
 *
 */
public interface JPAAttributeAccessor {
	public void setPropertyValue(final Object jpaEntity, final Object jpaPropertyValue) throws ODataJPAModelException;

	public Object getPropertyValue(final Object jpaEntity) throws ODataJPAModelException;
}
