package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;

class JPAVisitor implements ExpressionVisitor<JPAExpressionElement<?>> {

  private final JPAEntityFilterProcessor<?> filterProcessor;

  /**
   * @param jpaFilterCrossComplier
   */
  JPAVisitor(final JPAEntityFilterProcessor<?> jpaFilterCrossComplier) {
    this.filterProcessor = jpaFilterCrossComplier;
  }

  @Override
  public JPAExpression<?> visitAlias(final String aliasName)
      throws ExpressionVisitException, ODataApplicationException {
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
        HttpStatusCode.NOT_IMPLEMENTED, "Alias");
  }

  @SuppressWarnings("unchecked")
  @Override
  public JPAExpressionElement<?> visitBinaryOperator(final BinaryOperatorKind operator,
      final JPAExpressionElement<?> left, final JPAExpressionElement<?> right)
          throws ExpressionVisitException, ODataApplicationException {

    if (hasMemberNavigation(left) || hasMemberNavigation(right)) {
      return new JPAMemberOperationNavigation(this.filterProcessor, operator, left, right);
    }
    if (operator == BinaryOperatorKind.EQ
        || operator == BinaryOperatorKind.NE
        || operator == BinaryOperatorKind.GE
        || operator == BinaryOperatorKind.GT
        || operator == BinaryOperatorKind.LT
        || operator == BinaryOperatorKind.LE) {
      return new JPAComparisonOperatorImp(this.filterProcessor.getConverter(), operator,
          (JPAExpressionElement<Comparable<?>>) left, (JPAExpressionElement<Comparable<?>>) right);
    } else if (operator == BinaryOperatorKind.AND || operator == BinaryOperatorKind.OR) {
      return new JPABooleanOperationImpl(this.filterProcessor.getConverter(), operator,
          checkBooleanExpressionOperand(left), checkBooleanExpressionOperand(right));
    } else if (operator == BinaryOperatorKind.ADD
        || operator == BinaryOperatorKind.SUB
        || operator == BinaryOperatorKind.MUL
        || operator == BinaryOperatorKind.DIV
        || operator == BinaryOperatorKind.MOD) {
      return new JPAArithmeticOperatorImp(this.filterProcessor.getConverter(), operator,
          (JPAExpressionElement<Number>) left,
          (JPAExpressionElement<Number>) right);
    }
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
        HttpStatusCode.NOT_IMPLEMENTED, operator.name());
  }

  @Override
  public JPAExpressionElement<?> visitBinaryOperator(final BinaryOperatorKind operator,
      final JPAExpressionElement<?> left,
      final List<JPAExpressionElement<?>> right) throws ExpressionVisitException, ODataApplicationException {
    if (right.isEmpty()) {
      return visitBinaryOperator(operator, left, (JPAExpressionElement<?>) null);
    }
    if (right.size() > 1) {
      throw new UnsupportedOperationException("Multiple expressions on right side are currently not supported");
    }
    return visitBinaryOperator(operator, left, right.get(0));
  }

  @SuppressWarnings("unchecked")
  private JPAExpression<Boolean> checkBooleanExpressionOperand(final JPAExpressionElement<?> operator)
      throws ODataJPAFilterException {
    if (JPAExpressionOperation.class.isInstance(operator)) {
      return JPAExpressionOperation.class.cast(operator);
    } else if (ODataBuiltinFunctionCall.class.isInstance(operator)) {
      // only a few builtin functions are of result type 'boolean'
      switch (ODataBuiltinFunctionCall.class.cast(operator).getFunctionKind()) {
      case CONTAINS:
      case STARTSWITH:
      case ENDSWITH:
        return (JPAExpression<Boolean>) operator;
      default:
        // throw exception at end of method
      }
    } else if (JPAExistsOperation.class.isInstance(operator)) {
      return JPAExistsOperation.class.cast(operator);
    }
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
        HttpStatusCode.NOT_IMPLEMENTED, operator.getClass().getSimpleName());

  }

  @Override
  public JPAExpressionElement<?> visitEnum(final EdmEnumType type, final List<String> enumValues)
      throws ExpressionVisitException,
      ODataApplicationException {
    if (enumValues.isEmpty()) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
          HttpStatusCode.NOT_IMPLEMENTED, "Empty Enumeration value");
    }
    if (enumValues.size() > 1) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
          HttpStatusCode.NOT_IMPLEMENTED, "Multiple Enumeration values");
    }
    final Literal literal = new LiteralImpl(enumValues.get(0), type);
    return new JPALiteralOperand(getOdata(), getCriteriaBuilder(), literal);
  }

  @Override
  public JPAExpressionElement<Expression<?>> visitLambdaExpression(final String lambdaFunction,
      final String lambdaVariable,
      final org.apache.olingo.server.api.uri.queryoption.expression.Expression expression)
          throws ExpressionVisitException, ODataApplicationException {
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
        HttpStatusCode.NOT_IMPLEMENTED, "Lambda Expression");
  }

  @Override
  public JPAExpressionElement<Expression<?>> visitLambdaReference(final String variableName)
      throws ExpressionVisitException,
      ODataApplicationException {
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
        HttpStatusCode.NOT_IMPLEMENTED, "Lambda Reference");
  }

  @Override
  public JPAExpressionElement<?> visitLiteral(final Literal literal)
      throws ExpressionVisitException, ODataApplicationException {
    return new JPALiteralOperand(getOdata(), getCriteriaBuilder(), literal);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public JPAExpressionElement<?> visitMember(final Member member)
      throws ExpressionVisitException, ODataApplicationException {
    // the member may contain navigation elements... we ignore that here, because the member will be part of something
    // other (like operator, lambda, method call, ... where we check for required navigation)
    if (getLambdaType(member.getResourcePath()) == UriResourceKind.lambdaAny) {
      return new JPALambdaAnyOperation(filterProcessor, member);
    } else if (getLambdaType(member.getResourcePath()) == UriResourceKind.lambdaAll) {
      return new JPALambdaAllOperation(filterProcessor, member);
    } else if (isAggregation(member.getResourcePath())) {
      return new JPAAggregationOperationCountImpl(filterProcessor.getParent(), filterProcessor.getConverter(), member);
    } else if (isCustomFunction(member.getResourcePath())) {
      return new JPADatabaseFunctionCall(filterProcessor, member);
    }
    return new JPAMemberOperand(filterProcessor.getJpaEntityType(), this.filterProcessor.getParent()
        .getQueryResultFrom(),
        member);
  }

  @Override
  public JPAExpressionElement<?> visitMethodCall(final MethodKind methodCall,
      final List<JPAExpressionElement<?>> parameters)
          throws ExpressionVisitException, ODataApplicationException {
    for (final JPAExpressionElement<?> p : parameters) {
      if (hasMemberNavigation(p)) {
        return new JPAMemberFunctionCallNavigation(this.filterProcessor, methodCall, parameters);
      }
    }
    return new ODataBuiltinFunctionCallImpl(this.filterProcessor.getConverter(), methodCall, parameters);
  }

  @Override
  public JPAExpressionElement<?> visitTypeLiteral(final EdmType type)
      throws ExpressionVisitException, ODataApplicationException {
    return new JPALiteralTypeOperand(type);
  }

  @Override
  public JPAExpressionElement<?> visitUnaryOperator(final UnaryOperatorKind operator,
      final JPAExpressionElement<?> operand)
          throws ExpressionVisitException, ODataApplicationException {
    if (operator == UnaryOperatorKind.NOT) {
      return new JPAUnaryBooleanOperationImpl(this.filterProcessor.getConverter(), operator,
          checkBooleanExpressionOperand(operand));
    } else {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
          HttpStatusCode.NOT_IMPLEMENTED, operator.name());
    }
  }

  UriResourceKind getLambdaType(final UriInfoResource member) {
    for (final UriResource r : member.getUriResourceParts()) {
      if (r.getKind() == UriResourceKind.lambdaAny
          || r.getKind() == UriResourceKind.lambdaAll) {
        return r.getKind();
      }
    }
    return null;
  }

  /**
   * Inspect several expression types for nested resource paths...
   * @return These are navigations then the method will return TRUE.
   */
  boolean hasMemberNavigation(final JPAExpressionElement<?> operand) {
    if (operand instanceof JPAMemberOperand) {
      final List<UriResource> uriResourceParts = ((JPAMemberOperand<?>) operand).getMember().getResourcePath()
          .getUriResourceParts();
      for (int i = uriResourceParts.size() - 1; i >= 0; i--) {
        if (uriResourceParts.get(i) instanceof UriResourceNavigation) {
          return true;
        }
        // a collection (@ElementCollection) should have a navigation
        if(uriResourceParts.get(i) instanceof UriResourceProperty && UriResourceProperty.class.cast(uriResourceParts.get(i)).isCollection()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isAggregation(final UriInfoResource resourcePath) {
    if (resourcePath.getUriResourceParts().size() == 1 && resourcePath.getUriResourceParts().get(0)
        .getKind() == UriResourceKind.count) {
      return true;
    }
    return false;
  }

  private boolean isCustomFunction(final UriInfoResource resourcePath) {
    if (resourcePath.getUriResourceParts().size() > 0 && resourcePath.getUriResourceParts().get(
        0) instanceof UriResourceFunction) {
      return true;
    }
    return false;
  }

  private CriteriaBuilder getCriteriaBuilder() {
    return filterProcessor.getEntityManager().getCriteriaBuilder();
  }

  private OData getOdata() {
    return filterProcessor.getOdata();
  }
}