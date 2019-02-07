package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

//
class JPAComparisonOperatorImp<T extends Comparable<T>> implements JPAComparisonOperator<T> {
	private final JPAODataDatabaseProcessor converter;
	private final BinaryOperatorKind operator;
	private final JPAExpressionElement<?> left;
	private final JPAExpressionElement<?> right;

	public JPAComparisonOperatorImp(final JPAODataDatabaseProcessor converter, final BinaryOperatorKind operator,
			final JPAExpressionElement<?> left, final JPAExpressionElement<?> right) {
		super();
		this.converter = converter;
		this.operator = operator;
		this.left = left;
		this.right = right;
	}

	@Override
	public Expression<Boolean> get() throws ODataApplicationException {
		return converter.convert(this);
	}

	@Override
	public BinaryOperatorKind getOperator() {
		return operator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<T> getLeft() throws ODataApplicationException {
		if (left instanceof JPALiteralOperator) {
			return (Expression<T>) right.get();
		}
		return (Expression<T>) left.get();
	}

	@Override
	public Object getRight() {
		if (left instanceof JPALiteralOperator) {
			return left;
		}
		return right;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getRightAsComparable() throws ODataApplicationException {
		if (left instanceof JPALiteralOperator) {
			if (right instanceof JPAMemberOperator) {
				return (T) ((JPALiteralOperator) left).get(((JPAMemberOperator) right).determineAttribute());
			} else {
				return (T) left.get();
			}
		}
		if (right instanceof JPALiteralOperator) {
			if (left instanceof JPAMemberOperator) {
				return (T) ((JPALiteralOperator) right).get(((JPAMemberOperator) left).determineAttribute());
			} else {
				return (T) right.get();
			}
		}
		return (T) right.get();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<T> getRightAsExpression() throws ODataApplicationException {
		return (Expression<T>) right.get();
	}
}