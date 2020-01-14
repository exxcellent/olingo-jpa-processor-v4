package org.apache.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractRelationshipQuery;
import org.apache.olingo.jpa.processor.core.query.JPAFilterQuery;
import org.apache.olingo.jpa.processor.core.query.JPANavigationPropertyInfo;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

abstract class JPALambdaOperation extends JPAExistsOperation {

  protected final UriInfoResource member;

  JPALambdaOperation(final JPAAbstractFilterProcessor jpaComplier, final UriInfoResource member) {
    super(jpaComplier);
    this.member = member;
  }

  public JPALambdaOperation(final JPAAbstractFilterProcessor jpaComplier, final Member member) {
    super(jpaComplier);
    this.member = member.getResourcePath();
  }

  @Override
  protected Subquery<?> buildFilterSubQueries() throws ODataApplicationException {
    try {
      return buildFilterSubQueries(determineExpression());
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  protected final Subquery<?> buildFilterSubQueries(final Expression expression) throws ODataApplicationException,
  ODataJPAModelException {
    final List<UriResource> allUriResourceParts = new ArrayList<UriResource>(getUriResourceParts());
    allUriResourceParts.addAll(member.getUriResourceParts());

    final IntermediateServiceDocument sd = getIntermediateServiceDocument();
    // 1. Determine all relevant associations
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(sd, allUriResourceParts);
    JPAAbstractQuery<?, ?> parent = getOwnerQuery();
    final List<JPAAbstractRelationshipQuery> queryList = new ArrayList<>(naviPathList.size());

    // 2. Create the queries and roots
    final EntityManager em = getEntityManager();
    final OData odata = getOdata();
    for (int i = naviPathList.size() - 1; i >= 0; i--) {
      final JPANavigationPropertyInfo naviInfo = naviPathList.get(i);
      JPAFilterQuery query;
      if (i == 0) {
        query = new JPAFilterQuery(odata, sd, naviInfo.getNavigationUriResource(),
            naviInfo.getNavigationPath(), parent, em, expression);
      } else {
        query = new JPAFilterQuery(odata, sd, naviInfo.getNavigationUriResource(),
            naviInfo.getNavigationPath(), parent, em);
      }
      queryList.add(query);
      parent = queryList.get(queryList.size() - 1);
    }
    // 3. Create select statements
    Subquery<?> childQuery = null;
    for (int i = queryList.size() - 1; i >= 0; i--) {
      childQuery = queryList.get(i).getSubQueryExists(childQuery);
    }
    return childQuery;
  }

  protected Expression determineExpression() {
    for (final UriResource uriResource : member.getUriResourceParts()) {
      if (uriResource.getKind() == UriResourceKind.lambdaAny) {
        return ((UriResourceLambdaAny) uriResource).getExpression();
      } else if (uriResource.getKind() == UriResourceKind.lambdaAll) {
        return ((UriResourceLambdaAll) uriResource).getExpression();
      }
    }
    return null;
  }
}