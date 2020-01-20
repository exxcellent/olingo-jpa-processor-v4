package org.apache.olingo.jpa.processor.core.query.result;

import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;

/**
 * Builds a hierarchy of expand results. One instance contains on the on hand of the result itself, a map which has the
 * join columns values of the parent as its key and on the other hand a map that point the results of the next expand.
 * The join columns are concatenated in the order they are stored in the corresponding Association Path.
 * @author Oliver Grande
 *
 */
public final class QueryEntityResult extends AbstractEntityQueryResult {

  private final List<Tuple> resultValues;

  public QueryEntityResult(final List<Tuple> result, final JPAEntityType jpaEntityType) {
    super(jpaEntityType);
    assert result != null;
    this.resultValues = result;
  }

  /**
   * The {@linkplain #getResultNavigationKeyPath()} is used to order all tuples by the key build with that builder.
   *
   * @see NavigationKeyBuilder#buildKeyForNavigationTargetRow(Tuple)
   * @see getResultNavigationKeyPath()
   */
  public List<Tuple> getQueryResult() {
    return resultValues;
  }

}
