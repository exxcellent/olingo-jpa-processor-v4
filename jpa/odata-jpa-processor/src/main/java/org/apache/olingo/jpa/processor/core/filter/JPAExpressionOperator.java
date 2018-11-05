package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

public interface JPAExpressionOperator<E extends Enum<?>> extends JPAOperator<Expression<Boolean>> {
	public E getOperator();

}
