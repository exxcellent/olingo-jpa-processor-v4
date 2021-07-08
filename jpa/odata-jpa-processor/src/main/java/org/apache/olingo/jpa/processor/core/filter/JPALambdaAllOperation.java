package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;
import org.apache.olingo.server.core.uri.queryoption.expression.UnaryImpl;

public class JPALambdaAllOperation extends JPALambdaOperation {

  JPALambdaAllOperation(final JPAEntityFilterProcessor<?> jpaComplier, final Member member) {
    super(jpaComplier, member);
  }

  public Subquery<?> getNotExistsQuery() throws ODataApplicationException {
    try {
      return buildFilterSubQueries(new UnaryImpl(UnaryOperatorKind.NOT, determineLambdaExpression(), null));
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

}
