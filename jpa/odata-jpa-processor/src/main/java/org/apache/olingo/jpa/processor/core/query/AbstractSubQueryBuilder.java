package org.apache.olingo.jpa.processor.core.query;

import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.server.api.ODataApplicationException;

abstract class AbstractSubQueryBuilder extends AbstractQueryBuilder {

  private final Subquery<Integer> subQuery;
  private final JPAQueryBuilderIfc parentQueryBuilder;

  public AbstractSubQueryBuilder(final JPAQueryBuilderIfc parent) throws ODataApplicationException,
  ODataJPAModelException {
    super(parent.getEntityManager());
    this.parentQueryBuilder = parent;
    this.subQuery = parentQueryBuilder.createSubquery(Integer.class);
  }

  final protected Subquery<Integer> getSubQuery() {
    return subQuery;
  }

  protected final JPAQueryBuilderIfc getOwningQueryBuilder() {
    return parentQueryBuilder;
  }

  /**
   *
   * @return The {@link javax.persistence.criteria.Root Root} or
   * {@link javax.persistence.criteria.Join Join} used as primary
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
