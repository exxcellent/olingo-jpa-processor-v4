package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.Unary;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

public class JPALambdaAllOperation extends JPALambdaOperation {

  JPALambdaAllOperation(final JPAEntityFilterProcessor jpaComplier, final Member member) {
    super(jpaComplier, member);
  }

  public Subquery<?> getNotExistsQuery() throws ODataApplicationException {
    try {
      return buildFilterSubQueries(new NotExpression(determineExpression()));
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  @Override
  public Expression<Boolean> get() throws ODataApplicationException {
    final CriteriaBuilder cb = getQueryBuilder().getEntityManager().getCriteriaBuilder();
    return cb.and(cb.exists(buildFilterSubQueries()), cb.not(cb.exists(getNotExistsQuery())));
  }

  private class NotExpression implements Unary {
    private final org.apache.olingo.server.api.uri.queryoption.expression.Expression expression;

    public NotExpression(final org.apache.olingo.server.api.uri.queryoption.expression.Expression expression) {
      super();
      this.expression = expression;
    }

    @Override
    public <T> T accept(final ExpressionVisitor<T> visitor) throws ExpressionVisitException, ODataApplicationException {
      final T operand = expression.accept(visitor);
      return visitor.visitUnaryOperator(getOperator(), operand);
    }

    @Override
    public org.apache.olingo.server.api.uri.queryoption.expression.Expression getOperand() {
      return expression;
    }

    @Override
    public UnaryOperatorKind getOperator() {
      return UnaryOperatorKind.NOT;
    }

  }

}
