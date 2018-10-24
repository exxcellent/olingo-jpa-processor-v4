package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

class JPAUnaryBooleanOperatorImp implements JPAUnaryBooleanOperator {

	private final JPAOperationConverter converter;
	private final UnaryOperatorKind operator;
	private final JPAOperator<Expression<Boolean>> operand;

	public JPAUnaryBooleanOperatorImp(final JPAOperationConverter converter, final UnaryOperatorKind operator,
			final JPAOperator<Expression<Boolean>> operand) {
		super();
		this.converter = converter;
		this.operator = operator;
		this.operand = operand;
	}

	@Override
	public Expression<Boolean> get() throws ODataApplicationException {
		return converter.convert(this);
	}

	@Override
	public Expression<Boolean> getOperand() throws ODataApplicationException {
		return operand.get();
	}

	@Override
	public UnaryOperatorKind getOperator() {
		return operator;
	}

}
