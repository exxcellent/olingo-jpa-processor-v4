package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;
import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;

/**
 * In case the query result shall be filtered on an attribute of navigation target a sub-select will be generated.
 * @author Oliver Grande
 *
 */
class JPAMemberOperationNavigation extends AbstractMemberNavigation implements
JPAExpressionOperation<BinaryOperatorKind, Boolean> {

  private final BinaryOperatorKind operator;
  private final JPAMemberOperand<?> jpaMember;
  private final JPALiteralOperand operand;

  JPAMemberOperationNavigation(final JPAEntityFilterProcessor<?> jpaComplier, final BinaryOperatorKind operator,
      final JPAExpressionElement<?> left, final JPAExpressionElement<?> right) {

    super(jpaComplier);
    this.operator = operator;
    if (left instanceof JPAMemberOperand) {
      jpaMember = (JPAMemberOperand<?>) left;
      operand = (JPALiteralOperand) right;
    } else {
      jpaMember = (JPAMemberOperand<?>) right;
      operand = (JPALiteralOperand) left;
    }
  }

  @Override
  protected JPAMemberOperand<?> getNavigatingMember() {
    return jpaMember;
  }

  @Override
  public Expression getQueryExpressionElement() {
    // reuse existing call, but with original member (having all the navigation parts)
    return (Expression) buildResultingExpression(jpaMember.getMember());
  }

  @Override
  protected VisitableExpression buildResultingExpression(final Member attribute) {
    return new BinaryImpl(attribute, operator, operand.getODataLiteral(), null);
  }

  @Override
  public BinaryOperatorKind getOperator() {
    return operator;
  }
}
