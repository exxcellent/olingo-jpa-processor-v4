package org.apache.olingo.jpa.processor.core.query;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.processor.core.api.QueryCustomizer.QueryCustomization;

public class QueryCustomizerAdapter<DT> implements QueryCustomization {

  private final AbstractCriteriaQueryBuilder<?, DT> caller;

  public QueryCustomizerAdapter(final AbstractCriteriaQueryBuilder<?, DT> caller) {
    this.caller = caller;
  }

  @Override
  public EntityManager getEntityManager() {
    return caller.getEntityManager();
  }

  @Override
  public <S> From<S, S> getStartFrom() {
    return caller.getQueryStartFrom();
  }

  @Override
  public <S> Class<S> getStartTypeClass() {
    return caller.getQueryStartType().getTypeClass();
  }

  @Override
  public <T> From<T, T> getEndFrom() {
    return caller.getQueryEndFrom();
  }

  @Override
  public <T> Class<T> getEndTypeClass() {
    return caller.getQueryEndType().getTypeClass();
  }

  @Override
  public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return caller.createSubquery(subqueryResultType);
  }

  @Override
  public <E extends Expression<Boolean>> void withWhereClause(@SuppressWarnings("unchecked") final E... expressions) {
    if (expressions == null || expressions.length < 1) {
      return;
    }
    final CriteriaQuery<DT> query = caller.getQuery();
    for (final E expression : expressions) {
      javax.persistence.criteria.Expression<Boolean> whereClause = query.getRestriction();
      whereClause = caller.combineAND(whereClause, expression);
      if (whereClause != null) {
        query.where(whereClause);
      }
    }
  }

  @Override
  public void withGroupBy(final Expression<?>... grouping) throws IllegalStateException {
    if (hasAlreadyGroupBy()) {
      throw new IllegalStateException("There is already a grouping given");
    }
    final CriteriaQuery<DT> query = caller.getQuery();
    query.groupBy(grouping);
  }

  @Override
  public boolean hasAlreadyGroupBy() {
    return caller.getQuery().getGroupList() != null && !caller.getQuery().getGroupList().isEmpty();
  }

  @Override
  public void withOrderBy(final Order... sortings) throws IllegalStateException {
    if (hasAlreadyOrderBy()) {
      throw new IllegalStateException("There is already a sorting given");
    }
    final CriteriaQuery<DT> query = caller.getQuery();
    query.orderBy(sortings);
  }

  @Override
  public boolean hasAlreadyOrderBy() {
    return caller.getQuery().getOrderList() != null && !caller.getQuery().getOrderList().isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Selection<DT> getSelection() {
    return caller.getQuery().getSelection();
  }
}
