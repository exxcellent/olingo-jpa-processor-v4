package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

class JPAArithmeticOperatorImp extends JPAAbstractBinaryOperatorImp<Number, Number> implements
JPAArithmeticOperator {
  private final JPAODataDatabaseProcessor converter;

  public JPAArithmeticOperatorImp(final JPAODataDatabaseProcessor converter, final BinaryOperatorKind operator,
      final JPAExpressionElement<Number> left, final JPAExpressionElement<Number> right) {
    super(operator, left, right);
    this.converter = converter;
  }

  @Override
  public Expression<Number> get() throws ODataApplicationException {
    return converter.createCalculation(getOperator(), determineLeftExpression(), determineRightExpression());
  }

}
