package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

public final class JPAComparisonOperatorImp extends JPAAbstractBinaryOperatorImp<Comparable<?>, Boolean> {

  private final JPAODataDatabaseProcessor converter;

  public JPAComparisonOperatorImp(final JPAODataDatabaseProcessor converter, final BinaryOperatorKind operator,
      final JPAExpressionElement<Comparable<?>> left, final JPAExpressionElement<Comparable<?>> right) {
    super(operator, left, right);
    this.converter = converter;
  }

  @Override
  public Expression<Boolean> get() throws ODataApplicationException {
    return converter.createComparison(getOperator(), determineLeftExpression(), determineRightExpression());
  }

}