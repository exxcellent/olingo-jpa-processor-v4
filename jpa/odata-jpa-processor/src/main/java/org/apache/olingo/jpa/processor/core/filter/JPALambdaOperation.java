package org.apache.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.FilterContextQueryBuilderIfc;
import org.apache.olingo.jpa.processor.core.query.FilterSubQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.JPANavigationPropertyInfo;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAll;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

abstract class JPALambdaOperation extends JPAExistsOperation {

  private final Member member;

  public JPALambdaOperation(final JPAEntityFilterProcessor<?> jpaComplier, final Member member) {
    super(jpaComplier);
    this.member = member;
  }

  @Override
  public Expression getQueryExpressionElement() {
    return member;
  }

  @Override
  protected Subquery<?> buildFilterSubQueries() throws ODataApplicationException {
    try {
      return buildFilterSubQueries(determineLambdaExpression());
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }

  // TODO merge with logic with the similar one in JPAMemberOperationNavigation (AbstractMemberNavigation)
  protected final Subquery<?> buildFilterSubQueries(final Expression expression) throws ODataApplicationException,
  ODataJPAModelException {
    final IntermediateServiceDocument sd = getIntermediateServiceDocument();
    // 1. Determine all relevant associations
    final List<UriResource> uriResourceParts = member.getResourcePath().getUriResourceParts();
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(sd, this.getQueryBuilder()
        .getQueryResultType(), uriResourceParts);
    FilterContextQueryBuilderIfc parent = getQueryBuilder();
    final List<FilterSubQueryBuilder> queryList = new ArrayList<>(naviPathList.size());

    // 2. Create the queries and roots
    final OData odata = getOdata();
    for (int i = 0; i < naviPathList.size(); i++) {
      final JPANavigationPropertyInfo naviInfo = naviPathList.get(i);
      FilterSubQueryBuilder query;
      if (i == naviPathList.size() - 1) {
        query = new FilterSubQueryBuilder(odata, uriResourceParts, naviInfo.getNavigationUriResource(),
            naviInfo.getNavigationPath(), parent, expression);
      } else {
        query = new FilterSubQueryBuilder(odata, uriResourceParts, naviInfo.getNavigationUriResource(),
            naviInfo.getNavigationPath(), parent);
      }
      queryList.add(query);
      parent = query;
    }
    // 3. Create select statements
    Subquery<?> childQuery = null;
    for (int i = queryList.size() - 1; i >= 0; i--) {
      childQuery = queryList.get(i).buildSubQuery(childQuery);
    }
    return childQuery;
  }

  protected Expression determineLambdaExpression() {
    for (final UriResource uriResource : member.getResourcePath().getUriResourceParts()) {
      if (uriResource.getKind() == UriResourceKind.lambdaAny) {
        return ((UriResourceLambdaAny) uriResource).getExpression();
      } else if (uriResource.getKind() == UriResourceKind.lambdaAll) {
        return ((UriResourceLambdaAll) uriResource).getExpression();
      }
    }
    return null;
  }
}