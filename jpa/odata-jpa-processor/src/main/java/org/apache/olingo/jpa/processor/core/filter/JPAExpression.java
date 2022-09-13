package org.apache.olingo.jpa.processor.core.filter;

import jakarta.persistence.criteria.Expression;

/**
 *
 * @author Ralf zozmann
 *
 * @param <T> The type of resulting expression
 */
public interface JPAExpression<T> extends JPAExpressionElement<Expression<T>> {
  // marker interface for expression element producing a
  // jakarta.persistence.criteria.Expression
}