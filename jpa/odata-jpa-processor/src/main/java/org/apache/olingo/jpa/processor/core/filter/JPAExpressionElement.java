package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * @author Ralf zozmann
 *
 * @param <T> The result type of parameter
 */
//TODO remove this interface by using only 'JPAExpression<Expression>' as more correct style
public interface JPAExpressionElement<T> {
	public T get() throws ODataApplicationException;
}