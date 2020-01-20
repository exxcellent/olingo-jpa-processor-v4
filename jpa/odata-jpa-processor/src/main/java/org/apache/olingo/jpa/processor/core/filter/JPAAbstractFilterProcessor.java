package org.apache.olingo.jpa.processor.core.filter;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

abstract class JPAAbstractFilterProcessor {
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
  public abstract Expression<Boolean> compile() throws ExpressionVisitException, ODataApplicationException;

  protected abstract List<UriResource> getUriResourceParts();

  protected abstract IntermediateServiceDocument getSd();

  protected abstract OData getOdata();

  protected abstract EntityManager getEntityManager();

  protected abstract JPAODataDatabaseProcessor getConverter();

  /**
   * Returns a list of all filter elements of type Member. This could be used e.g.
   * to determine if a join is required
   */
  public final List<JPASelector> getMember() {
    final JPAMemberVisitor visitor = new JPAMemberVisitor(jpaEntityType);
    if (expression != null) {
      try {
        expression.accept(visitor);
      } catch (final ExpressionVisitException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (final ODataApplicationException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return Collections.unmodifiableList(visitor.get());
    } else {
      return Collections.emptyList();
    }
  }

}