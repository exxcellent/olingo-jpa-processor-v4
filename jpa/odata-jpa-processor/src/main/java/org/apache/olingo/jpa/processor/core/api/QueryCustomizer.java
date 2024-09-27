package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.jpa.processor.core.query.EntityQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.NavigationIfc;
import org.apache.olingo.jpa.processor.impl.JPAStructureProcessor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Selection;
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

  /**
   * Adapter representing the query to customize.
   */
  public interface QueryCustomization {

    /**
     *
     * @return The currently used entity manager for query handling.
     */
    public EntityManager getEntityManager();

    /**
     * @return The start (before any navigation) table of current query.
     */
    public <S> From<S, S> getStartFrom();

    /**
     * @return The start (before any navigation) model class of current query (entity represented by
     * {@link #getStartFrom()}).
     */
    public <S> Class<S> getStartTypeClass();

    /**
     * @return The target (after navigation) table of current query. Without navigation this is the same as
     * {@link #getStartFrom()}.
     */
    public <T> From<T, T> getEndFrom();

    /**
     * @return The target (after navigation) model class of current query (entity represented by {@link #getEndFrom()}).
     * Without navigation this is the same as {@link #getStartTypeClass()}.
     */
    public <T> Class<T> getEndTypeClass();

    /**
     * Create subquery for current query targeting the {@link #getEndFrom() root}.
     */
    public <Q> Subquery<Q> createSubquery(Class<Q> subqueryResultType);

    /**
     * Add one or more expression to the existing WHERE conditions with AND operator.
     */
    public <E extends Expression<Boolean>> void withWhereClause(@SuppressWarnings("unchecked") E... expressions);

    /**
     * Set a grouping into the query. That's only possible if not an grouping is already present. A grouping will be
     * present if the OData query is forcing such one via $orderby parameter.
     *
     * @throws IllegalStateException If a grouping is already present (check via {@link #hasAlreadyGroupBy()} before
     * trying).
     *
     * @see jakarta.persistence.criteria.CriteriaQuery#groupBy(Expression...)
     * @see #withOrderBy(Order...)
     */
    public void withGroupBy(Expression<?>... grouping) throws IllegalStateException;

    /**
     * @return TRUE if query has already a grouping
     */
    public boolean hasAlreadyGroupBy();

    /**
     * Set a sorting into the query. That's only possible if not an sorting is already present. A sorting will be
     * present if the OData query is forcing such one via $orderby parameter.
     *
     * @throws IllegalStateException If a sorting is already present (check via {@link #hasAlreadyOrderBy()} before
     * trying).
     *
     * @see jakarta.persistence.criteria.CriteriaQuery#orderBy(Order...)
     * @see #withGroupBy(Expression...)
     */
    public void withOrderBy(Order... sortings) throws IllegalStateException;

    /**
     * @return TRUE if query has already a sorting
     */
    public boolean hasAlreadyOrderBy();

    /**
     *
     * @return The currently defined selection of columns for result set, maybe <code>null</code>.
     */
    public <T> Selection<T> getSelection();
  }

  /**
   * This method is called for every 'root' query and also for every additional query like to resolve $expand
   * relationships, @ElementCollection's etc. The type of query can differ:
   * <ul>
   * <li>entity -> tuple</li>
   * <li>count -> number</li>
   * <li>element collection (attribute) -> tuple</li>
   * <li>aggregation -> tuple</li>
   * </ul>
   *
   * Be aware: the JPA query is prepared based on data in the OData query (expression). So it's very important to check
   * the conditions when to modify. Some hints:</br>
   * <ul>
   * <li>Normally only additional WHERE conditions should be added (maybe as sub query) to implement stronger
   * restrictions what data should be exposed (example: filter for something being in a critical state)</li>
   * <li>If the OData query is forcing a sorting ($orderBy), also the grouping in the JPA query is prepared. An existing
   * grouping cannot be replaced by the customizer.</li>
   * <li>Sorting and grouping come often together.</li>
   * <li>Navigation in the OData query is handled as SQL 'Join' and that join will be used as 'from' in
   * {@link QueryCustomization#getEndFrom()}. So the customizer has to differ between
   * {@link QueryCustomization#getStartFrom()} and
   * {@link QueryCustomization#getEndFrom()}. Navigation steps in between are not available for customization.</li>
   * <li>The customization should affect all queries related to the use case, because the customizer is called also for
   * the relationships, expands, collections.They are loaded in separate queries but their results should be limited
   * also by the customization part.</li>
   * </ul>
   *
   * @param context The API helper as query covering adapter to customize the query
   */
  public void customizeQuery(QueryCustomization context, NavigationIfc queryScope);
}
