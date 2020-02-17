package org.apache.olingo.jpa.processor.core.query.result;

import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.processor.core.query.EntityQueryBuilder;
import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Result class used to convert a {@link QueryEntityResult direct result} from {@link EntityQueryBuilder expand query}
 * into a structure representing the expand relationship relative to an owning parent query result of type
 * {@link QueryEntityResult}.
 */
public final class ExpandQueryEntityResult extends AbstractEntityQueryResult {

  private final JPAAssociationPath navigationPath;
  private final Map<String, List<Tuple>> resultValues;
  private final NavigationKeyBuilder resultNavigationKeyBuilder;

  public ExpandQueryEntityResult(final JPAAssociationPath navigationPath, final QueryEntityResult expandResult,
      final NavigationKeyBuilder owningEntityKeyBuilder)
          throws ODataApplicationException {
    super(expandResult.getEntityType());
    this.navigationPath = navigationPath;
    this.resultValues = convertResult(expandResult.getQueryResult(), owningEntityKeyBuilder);
    putExpandResults(expandResult.getExpandChildren());
    putElementCollectionResults(expandResult.getElementCollections());
    resultNavigationKeyBuilder = owningEntityKeyBuilder;
  }

  public final NavigationKeyBuilder getNavigationKeyBuilder() {
    return resultNavigationKeyBuilder;
  }

  /**
   * The {@linkplain #getResultNavigationKeyPath()} is used to order all tuples by the key build with that builder.
   *
   * @see NavigationKeyBuilder#buildKeyForNavigationTargetRow(Tuple)
   * @see getResultNavigationKeyPath()
   */
  public List<Tuple> getDirectMappingsResult(final String owningEntityKey) {
    return resultValues.get(owningEntityKey);
  }

  /**
   *
   * @return The navigation path that the expand result represents.
   */
  public JPAAssociationPath getNavigationPath() {
    return navigationPath;
  }

}
