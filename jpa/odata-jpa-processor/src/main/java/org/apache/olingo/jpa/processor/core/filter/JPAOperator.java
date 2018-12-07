package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * @author Ralf zozmann
 *
 * @param <T> The type of operator
 */
public interface JPAOperator<T> {
	public T get() throws ODataApplicationException;
}