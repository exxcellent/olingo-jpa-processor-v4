package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryElementCollectionResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;

class JPAElementCollectionQuery extends JPAAbstractEntityQuery<CriteriaQuery<Tuple>, Tuple> {
  private final CriteriaQuery<Tuple> cq;
  private final Root<?> root;
  private final JPAAttribute<?> attribute;
  private final List<JPASelector> paths;

  JPAElementCollectionQuery(final EdmType edmType,
      final JPAAttribute<?> attribute, final List<JPASelector> paths,
      final JPAODataContext context,
      final UriInfoResource uriInfo, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    //    super(context, edmType, em, uriInfo);
    super(null, context, uriInfo, em);
    cq = getCriteriaBuilder().createTupleQuery();

    root = cq.from(determineStartingEntityType().getTypeClass());

    //    root = cq.from(getQueryScopeType().getTypeClass());
    this.attribute = attribute;
    if (!attribute.isCollection() || attribute.getAttributeMapping() == AttributeMapping.RELATIONSHIP) {
      throw new IllegalStateException("The element is not a @ElementCollection, bug!");
    }
    this.paths = paths;
    // now we are ready
    initializeQuery();
  }

  @Override
  public CriteriaQuery<Tuple> getQuery() {
    return cq;
  }

  @Override
  public From<?, ?> getQueryScopeFrom() {
    return root;
  }

  //  @Override
  //  protected JPAEntityType getQueryResultType() {
  //    throw new IllegalStateException("not allowed");
  //  }

  public JPAQueryElementCollectionResult execute() throws ODataApplicationException {
    LOG.log(Level.FINE, "Process element collection for: " + getQueryScopeType().getExternalName() + "#" + attribute
        .getExternalName() + (attribute.getStructuredType() != null ? " (" + attribute.getStructuredType()
        .getExternalName() + ")" : ""));
    try {
      // this will cause a Join...
      final List<Selection<?>> joinSelections = createSelectClause(paths);

      // add the key columns to selection, to build the key to map results for element
      // collection entries to owning entity
      // TODO don't select path's multiple times
      //      joinSelections = extendSelectionWithEntityKeys(joinSelections);
      final List<? extends JPAAttribute<?>> jpaKeyList = getQueryResultType().getKeyAttributes(true);
      final List<JPASelector> listKeyPaths = new ArrayList<JPASelector>(jpaKeyList.size());
      for (final JPAAttribute<?> key : jpaKeyList) {
        final JPASelector keyPath = getQueryResultType().getPath(key.getExternalName());
        listKeyPaths.add(keyPath);
      }
      joinSelections.addAll(createSelectClause(listKeyPaths));

      cq.multiselect(joinSelections);
      final Expression<Boolean> where = createWhere();
      if (where != null) {
        cq.where(where);
      }
      final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
      // FIXME how to add TOP or SKIP for elements of another table? (do not work as
      // in JPAExpandQuery, because we have to avoid loading of too much rows)
      final List<Tuple> intermediateResult = tq.getResultList();
      final Map<String, List<Tuple>> result = convertResult(intermediateResult, listKeyPaths);
      return new JPAQueryElementCollectionResult(result, listKeyPaths);
    } catch (final ODataJPAModelException e) {
      throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
          Locale.ENGLISH, e);
    }
  }

  private Map<String, List<Tuple>> convertResult(final List<Tuple> intermediateResult,
      final List<JPASelector> listKeyPath)
          throws ODataApplicationException {

    List<Tuple> subResult;
    String actualKey;
    final Map<String, List<Tuple>> convertedResult = new HashMap<String, List<Tuple>>();
    for (final Tuple row : intermediateResult) {
      // build key using the key columns from owning entity to assign to that entity
      // instances
      actualKey = buildTargetResultKey(row, listKeyPath);
      subResult = convertedResult.get(actualKey);
      if (subResult == null) {
        subResult = new LinkedList<Tuple>();
        convertedResult.put(actualKey, subResult);
      }
      subResult.add(row);
    }
    return convertedResult;
  }

  private String buildTargetResultKey(final Tuple row, final List<JPASelector> joinColumns) {
    final StringBuffer buffer = new StringBuffer();
    for (final JPASelector item : joinColumns) {
      buffer.append(JPASelector.PATH_SEPERATOR);
      buffer.append(row.get(item.getAlias()));
    }
    buffer.deleteCharAt(0);
    return buffer.toString();
  }

}
