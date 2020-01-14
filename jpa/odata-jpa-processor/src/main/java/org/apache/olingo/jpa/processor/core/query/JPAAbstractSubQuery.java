package org.apache.olingo.jpa.processor.core.query;

import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.server.api.ODataApplicationException;

abstract class JPAAbstractSubQuery extends JPAAbstractQuery<Subquery<Integer>, Integer> {

  private final Subquery<Integer> subQuery;
  private final JPAAbstractQuery<?, ?> parentCall;

  public JPAAbstractSubQuery(final IntermediateServiceDocument sd, final EdmType edmType, final EntityManager em,
      final JPAAbstractQuery<?, ?> parent) throws ODataApplicationException, ODataJPAModelException {
    super(sd, edmType, em);
    this.parentCall = parent;
    this.subQuery = parentCall.getQuery().subquery(Integer.class);
  }

  @Override
  final public Subquery<Integer> getQuery() {
    return subQuery;
  }

  @Override
  public final <T> From<T, T> getQueryScopeFrom() {
    // no change so we can take the parent scope
    return parentCall.getQueryScopeFrom();
  }

  @Override
  public JPAEntityType getQueryResultType() {
    // should be the same if no JOIN in FROM part comes up
    return parentCall.getQueryResultType();
  }


  /**
   *
   * @return The {@link javax.persistence.criteria.Root Root} or
   * {@link javax.persistence.criteria.Join Join} used as primary
   * selection type scope (FROM) of subquery.
   */
  @SuppressWarnings("unchecked")
  protected final From<?, ?> createSubqueryResultFrom() {
    // 1. correlate wih parent
    final From<?, ?> fromParent = parentCall.getQueryResultFrom();
    if (Root.class.isInstance(fromParent)) {
      return subQuery.correlate(Root.class.cast(fromParent));
    } else if (CollectionJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(CollectionJoin.class.cast(fromParent));
    } else if (SetJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(SetJoin.class.cast(fromParent));
    } else if (ListJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(ListJoin.class.cast(fromParent));
    } else if (MapJoin.class.isInstance(fromParent)) {
      return subQuery.correlate(MapJoin.class.cast(fromParent));
    } else if (Join.class.isInstance(fromParent)) {
      return subQuery.correlate(Join.class.cast(fromParent));
    } else {
      throw new IllegalStateException("Unexpected '" + From.class.getSimpleName() + "' type: " + fromParent.getClass()
      .getSimpleName());
    }
  }

  @Override
  final protected Locale getLocale() {
    return parentCall.getLocale();
  }

  @Override
  final JPAODataContext getContext() {
    return parentCall.getContext();
  }
}
