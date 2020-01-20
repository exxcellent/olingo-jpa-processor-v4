package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.query.result.QueryElementCollectionResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.core.uri.UriResourceComplexPropertyImpl;
import org.apache.olingo.server.core.uri.UriResourcePrimitivePropertyImpl;

class ElementCollectionQueryBuilder extends AbstractCriteriaQueryBuilder<CriteriaQuery<Tuple>, Tuple> {
  private final CriteriaQuery<Tuple> cq;
  private final Root<?> root;
  private final JPAAttribute<?> attribute;
  private final List<JPASelector> paths;

  /**
   *
   * @param attribute
   * @param paths
   * @param context
   * @param uriInfo The resource path or parent entity! Must not lead to the target property/type, because sometimes
   * only one column (of simple type) is selected for @ElementCollection
   * @param em
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   */
  ElementCollectionQueryBuilder(final EdmStructuredType owningType,
      final JPAAttribute<?> attribute, final List<JPASelector> paths,
      final JPAODataContext context,
      final UriInfoResource uriInfo, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(context, createPropertyUriResourcePath(owningType, attribute, uriInfo), em);
    this.cq = getCriteriaBuilder().createTupleQuery();
    this.root = cq.from(getQueryStartType().getTypeClass());
    this.attribute = attribute;
    if (!attribute.isCollection() || attribute.getAttributeMapping() == AttributeMapping.RELATIONSHIP) {
      throw new IllegalStateException("The element is not a @ElementCollection, bug!");
    }
    this.paths = paths;
    // now we are ready
    initializeQuery();
  }

  /**
   * Create a special uri info resource to produce a navigation manageable by the super class and including the required
   * JOIN's.
   */
  private static UriInfoResource createPropertyUriResourcePath(final EdmStructuredType owningType,
      final JPAAttribute<?> attribute, final UriInfoResource uriInfoParent) {
    final EdmProperty edmProperty = (EdmProperty) owningType.getProperty(attribute.getExternalName());
    UriResourceProperty resourceProperty;
    if (attribute.isComplex()) {
      // complex
      resourceProperty = new UriResourceComplexPropertyImpl(edmProperty);
    } else {
      // primitive
      resourceProperty = new UriResourcePrimitivePropertyImpl(edmProperty);
    }
    return new NavigationUriResource(uriInfoParent, resourceProperty);

  }

  @Override
  public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return cq.subquery(subqueryResultType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public From<?, ?> getQueryStartFrom() {
    return root;
  }

  public QueryElementCollectionResult execute() throws ODataApplicationException {
    LOG.log(Level.FINE, "Process element collection for: " + getQueryResultNavigationKeyBuilder().getNavigationLabel()
        + "#" + attribute
        .getExternalName() + (attribute.getStructuredType() != null ? " (" + attribute.getStructuredType()
        .getExternalName() + ")" : ""));
    try {
      // this will cause a Join...
      final List<Selection<?>> joinSelections = createSelectClause(paths);

      // TODO remove
      final List<? extends JPAAttribute<?>> jpaKeyList = getQueryResultType().getKeyAttributes(true);
      final List<JPASelector> listKeyPaths = new ArrayList<JPASelector>(jpaKeyList.size());
      for (final JPAAttribute<?> key : jpaKeyList) {
        final JPASelector keyPath = getQueryResultType().getPath(key.getExternalName());
        listKeyPaths.add(keyPath);
      }

      cq.multiselect(joinSelections);
      final Expression<Boolean> where = createWhere();
      if (where != null) {
        cq.where(where);
      }
      final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
      // FIXME how to add TOP or SKIP for elements of another table? (do not work as
      // in JPAExpandQuery, because we have to avoid loading of too much rows)
      final List<Tuple> intermediateResult = tq.getResultList();
      return new QueryElementCollectionResult(intermediateResult, getLastAffectingNavigationKeyBuilder());
    } catch (final ODataJPAModelException e) {
      throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
          Locale.ENGLISH, e);
    }
  }

}
