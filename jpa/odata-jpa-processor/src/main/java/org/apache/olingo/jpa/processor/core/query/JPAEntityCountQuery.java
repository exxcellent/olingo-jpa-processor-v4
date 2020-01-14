package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

public class JPAEntityCountQuery extends JPAAbstractEntityQuery<CriteriaQuery<Long>, Long> {

  private final CriteriaQuery<Long> cq;
  private final Root<?> root;

  public JPAEntityCountQuery(final EdmEntityType edmEntityType, final JPAODataContext context,
      final UriInfo uriInfo, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(edmEntityType, context, uriInfo, em);
    cq = getCriteriaBuilder().createQuery(Long.class);
    root = cq.from(getQueryScopeType().getTypeClass());
    // now we are ready
    initializeQuery();
  }

  @Override
  public final CriteriaQuery<Long> getQuery() {
    return cq;
  }

  @Override
  public From<?, ?> getQueryScopeFrom() {
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
   * @see JPAEntityQuery#execute(boolean)
   */
  public final EntityCollection execute() throws ODataApplicationException, ODataJPAModelException {
    /*
     * URL example:
     * .../Organizations?$count=true
     * .../Organizations/count
     * .../Organizations('3')/Roles/$count
     */

    final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
    /* final Map<String, From<?, ?>> resultsetAffectingTables = */ createFromClause(orderByNaviAttributes);

    cq.select(getCriteriaBuilder().count(root));

    final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }
    final Long count = getEntityManager().createQuery(cq).getSingleResult();
    final EntityCollection odataEntityCollection = new EntityCollection();
    odataEntityCollection.setCount(Integer.valueOf(count.intValue()));
    return odataEntityCollection;
  }

}
