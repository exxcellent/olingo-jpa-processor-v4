package org.apache.olingo.jpa.processor.core.query.result;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;

public abstract class AbstractEntityQueryResult {

  private final Map<JPAAssociationPath, ExpandQueryEntityResult> resultRelationshipTargets = new HashMap<>();
  private final Map<JPAAttribute<?>, QueryElementCollectionResult> resultElementCollectionTargets = new HashMap<>();
  private final JPAEntityType jpaEntityType;

  protected AbstractEntityQueryResult(final JPAEntityType jpaEntityType) {
    super();
    assert jpaEntityType != null;
    this.jpaEntityType = jpaEntityType;
  }

  public final void putExpandResults(final Map<JPAAssociationPath, ExpandQueryEntityResult> childResults)
      throws ODataApplicationException {
    // check already present entries
    for (final JPAAssociationPath child : childResults.keySet()) {
      if (resultRelationshipTargets.get(child) != null) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_EXPAND_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
    }
    resultRelationshipTargets.putAll(childResults);
  }

  public final void putElementCollectionResults(
      final Map<JPAAttribute<?>, QueryElementCollectionResult> collectionResults)
          throws ODataApplicationException {
    // check already present entries
    for (final Entry<JPAAttribute<?>, QueryElementCollectionResult> entry : collectionResults.entrySet()) {
      if (resultElementCollectionTargets.containsKey(entry.getKey())) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
    }
    resultElementCollectionTargets.putAll(collectionResults);
  }

  public final Map<JPAAssociationPath, ExpandQueryEntityResult> getExpandChildren() {
    return resultRelationshipTargets;
  }

  public final Map<JPAAttribute<?>, QueryElementCollectionResult> getElementCollections() {
    return resultElementCollectionTargets;
  }

  public final JPAEntityType getEntityType() {
    return jpaEntityType;
  }

  static Map<String, List<Tuple>> convertResult(final List<Tuple> expandResult,
      final NavigationKeyBuilder keyBuilder) {

    List<Tuple> subResult;
    final Map<String, List<Tuple>> convertedResult = new HashMap<String, List<Tuple>>();
    for (final Tuple row : expandResult) {
      // build key using the key columns from owning entity to assign to that entity instances
      final String actualKey = keyBuilder.buildKeyForNavigationTargetRow(row);
      subResult = convertedResult.get(actualKey);
      if (subResult == null) {
        subResult = new LinkedList<Tuple>();
        convertedResult.put(actualKey, subResult);
      }
      subResult.add(row);
    }
    return convertedResult;
  }

}
