package org.apache.olingo.jpa.processor.core.query.result;

import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

/**
 * @author Ralf Zozmann
 *
 */
public final class QueryElementCollectionResult {

  private final Map<String, List<Tuple>> resultValues;
  private final NavigationKeyBuilder resultNavigationKeyBuilder;

  /**
   *
   * @param result The key in the map is the 'derived identifier' for the entity
   *               where the result tuple list belongs to.
   */
  public QueryElementCollectionResult(final List<Tuple> result, final NavigationKeyBuilder keyBuilder) {
    super();
    assert result != null;
    this.resultValues = AbstractEntityQueryResult.convertResult(result, keyBuilder);
    resultNavigationKeyBuilder = keyBuilder;
  }

  public final NavigationKeyBuilder getNavigationKeyBuilder() {
    return resultNavigationKeyBuilder;
  }

  public List<Tuple> getDirectMappingsResult(final String owningEntityKey) {
    return resultValues.get(owningEntityKey);
  }

}
