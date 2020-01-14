package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAFilterExpression;
import org.apache.olingo.jpa.processor.core.filter.JPAMemberOperator;
import org.apache.olingo.jpa.processor.core.filter.JPANavigationFilterProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

public class JPAFilterNavigationSubQuery extends JPAAbstractSubQuery {

  private final JPANavigationFilterProcessor filter;
  private final List<UriParameter> keyPredicates;
  private final JPANavigationPath navigationPath;
  private From<?, ?> subqueryResultFrom = null;

  public JPAFilterNavigationSubQuery(final OData odata, final IntermediateServiceDocument sd,
      final UriResourcePartTyped navigationResource, final JPANavigationPath association,
      final JPAAbstractQuery<?, ?> parent, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    this(odata, sd, navigationResource, association, parent, em, null);
  }

  public JPAFilterNavigationSubQuery(final OData odata, final IntermediateServiceDocument sd,
      final UriResourcePartTyped navigationResource, final JPANavigationPath association,
      final JPAAbstractQuery<?, ?> parent, final EntityManager em,
      final VisitableExpression expression) throws ODataApplicationException, ODataJPAModelException {
    super(sd, navigationResource.getType(), em, parent);
    if (expression != null) {
      // the target of the navigation is the type context for the filter processor
      this.filter = new JPANavigationFilterProcessor(odata, sd, em,
          association.getLeaf().getStructuredType() /* getQueryResultType() */,
          parent.getContext().getDatabaseProcessor(), null, this, expression);
    } else {
      this.filter = null;
    }
    this.keyPredicates = determineKeyPredicates(navigationResource);
    this.navigationPath = association;
    if (navigationPath == null) {
      throw new IllegalArgumentException("selector required");
    }

    // context should be already prepared for filter queries
    initializeQuery();
  }

  @Override
  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    // 1. correlated root for the subquery part
    final From<?, ?> parentCorrelatedFrom = createSubqueryResultFrom();
    // 2. join in subquery
    From<?, ?> subFrom = parentCorrelatedFrom;
    for (final JPAAttribute<?> a : navigationPath.getPathElements()) {
      subFrom = subFrom.join(a.getInternalName());
    }
    subqueryResultFrom = subFrom;
    super.initializeQuery();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> From<T, T> getQueryResultFrom() {
    return (From<T, T>) subqueryResultFrom;
  }

  private Expression<Boolean> createSubqueryWhereByAssociation()
      throws ODataApplicationException, ODataJPAModelException {
    if (filter != null && getAggregationType(this.filter.getExpression()) == null) {
      try {
        if (filter.getExpression() != null) {
          return filter.compile();
        }
      } catch (final ExpressionVisitException e) {
        throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  public final <T extends Object> Subquery<T> buildSubQuery(final Subquery<?> childQuery)
      throws ODataApplicationException {

    assertInitialized();

    try {

      final Subquery<Integer> subQuery = getQuery();

      final Expression<Boolean> subqueryWhere = createSubqueryWhereByAssociation();

      Expression<Boolean> whereCondition = extendWhereByKey(subqueryResultFrom, getQueryResultType(),
          this.keyPredicates);
      whereCondition = combineAND(whereCondition, subqueryWhere);
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

      // Warning: EclipseLink will produce an invalid query if we have a 'group by'
      // without SELECT the column in that 'group by', Hibernate is working properly
      final List<Expression<?>> groupByColumns = handleAggregation(subQuery, subqueryResultFrom);
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
      return (Subquery<T>) subQuery;
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e, navigationPath.getAlias());
    }
  }

  protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, final From<?, ?> subRoot)
      throws ODataApplicationException, ODataJPAModelException {

    if (filter == null) {
      return Collections.emptyList();
    }
    if (getAggregationType(this.filter.getExpression()) == null) {
      return Collections.emptyList();
    }
    final List<Expression<?>> groupByLIst = new LinkedList<>();
    // TODO avoid cast to JPAAssociationPath
    final List<JPASelector> navigationSourceSelectors = ((JPAAssociationPath) navigationPath).getRightPaths();
    for (int index = 0; index < navigationSourceSelectors.size(); index++) {
      Path<?> subPath = subRoot;
      final JPASelector sourceSelector = navigationSourceSelectors.get(index);
      for (final JPAElement jpaPathElement : sourceSelector.getPathElements()) {
        subPath = subPath.get(jpaPathElement.getInternalName());
      }
      groupByLIst.add(subPath);
    }
    subQuery.groupBy(groupByLIst);

    try {
      subQuery.having(this.filter.compile());
    } catch (final ExpressionVisitException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    return groupByLIst;

  }

  @SuppressWarnings("rawtypes")
  private UriResourceKind getAggregationType(final VisitableExpression expression) {
    UriInfoResource member = null;
    if (expression != null && expression instanceof Binary) {
      if (((Binary) expression).getLeftOperand() instanceof JPAMemberOperator) {
        member = ((JPAMemberOperator) ((Binary) expression).getLeftOperand()).getMember().getResourcePath();
      } else if (((Binary) expression).getRightOperand() instanceof JPAMemberOperator) {
        member = ((JPAMemberOperator) ((Binary) expression).getRightOperand()).getMember().getResourcePath();
      }
    } else if (expression != null && expression instanceof JPAFilterExpression) {
      member = ((JPAFilterExpression) expression).getMember();
    }

    if (member != null) {
      for (final UriResource r : member.getUriResourceParts()) {
        if (r.getKind() == UriResourceKind.count) {
          return r.getKind();
        }
      }
    }
    return null;
  }
}
