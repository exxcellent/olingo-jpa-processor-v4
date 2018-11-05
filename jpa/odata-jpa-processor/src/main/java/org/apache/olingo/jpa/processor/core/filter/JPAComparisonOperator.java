package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

public interface JPAComparisonOperator<T extends Comparable<T>> extends JPAExpressionOperator<BinaryOperatorKind> {

	Expression<T> getLeft() throws ODataApplicationException;

	Object getRight();

	Comparable<T> getRightAsComparable() throws ODataApplicationException;

	Expression<T> getRightAsExpression() throws ODataApplicationException;

}