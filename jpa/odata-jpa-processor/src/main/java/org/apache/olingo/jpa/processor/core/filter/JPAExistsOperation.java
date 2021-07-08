package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.FilterContextQueryBuilderIfc;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;

abstract class JPAExistsOperation implements JPAExpression<Boolean> {

  private final List<UriResource> uriResourceParts;
  private final FilterContextQueryBuilderIfc queryBuilder;
  private final IntermediateServiceDocument sd;
  private final EntityManager em;
  private final OData odata;

  JPAExistsOperation(final JPAEntityFilterProcessor<?> jpaComplier) {

    this.uriResourceParts = jpaComplier.getUriResourceParts();
    this.queryBuilder = jpaComplier.getParent();
    this.sd = jpaComplier.getSd();
    this.em = jpaComplier.getEntityManager();
    this.odata = jpaComplier.getOdata();
  }

  protected final List<UriResource> getUriResourceParts() {
    return uriResourceParts;
  }

  protected final FilterContextQueryBuilderIfc getQueryBuilder() {
    return queryBuilder;
  }

  protected final CriteriaBuilder getCriteriaBuilder() {
    return queryBuilder.getEntityManager().getCriteriaBuilder();
  }

  protected final IntermediateServiceDocument getIntermediateServiceDocument() {
    return sd;
  }

  protected final OData getOdata() {
    return odata;
  }

  @Override
  public Expression<Boolean> get() throws ODataApplicationException {
    final Subquery<?> subquery = buildFilterSubQueries();
    if (subquery == null) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    return em.getCriteriaBuilder().exists(subquery);
  }

  abstract Subquery<?> buildFilterSubQueries() throws ODataApplicationException;

}