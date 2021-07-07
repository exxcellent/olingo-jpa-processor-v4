package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

class JPAUnaryBooleanOperationImpl implements JPAUnaryBooleanOperation {

  private final JPAODataDatabaseProcessor converter;
  private final UnaryOperatorKind operator;
  private final JPAExpression<Boolean> operand;

  public JPAUnaryBooleanOperationImpl(final JPAODataDatabaseProcessor converter, final UnaryOperatorKind operator,
      final JPAExpression<Boolean> operand) {
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
