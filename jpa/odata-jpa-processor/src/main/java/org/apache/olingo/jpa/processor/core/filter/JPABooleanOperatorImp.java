package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

class JPABooleanOperatorImp implements JPABooleanOperator {

	private final JPAOperationConverter converter;
	private final BinaryOperatorKind operator;
	private final JPAOperator<Expression<Boolean>> left;
	private final JPAOperator<Expression<Boolean>> right;

	public JPABooleanOperatorImp(final JPAOperationConverter converter, final BinaryOperatorKind operator,
			final JPAOperator<Expression<Boolean>> left, final JPAOperator<Expression<Boolean>> right) {
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
	public Expression<Boolean> getLeft() throws ODataApplicationException {
		return left.get();
	}

	@Override
	public Expression<Boolean> getRight() throws ODataApplicationException {
		return right.get();
	}

}