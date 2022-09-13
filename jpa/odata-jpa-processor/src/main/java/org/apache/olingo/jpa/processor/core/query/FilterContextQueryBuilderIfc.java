package org.apache.olingo.jpa.processor.core.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;

public interface FilterContextQueryBuilderIfc {

  public JPAODataRequestContext getContext();

  public EntityManager getEntityManager();

  /**
   * Factory method to create a nested subquery for the current (sub)query instance.
   */
  <T> Subquery<T> createSubquery(Class<T> subqueryResultType);

  /**
   *
   * @return The query {@link #getQueryResultFrom() result} entity type (selection from the last joined table).
   */
  public JPAEntityType getQueryResultType();

  /**
   *
   * @return The {@link From target} entity table. Maybe the same as
   * {@link AbstractCriteriaQueryBuilder#getQueryStartFrom()} if no navigation
   * (join) is involved.
   */
  public From<?, ?> getQueryResultFrom();

}
