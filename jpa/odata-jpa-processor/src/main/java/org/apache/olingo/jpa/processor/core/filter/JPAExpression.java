package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

/**
 *
 * @author Ralf zozmann
 *
 * @param <T> The result type of parameter
 */
public interface JPAExpression<T extends Expression<?>> extends JPAExpressionElement<T> {
	// marker interface for expression element producing a
	// javax.persistence.criteria.Expression
}