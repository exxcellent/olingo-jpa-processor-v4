package org.apache.olingo.jpa.processor.core.query;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;
import jakarta.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.server.api.ODataApplicationException;

abstract class AbstractSubQueryBuilder extends AbstractQueryBuilder {

  private final Subquery<Integer> subQuery;
  private final FilterContextQueryBuilderIfc parentQueryBuilder;

  public AbstractSubQueryBuilder(final FilterContextQueryBuilderIfc parent) throws ODataApplicationException,
  ODataJPAModelException {
    super(parent.getEntityManager());
    this.parentQueryBuilder = parent;
    this.subQuery = parentQueryBuilder.createSubquery(Integer.class);
  }

  final protected Subquery<Integer> getSubQuery() {
    return subQuery;
  }

  protected final FilterContextQueryBuilderIfc getOwningQueryBuilder() {
    return parentQueryBuilder;
  }

  /**
   *
   * @return The {@link jakarta.persistence.criteria.Root Root} or
   * {@link jakarta.persistence.criteria.Join Join} used as primary
   * selection type scope (FROM) of subquery.
   */
  @SuppressWarnings("unchecked")
  protected final From<?, ?> createSubqueryResultFrom() {
    final From<?, ?> fromParent = parentQueryBuilder.getQueryResultFrom();
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
}
