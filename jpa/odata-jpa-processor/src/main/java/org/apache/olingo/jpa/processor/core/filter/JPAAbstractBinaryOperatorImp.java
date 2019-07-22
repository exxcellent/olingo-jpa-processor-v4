package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;

/**
 *
 * @author Ralf Zozmann
 *
 * @param <OT> The operand(s) type
 * @param <ET> The result expression type
 */
@SuppressWarnings("rawtypes")
abstract class JPAAbstractBinaryOperatorImp<OT, ET> implements JPAExpressionOperator<BinaryOperatorKind, ET> {

  private final BinaryOperatorKind operator;
  private final JPAExpressionElement<OT> left;
  private final JPAExpressionElement<OT> right;

  public JPAAbstractBinaryOperatorImp(final BinaryOperatorKind operator,
      final JPAExpressionElement<OT> left,
      final JPAExpressionElement<OT> right) {
    super();
    this.operator = operator;
    this.left = left;
    this.right = right;
  }

  @SuppressWarnings("unchecked")
  protected final Expression<OT> determineLeftExpression() throws ODataApplicationException {
    if (left instanceof JPALiteralOperand) {
      return determineLiteralExpression((JPALiteralOperand) left, right);
    }
    // default behaviour
    return ((JPAExpression<OT>) left).get();
  }

  @SuppressWarnings("unchecked")
  protected final Expression<OT> determineRightExpression() throws ODataApplicationException {
    if (right instanceof JPALiteralOperand) {
      return determineLiteralExpression((JPALiteralOperand) right, left);
    }
    // default behaviour
    return ((JPAExpression<OT>) right).get();
  }

  /**
   * Try to determine the best literal value object based on hints taken from the other expression operand, normally
   * having more specific type information about literal (because a entity member attribute with meta informations)
   */
  @SuppressWarnings("unchecked")
  private Expression<OT> determineLiteralExpression(final JPALiteralOperand literalOperand,
      final JPAExpressionElement operandContext) throws ODataApplicationException {
    if (operandContext instanceof JPAMemberOperator) {
      final JPAMemberOperator rMemberOperand = (JPAMemberOperator) operandContext;
      return (Expression<OT>) literalOperand.getLiteralExpression(rMemberOperand.determineAttribute());
    } else if (operandContext instanceof ODataBuiltinFunctionCall) {
      final ODataBuiltinFunctionCall functionOperand = (ODataBuiltinFunctionCall) operandContext;
      final EdmPrimitiveTypeKind type = functionOperand.getResultType();
      return (Expression<OT>) literalOperand.getLiteralExpression(type);
    } else if (operandContext instanceof JPAFunctionOperator) {
      final JPAFunctionOperator functionOperand = (JPAFunctionOperator) operandContext;
      return (Expression<OT>) literalOperand.getLiteralExpression(functionOperand.getReturnType());
    }
    // default behaviour
    return (Expression<OT>) literalOperand.getLiteralExpression();
  }

  @Override
  public final BinaryOperatorKind getOperator() {
    return operator;
  }

}