package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

public interface JPAUnaryBooleanOperator extends JPAExpressionOperator<UnaryOperatorKind> {

	public Expression<Boolean> getOperand() throws ODataApplicationException;

}