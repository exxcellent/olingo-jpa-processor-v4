package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;

abstract class JPAExistsOperation implements JPAExpression<Expression<Boolean>> {

  private final List<UriResource> uriResourceParts;
  private final JPAAbstractQuery<?, ?> root;
  private final IntermediateServiceDocument sd;
  private final EntityManager em;
  private final OData odata;

  JPAExistsOperation(final JPAAbstractFilterProcessor jpaComplier) {

    this.uriResourceParts = jpaComplier.getUriResourceParts();
    this.root = jpaComplier.getParent();
    this.sd = jpaComplier.getSd();
    this.em = jpaComplier.getEntityManager();
    this.odata = jpaComplier.getOdata();
  }

  protected final List<UriResource> getUriResourceParts() {
    return uriResourceParts;
  }

  protected final JPAAbstractQuery<?, ?> getOwnerQuery() {
    return root;
  }

  protected final EntityManager getEntityManager() {
    return em;
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