package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.jpa.processor.core.query.EntityQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.NavigationIfc;
import org.apache.olingo.jpa.processor.impl.JPAStructureProcessor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Subquery;

/**
 * Implementors of this interface may customize queries created by the {@link EntityQueryBuilder} and used by the
 * {@link JPAStructureProcessor}. The customizer comes into effect only with this setup.</br>
 * The customizer instance can be registered in
 * {@link JPAODataServletHandler#modifyRequestContext(org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext)}
 * as injectable dependency:</br>
 * <code>requestContext.getDependencyInjector().registerDependencyMapping(QueryCustomizer.class, &lt;customizer
 * instance&gt;);</code>
 */
public interface QueryCustomizer {

  public interface QueryContext {

    /**
     *
     * @return The currently used entity manager for query handling.
     */
    public EntityManager getEntityManager();

    /**
     * @return The target table of current query.
     */
    public <T> From<T, T> getFrom();

    /**
     * Create subquery for current query targeting the {@link #getFrom() root}.
     */
    public <T> Subquery<T> createSubquery(Class<T> subqueryResultType);
  }

  /**
   * This method is called for every 'root' query and also for every additional query like to resolve $expand
   * relationships, @ElementCollection's etc.
   *
   * @return A additional WHERE clause, attached to scope query with AND operator. Or <code>null</code> to skip clause
   * for current query.
   */
  public Expression<Boolean> restrictQuery(QueryContext context, NavigationIfc queryScope);
}
