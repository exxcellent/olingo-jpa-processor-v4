package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.query.FilterContextQueryBuilderIfc;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

abstract class JPAAbstractFilterProcessor<T> {
  private final JPAStructuredType jpaEntityType;
  private final VisitableExpression expression;

  protected JPAAbstractFilterProcessor(final JPAStructuredType jpaEntityType, final VisitableExpression expression) {
    super();
    this.jpaEntityType = jpaEntityType;
    this.expression = expression;
  }

  public VisitableExpression getExpression() {
    return expression;
  }

  public final JPAStructuredType getJpaEntityType() {
    return jpaEntityType;
  }

  /**
   * Parse the filter query into a JPA criteria API expression.
   *
   * @return A composite expression representing the filter query part (WHERE
   *         clause).
   */
  public abstract Expression<T> compile() throws ExpressionVisitException, ODataApplicationException;

  protected abstract List<UriResource> getUriResourceParts();

  protected abstract IntermediateServiceDocument getSd();

  protected abstract OData getOdata();

  protected abstract EntityManager getEntityManager();

  protected abstract JPAODataDatabaseProcessor getConverter();

  protected abstract FilterContextQueryBuilderIfc getParent();

}