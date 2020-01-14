package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 * A relationship can affect the resulting (parent) query in two ways:
 * <ul>
 * <li>JOIN with parent's FROM target</li>
 * <li>Produce a Subquery for parent's WHERE with JOINS/correlation and more specific subquery WHERE</li>
 * </ul>
 * A filter will always affect only the parent's WHERE, a navigation will also change the parent SELECT by JOIN's
 *
 * @author Oliver Grande
 * .
 */
public abstract class JPAAbstractRelationshipQuery
extends JPAAbstractQuery<Subquery<Integer>, Integer> {

  private final List<UriParameter> keyPredicates;
  private final JPANavigationPath navigationPath;
  private Subquery<Integer> subQuery = null;
  private final JPAAbstractQuery<?, ?> parentCall;
  private final UriResourcePartTyped navigationResource;

  public <T extends Object> JPAAbstractRelationshipQuery(final IntermediateServiceDocument sd,
      final UriResourcePartTyped navigationResource, final JPANavigationPath navigationPath,
      final JPAAbstractQuery<?, ?> parent, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(sd, navigationResource.getType(), em);
    this.keyPredicates = determineKeyPredicates(navigationResource);
    this.navigationPath = navigationPath;
    this.navigationResource = navigationResource;
    if (navigationPath == null) {
      throw new IllegalArgumentException("selector required");
    }
    this.parentCall = parent;
  }

  @Override
  protected UriResource getQueryScopeUriInfoResource() {
    return navigationResource;
  }

  @Override
  protected JPAEntityType getQueryResultType() {
    // should be the same
    return getQueryScopeType();
  }

  /**
   * The 'scope from' of an subquery must be a FROM of {@link #getParentQuery() parent query} (maybe after JOIN's)!!!
   * @see JPAAbstractQuery#getQueryScopeFrom()
   */
  @Override
  public abstract <T> From<T, T> getQueryScopeFrom();

  protected JPANavigationPath getSelector() {
    return navigationPath;
  }

  protected final JPAAbstractQuery<?, ?> getParentQuery() {
    return parentCall;
  }

  @Override
  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    this.subQuery = parentCall.getQuery().subquery(Integer.class);// we select always '1'
    super.initializeQuery();
  }

  @Override
  public final <T> From<T, T> getQueryResultFrom() {
    return getQueryScopeFrom();
  }

  /**
   *
   * @return The {@link javax.persistence.criteria.Root Root} or
   * {@link javax.persistence.criteria.Join Join} used as primary
   * selection type scope (FROM) of subquery.
   */
  @SuppressWarnings("unchecked")
  protected From<?, ?> createSubqueryCorrelation() {
    final From<?, ?> fromParent = getQueryScopeFrom();
    if (Root.class.isInstance(fromParent)) {
      return subQuery.correlate(Root.class.cast(fromParent));
    } else if (CollectionJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(CollectionJoin.class.cast(fromParent));
    } else if (SetJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(SetJoin.class.cast(fromParent));
    } else if (ListJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(ListJoin.class.cast(fromParent));
    } else if (MapJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(MapJoin.class.cast(fromParent));
    } else if (Join.class.isInstance(fromParent)) {
      return subQuery.correlate(Join.class.cast(fromParent));
    }
    else {
      throw new IllegalStateException("Unexpected '"+From.class.getSimpleName()+"' type: "+fromParent.getClass().getSimpleName());
    }
  }

  @Override
  final public Subquery<Integer> getQuery() {
    assertInitialized();
    return subQuery;
  }

  @SuppressWarnings("unchecked")
  public final <T extends Object> Subquery<T> getSubQueryExists(final Subquery<?> childQuery)
      throws ODataApplicationException {

    assertInitialized();

    try {

      // FIXME?
      // correlated root for the subquery part
      final From<?, ?> queryResultTypeFrom = createSubqueryCorrelation();

      final Expression<Boolean> subqueryWhere = createSubqueryWhereByAssociation();

      Expression<Boolean> whereCondition = extendWhereByKey(queryResultTypeFrom/* getQueryScopeFrom() */, subqueryWhere,
          this.keyPredicates);
      final CriteriaBuilder cb = getCriteriaBuilder();
      final Expression<Boolean> existsCondition;
      if (childQuery != null) {
        existsCondition = cb.exists(childQuery);
      } else {
        existsCondition = null;
      }
      whereCondition = combineAND(whereCondition, existsCondition);

      if (whereCondition != null) {
        subQuery.where(whereCondition);
      }
      //FIXME?
      //      // correlated root for the subquery part
      //      final From<?, ?> queryResultTypeFrom = createSubqueryCorrelation();

      // Warning: EclipseLink will produce an invalid query if we have a 'group by'
      // without SELECT the column in that 'group by', Hibernate is working properly
      final List<Expression<?>> groupByColumns = handleAggregation(subQuery, queryResultTypeFrom);
      if (groupByColumns.isEmpty()) {
        // EXISTS subselect needs only a marker select for existence
        ((Subquery<T>) subQuery).select((Expression<T>) getCriteriaBuilder().literal(Integer.valueOf(1)));
      } else if (groupByColumns.size() == 1) {
        // good case
        ((Subquery<T>) subQuery).select((Expression<T>) groupByColumns.get(0));
      } else {
        // a subquery can select only one column, so we have a problem...
        LOG.log(Level.SEVERE,
            "This subquery is using a 'group by' with multiple columns, but can select only one... take the first one only!");
        ((Subquery<T>) subQuery).select((Expression<T>) groupByColumns.get(0));
      }
      if (whereCondition != null) {
        return (Subquery<T>) subQuery;
      }
      return null;
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e, navigationPath.getAlias());
    }
  }

  /**
   *
   * @return The list of expressions used in 'group by', maybe empty, but not
   *         <code>null</code>.
   */
  abstract protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, From<?, ?> subRoot)
      throws ODataApplicationException, ODataJPAModelException;

  /**
   *
   * @return A WHERE condition or <code>null</code> if no explicit condition is
   *         required (in case of correlated join for example)
   */
  abstract protected Expression<Boolean> createSubqueryWhereByAssociation()
      throws ODataApplicationException, ODataJPAModelException;

  @Override
  final protected Locale getLocale() {
    return parentCall.getLocale();
  }

  @Override
  final JPAODataContext getContext() {
    return parentCall.getContext();
  }
}
