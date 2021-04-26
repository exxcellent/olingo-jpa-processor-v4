package org.apache.olingo.jpa.processor.core.query.result;

import java.util.Collection;
import java.util.Collections;
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
  private final Collection<String> requestedResultAttributes;

  public QueryEntityResult(final List<Tuple> result, final JPAEntityType jpaEntityType) {
    this(result, null, jpaEntityType);
  }

  /**
   *
   * @param requestedResultAttributes The list of attributes (DB alias) names of all attributes request by client in query
   * (via $select or other method) or <code>null</code> for selection constraint. This list can be used to limit the
   * returned result list to that columns... like for Excel export.
   */
  public QueryEntityResult(final List<Tuple> result, final Collection<String> requestedResultAttributes,
      final JPAEntityType jpaEntityType) {
    super(jpaEntityType);
    assert result != null;
    this.resultValues = result;
    this.requestedResultAttributes = requestedResultAttributes != null ? requestedResultAttributes : Collections
        .emptyList();
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

  /**
   *
   * @return List with requested attribute (DB alias) names or empty for no hint.
   */
  public Collection<String> getRequestedResultAttributes() {
    return requestedResultAttributes;
  }

}
