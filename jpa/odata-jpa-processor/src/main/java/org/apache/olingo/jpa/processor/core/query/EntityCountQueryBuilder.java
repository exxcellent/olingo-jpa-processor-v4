package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

/**
 * <pre>
 * URL example:
 *
 * .../Organizations/$count
 * .../Organizations('3')/Roles/$count
 *
 * This is NOT covered:
 * .../Organizations?$count=true
 * This example is wrong because the entity collection self is loaded, but the count added to response, so the
 * {@link EntityQueryBuilder} will handle that
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
  public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return cq.subquery(subqueryResultType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public From<?, ?> getQueryStartFrom() {
    return root;
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

    // HANA does not work as expected on calculation views -> count only has the expected result if COUNT(*) or
    // COUNT(<with all distinct columns>) is used, but both is not possible with JPA 2.2
    final From<?, ?> targetFrom = getQueryResultFrom();
    cq.select(getCriteriaBuilder().count(targetFrom));

    final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }
    final Long count = getEntityManager().createQuery(cq).getSingleResult();
    return count.longValue();
  }

}
