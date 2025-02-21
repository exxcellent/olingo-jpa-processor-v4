package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * <pre>
 * URL example:
 *
 * .../Organizations/$count
 * .../Organizations('3')/Roles/$count
 *
 * This is NOT covered (automatically):
 * .../Organizations?$count=true
 * In that case the {@link EntityQueryBuilder} will call this builder to process $count.
 * </pre>
 */
public class EntityCountQueryBuilder extends AbstractCriteriaQueryBuilder<CriteriaQuery<Long>, Long> {

  private final CriteriaQuery<Long> cq;
  private final Root<?> root;

  public EntityCountQueryBuilder(final JPAODataRequestContext context, final NavigationIfc uriInfo,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(context, uriInfo, em);
    cq = getCriteriaBuilder().createQuery(Long.class);
    root = cq.from(getQueryStartType().getTypeClass());
    // now we are ready
    initializeQuery();
  }

  @Override
  protected <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return cq.subquery(subqueryResultType);
  }

  @Override
  protected CriteriaQuery<Long> getQuery() {
    return cq;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S> From<S, S> getQueryStartFrom() {
    return (From<S, S>) root;
  }

  /**
   * Counts the number of results to be expected by a query. The method shall
   * fulfill the requirements of the $count query option. This is defined as
   * follows:
   * <p>
   * <i>The $count system query option ignores any $top, $skip, or $expand query
   * options, and returns the total count of results across all pages including
   * only those results matching any specified $filter and $search.</i>
   * <p>
   * For details see: <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398308"
   * >OData Version 4.0 Part 1 - 11.2.5.5 System Query Option $count</a>
   *
   * @return Number of results wrapped into an empty entity collection
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   * @throws ExpressionVisitException
   * @see EntityQueryBuilder#execute(boolean)
   */
  public final long execute() throws ODataApplicationException, ODataJPAModelException {

    final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
    /* final Map<String, From<?, ?>> resultsetAffectingTables = */ createFromClause(orderByNaviAttributes);

    final From<?, ?> targetFrom = getQueryEndFrom();
    //also for count queries we may have joins that will affect the number of result row, with COUNT(DISTINCT ...)
    //on the target table we can limit the result to the distinct key rows of target table
    cq.select(getCriteriaBuilder().countDistinct(targetFrom));

    final jakarta.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }

    involveQueryCustomizer();// as last before querying

    final Long count = getEntityManager().createQuery(cq).getSingleResult();
    return count.longValue();
  }

}
