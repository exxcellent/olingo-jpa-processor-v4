package org.apache.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAElementCollectionPathImpl;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.FilterContextQueryBuilderIfc;
import org.apache.olingo.jpa.processor.core.query.FilterSubQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;
import org.apache.olingo.server.core.uri.UriInfoImpl;
import org.apache.olingo.server.core.uri.queryoption.expression.MemberImpl;

abstract class AbstractMemberNavigation extends JPAExistsOperation {

  AbstractMemberNavigation(final JPAEntityFilterProcessor<?> jpaComplier) {
    super(jpaComplier);
  }

  abstract protected JPAMemberOperand<?> getNavigatingMember();

  /**
   *
   * @return The new member instance derived from original member but without navigation parts.
   */
  abstract protected VisitableExpression buildResultingExpression(Member attribute);

  private JPANavigationPath determineAssociationPath(final JPAEntityType parentType,
      final UriResourcePartTyped uriresource) throws ODataJPAModelException {
    if (uriresource instanceof UriResourceNavigation) {
      return parentType.getAssociationPath(((UriResourceNavigation) uriresource).getProperty().getName());
    } else {
      // must be an complex/primitive collection with separate table
      final JPASelector selector = parentType.getPath(((UriResourceProperty) uriresource).getProperty().getName());
      return new JPAElementCollectionPathImpl(selector);
    }
  }

  @Override
  final protected Subquery<?> buildFilterSubQueries() throws ODataApplicationException {
    // 1. Determine all relevant associations
    final List<UriResource> memberPath = getNavigatingMember().getMember().getResourcePath().getUriResourceParts();
    final List<UriResourcePartTyped> navs = Util.determineNavigations(memberPath);
    final List<UriResource> propPath = memberPath.subList(navs.size(), memberPath.size());

    // 2. Create the queries and roots
    final OData odata = getOdata();
    FilterContextQueryBuilderIfc parent = getQueryBuilder();
    final List<FilterSubQueryBuilder> queryList = new ArrayList<FilterSubQueryBuilder>();

    FilterSubQueryBuilder query;
    JPAEntityType parentType = getQueryBuilder().getQueryResultType();
    for (int i = 0; i < navs.size(); i++) {
      final UriResourcePartTyped uriresource = navs.get(i);
      try {
        final JPANavigationPath navPath = determineAssociationPath(parentType, uriresource);
        assert navPath != null;
        VisitableExpression expression = null;
        // rebuild the filter expression for the last sub query part
        if (i == navs.size() - 1) {
          // build simple fake member having the remaining simple/complex resource path
          final UriInfoImpl uriInfo = new UriInfoImpl();
          for (final UriResource remaining : propPath) {
            uriInfo.addResourcePart(remaining);
          }
          expression = buildResultingExpression(new MemberImpl(uriInfo, null));
        }
        query = new FilterSubQueryBuilder(odata, Collections.emptyList(), uriresource, navPath, parent, expression);
        queryList.add(query);
        parent = query;
        parentType = query.getQueryResultType();
      } catch (ODataJPAModelException | ODataApplicationException e) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }
    // 3. Create select statements; child queries in reverse order as in creation (to have valid parent/child relation)
    Subquery<?> childQuery = null;
    for (int i = queryList.size() - 1; i >= 0; i--) {
      childQuery = queryList.get(i).buildSubQuery(childQuery);
    }
    return childQuery;
  }
}
