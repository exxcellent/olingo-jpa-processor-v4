package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;

class JPABooleanOperationImpl implements JPABooleanOperation {

  private final JPAODataDatabaseProcessor converter;
  private final BinaryOperatorKind operator;
  private final JPAExpression<Boolean> left;
  private final JPAExpression<Boolean> right;

  public JPABooleanOperationImpl(final JPAODataDatabaseProcessor converter, final BinaryOperatorKind operator,
      final JPAExpression<Boolean> left, final JPAExpression<Boolean> right) {
    super();
    this.converter = converter;
    this.operator = operator;
    this.left = left;
    this.right = right;
  }

  @Override
  public org.apache.olingo.server.api.uri.queryoption.expression.Expression getQueryExpressionElement() {
    return new BinaryImpl(left.getQueryExpressionElement(), operator, right.getQueryExpressionElement(), null);
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