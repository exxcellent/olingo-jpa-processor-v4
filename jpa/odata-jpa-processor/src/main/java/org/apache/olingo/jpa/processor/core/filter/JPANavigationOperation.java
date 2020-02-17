package org.apache.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.FilterSubQueryBuilder;
import org.apache.olingo.jpa.processor.core.query.JPANavigationPropertyInfo;
import org.apache.olingo.jpa.processor.core.query.FilterContextQueryBuilderIfc;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.DeltaTokenOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.FormatOption;
import org.apache.olingo.server.api.uri.queryoption.IdOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.SkipTokenOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

/**
 * In case the query result shall be filtered on an attribute of navigation target a sub-select will be generated.
 * @author Oliver Grande
 *
 */
class JPANavigationOperation extends JPAExistsOperation implements JPAExpressionOperator<BinaryOperatorKind, Boolean> {

  final BinaryOperatorKind operator;
  final JPAMemberOperator<?> jpaMember;
  final JPALiteralOperand operand;
  private final UriResourceKind aggregationType;

  JPANavigationOperation(final JPAEntityFilterProcessor jpaComplier, final BinaryOperatorKind operator,
      final JPAExpressionElement<?> left, final JPAExpressionElement<?> right) {

    super(jpaComplier);
    this.aggregationType = null;
    this.operator = operator;
    if (left instanceof JPAMemberOperator) {
      jpaMember = (JPAMemberOperator<?>) left;
      operand = (JPALiteralOperand) right;
    } else {
      jpaMember = (JPAMemberOperator<?>) right;
      operand = (JPALiteralOperand) left;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Expression<Boolean> get() throws ODataApplicationException {
    // TODO ??? better to reuse parent behaviour?
    if (aggregationType != null) {
      return (Expression<Boolean>) buildFilterSubQueries().getRoots().toArray()[0];
    }
    return getCriteriaBuilder().exists(buildFilterSubQueries());
  }

  @Override
  Subquery<?> buildFilterSubQueries() throws ODataApplicationException {
    final List<UriResource> allUriResourceParts = new ArrayList<UriResource>(getUriResourceParts());
    allUriResourceParts.addAll(jpaMember.getMember().getResourcePath().getUriResourceParts());

    final IntermediateServiceDocument sd = getIntermediateServiceDocument();
    // 1. Determine all relevant associations
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(sd, allUriResourceParts);
    FilterContextQueryBuilderIfc parent = getQueryBuilder();
    final List<FilterSubQueryBuilder> queryList = new ArrayList<FilterSubQueryBuilder>();

    // 2. Create the queries and roots

    // for (int i = 0; i < naviPathList.size(); i++) {
    final OData odata = getOdata();
    for (int i = naviPathList.size() - 1; i >= 0; i--) {
      final JPANavigationPropertyInfo naviInfo = naviPathList.get(i);
      try {
        FilterSubQueryBuilder query;
        if (i == 0 && aggregationType == null) {
          final JPAFilterExpression expression = new JPAFilterExpression(new SubMember(jpaMember), operand.getODataLiteral(),
              operator);
          query = new FilterSubQueryBuilder(odata, allUriResourceParts, naviInfo.getNavigationUriResource(),
              naviInfo.getNavigationPath(), parent, expression);
        } else {
          query = new FilterSubQueryBuilder(odata, allUriResourceParts, naviInfo.getNavigationUriResource(),
              naviInfo.getNavigationPath(), parent);
        }
        queryList.add(query);
      } catch (final ODataJPAModelException e) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
      parent = queryList.get(queryList.size() - 1);
    }
    // 3. Create select statements
    Subquery<?> childQuery = null;
    for (int i = queryList.size() - 1; i >= 0; i--) {
      childQuery = queryList.get(i).buildSubQuery(childQuery);
    }
    return childQuery;
  }

  @Override
  public BinaryOperatorKind getOperator() {
    return operator;
  }

  private class SubMember implements Member {
    final private JPAMemberOperator<?> parentMember;

    public SubMember(final JPAMemberOperator<?> parentMember) {
      super();
      this.parentMember = parentMember;
    }

    @Override
    public <T> T accept(final ExpressionVisitor<T> visitor) throws ExpressionVisitException, ODataApplicationException {
      return null;
    }

    @Override
    public UriInfoResource getResourcePath() {
      return new SubResource(parentMember);
    }

    @Override
    public EdmType getType() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public EdmType getStartTypeFilter() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isCollection() {
      // TODO Auto-generated method stub
      return false;
    }

  }

  private class SubResource implements UriInfoResource {
    final private JPAMemberOperator<?> parentMember;

    public SubResource(final JPAMemberOperator<?> member) {
      super();
      this.parentMember = member;
    }

    @Override
    public List<CustomQueryOption> getCustomQueryOptions() {
      return null;
    }

    @Override
    public ExpandOption getExpandOption() {
      return null;
    }

    @Override
    public FilterOption getFilterOption() {
      return null;
    }

    @Override
    public FormatOption getFormatOption() {
      return null;
    }

    @Override
    public IdOption getIdOption() {
      return null;
    }

    @Override
    public CountOption getCountOption() {
      return null;
    }

    @Override
    public OrderByOption getOrderByOption() {
      return null;
    }

    @Override
    public SearchOption getSearchOption() {
      return null;
    }

    @Override
    public SelectOption getSelectOption() {
      return null;
    }

    @Override
    public SkipOption getSkipOption() {
      return null;
    }

    @Override
    public SkipTokenOption getSkipTokenOption() {
      return null;
    }

    @Override
    public TopOption getTopOption() {
      return null;
    }

    @Override
    public DeltaTokenOption getDeltaTokenOption() {
      return null;
    }

    @Override
    public List<UriResource> getUriResourceParts() {
      final List<UriResource> result = new ArrayList<UriResource>();
      final List<UriResource> source = parentMember.getMember().getResourcePath().getUriResourceParts();
      for (int i = source.size() - 1; i > 0; i--) {
        if (source.get(i).getKind() == UriResourceKind.navigationProperty || source.get(i)
            .getKind() == UriResourceKind.entitySet) {
          break;
        }
        result.add(0, source.get(i));
      }
      return result;
    }

    @Override
    public String getValueForAlias(final String alias) {
      return null;
    }

    @Override
    public ApplyOption getApplyOption() {
      return null;
    }

  }
}
