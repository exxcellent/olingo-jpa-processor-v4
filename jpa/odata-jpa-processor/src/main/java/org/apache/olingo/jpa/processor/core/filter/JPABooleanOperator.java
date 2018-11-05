package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

public interface JPABooleanOperator extends JPAExpressionOperator<BinaryOperatorKind> {

	Expression<Boolean> getLeft() throws ODataApplicationException;

	Expression<Boolean> getRight() throws ODataApplicationException;

}