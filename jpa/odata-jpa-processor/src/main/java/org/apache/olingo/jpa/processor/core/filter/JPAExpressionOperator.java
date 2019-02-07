package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

public interface JPAExpressionOperator<E extends Enum<?>> extends JPAExpression<Expression<Boolean>> {
	public E getOperator();

}
