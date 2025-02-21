package org.apache.olingo.jpa.processor.core.query;

import org.apache.olingo.jpa.processor.core.api.QueryRequestCustomizer.QueryCustomization;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.criteria.Subquery;

public class QueryCustomizerAdapter<DT> implements QueryCustomization {

  private final AbstractCriteriaQueryBuilder<?, DT> caller;
  private final SelectionKind kind;

  public QueryCustomizerAdapter(final AbstractCriteriaQueryBuilder<?, DT> caller) {
    this.caller = caller;
    if(caller instanceof ElementCollectionQueryBuilder || caller instanceof EntityQueryBuilder) {
      this.kind = SelectionKind.COLUMN;
    } else if (caller instanceof EntityAggregationQueryBuilder || caller instanceof EntityCountQueryBuilder) {
      this.kind = SelectionKind.AGGREGATION;      
    } else {
      throw new UnsupportedOperationException("Selection kind cannot be determined from unknown quer builder class "+caller.getClass().getSimpleName());
    }
  }

  @Override
  public EntityManager getEntityManager() {
    return caller.getEntityManager();
  }

  @Override
  public <S> From<S, S> getStartFrom() {
    return caller.getQueryStartFrom();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S> Class<S> getStartTypeClass() {
    return (Class<S>) caller.getQueryStartType().getTypeClass();
  }

  @Override
  public <T> From<T, T> getEndFrom() {
    return caller.getQueryEndFrom();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Class<T> getEndTypeClass() {
    return (Class<T>) caller.getQueryEndType().getTypeClass();
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
      jakarta.persistence.criteria.Expression<Boolean> whereClause = query.getRestriction();
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
  
  @Override
  public SelectionKind getSelectionKind() {
    return kind;
  }
}
