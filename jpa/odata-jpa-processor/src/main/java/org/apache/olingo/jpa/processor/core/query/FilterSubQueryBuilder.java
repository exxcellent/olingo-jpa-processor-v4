package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAEntityFilterProcessor;
import org.apache.olingo.jpa.processor.core.filter.JPAFilterExpression;
import org.apache.olingo.jpa.processor.core.filter.JPAMemberOperator;
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

public class FilterSubQueryBuilder extends AbstractSubQueryBuilder implements FilterContextQueryBuilderIfc {

  private final JPAEntityFilterProcessor filter;
  private final List<UriParameter> keyPredicates;
  private final JPANavigationPath navigationPath;
  private final JPAEntityType entityType;
  private final EdmType edmType;
  private final From<?, ?> subqueryResultFrom;

  public FilterSubQueryBuilder(final OData odata, final List<UriResource> allUriResourceParts,
      final UriResourcePartTyped navigationResource,
      final JPANavigationPath association, final FilterContextQueryBuilderIfc parent)
          throws ODataApplicationException, ODataJPAModelException {
    this(odata, allUriResourceParts, navigationResource, association, parent, null);
  }

  public FilterSubQueryBuilder(final OData odata, final List<UriResource> allUriResourceParts,
      final UriResourcePartTyped navigationResource,
      final JPANavigationPath association,
      final FilterContextQueryBuilderIfc parent, final VisitableExpression expression)
          throws ODataApplicationException,
          ODataJPAModelException {
    super(parent);
    final IntermediateServiceDocument sd = parent.getContext().getEdmProvider().getServiceDocument();
    if (expression != null) {
      // the target of the navigation is the type context for the filter processor
      this.filter = new JPAEntityFilterProcessor(odata, sd,
          getEntityManager(),
          association.getLeaf().getStructuredType(), parent.getContext().getDatabaseProcessor(), allUriResourceParts,
          expression, this);
    } else {
      this.filter = null;
    }
    this.edmType = navigationResource.getType();
    this.entityType = sd.getEntityType(edmType);
    this.keyPredicates = determineKeyPredicates(navigationResource);
    this.navigationPath = association;
    if (navigationPath == null || navigationPath.getPathElements().isEmpty()) {
      throw new IllegalArgumentException("selector required");
    }

    // 1. correlated root for the subquery part
    final From<?, ?> parentCorrelatedFrom = createSubqueryResultFrom();
    // 2. join in subquery
    subqueryResultFrom = buildJoinPath(parentCorrelatedFrom, navigationPath);
  }

  @Override
  public final <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return getSubQuery().subquery(subqueryResultType);
  }

  @Override
  public final JPAODataRequestContext getContext() {
    return getOwningQueryBuilder().getContext();
  }

  @Override
  public final JPAEntityType getQueryResultType() {
    return entityType;
  }

  @Override
  public final From<?, ?> getQueryResultFrom() {
    return subqueryResultFrom;
  }

  private Expression<Boolean> createSubqueryWhereByFilter()
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
  public final Subquery<Integer> buildSubQuery(final Subquery<?> childQuery)
      throws ODataApplicationException {

    try {

      final Subquery<Integer> subQuery = getSubQuery();

      final Expression<Boolean> subqueryWhere = createSubqueryWhereByFilter();

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
        subQuery.select(getCriteriaBuilder().literal(Integer.valueOf(1)));
      } else if (groupByColumns.size() == 1) {
        // good case
        subQuery.select((Expression<Integer>) groupByColumns.get(0));
      } else {
        // a subquery can select only one column, so we have a problem...
        LOG.log(Level.SEVERE,
            "This subquery is using a 'group by' with multiple columns, but can select only one... take the first one only!");
        subQuery.select((Expression<Integer>) groupByColumns.get(0));
      }
      return subQuery;
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
