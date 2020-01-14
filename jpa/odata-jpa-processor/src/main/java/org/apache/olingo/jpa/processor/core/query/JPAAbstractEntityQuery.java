package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryElementCollectionResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public abstract class JPAAbstractEntityQuery<QT extends CriteriaQuery<DT>, DT> extends
JPAAbstractCriteriaQuery<QT, DT> {

  private final List<UriParameter> keyPredicates;
  private final UriResourceEntitySet queryStartUriResource;
  private final UriInfoResource uriResource;
  private List<JPANavigationQuery> navigationQueryList = null;

  protected JPAAbstractEntityQuery(final EdmEntityType edmEntityType, final JPAODataContext context,
      final UriInfoResource uriInfo,
      final EntityManager em) throws ODataApplicationException, ODataJPAModelException {
    super(context, Util.determineStartingEntityUriResource(uriInfo).getEntityType(), em, uriInfo);
    this.uriResource = uriInfo;
    queryStartUriResource = Util.determineStartingEntityUriResource(uriInfo);
    assert queryStartUriResource != null;
    this.keyPredicates = determineKeyPredicates(queryStartUriResource);
  }

  protected final JPAEntityType determineStartingEntityType()
      throws ODataJPAModelException {
    final EdmEntitySet es = getQueryStartUriResource().getEntitySet();
    return getServiceDocument().getEntitySetType(es.getName());
  }

  @Override
  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    navigationQueryList = buildNavigation();
    for (final JPANavigationQuery query : navigationQueryList) {
      query.initializeQuery();
    }
    super.initializeQuery();
  }

  protected UriInfoResource getUriInfoResource() {
    return uriResource;
  }

  @Override
  protected final List<UriParameter> getKeyPredicates() {
    return keyPredicates;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <T> From<T, T> getQueryResultFrom() {
    assertInitialized();

    // the resulting type is always the end navigation path or if not given the starting 'from'
    if (navigationQueryList.isEmpty()) {
      return getQueryScopeFrom();
    }
    final JPANavigationQuery navQuery = navigationQueryList.get(navigationQueryList.size() - 1);
    return (From<T, T>) navQuery.getQueryResultFrom();
  }

  /**
   *
   * @return The uri resource assigned to start table/entity of query.
   */
  protected final UriResourceEntitySet getQueryStartUriResource() {
    return queryStartUriResource;
  }

  /**
   *
   * @return The resource info for the target entity, derived from last resource element (navigation).
   */
  protected final UriResource getQueryResultUriInfoResource() {
    assertInitialized();
    if (navigationQueryList.isEmpty()) {
      return getQueryScopeUriInfoResource();
    }
    return navigationQueryList.get(navigationQueryList.size() - 1).getQueryScopeUriInfoResource();
  }

  @Override
  protected final JPAEntityType getQueryResultType() {
    assertInitialized();
    if (navigationQueryList.isEmpty()) {
      return getQueryScopeType();
    }
    return navigationQueryList.get(navigationQueryList.size() - 1).getQueryResultType();
  }

  protected final EdmType getQueryResultEdmType() {
    assertInitialized();
    if (navigationQueryList.isEmpty()) {
      return getQueryScopeEdmType();
    }
    return navigationQueryList.get(navigationQueryList.size() - 1).getQueryScopeEdmType();
  }

  /**
   * Extend the given selection list (derived from {@link #getQueryResultType()}) with key attributes from query start
   * {@link #getQueryScopeType() entity}.
   * @throws ODataJPAModelException
   */
  protected List<Selection<?>> extendSelectionWithEntityKeys(final List<Selection<?>> joinSelections)
      throws ODataJPAModelException {

    final List<Selection<?>> result = new LinkedList<Selection<?>>(joinSelections);
    final List<JPASelector> listAssociationJoinKeyPaths = buildKeyPath(getQueryScopeType());
    for (final JPASelector jpaPath : listAssociationJoinKeyPaths) {
      final Path<?> p = convertToCriteriaPath(getQueryScopeFrom(), jpaPath);
      if (p == null) {
        continue;
      }
      // p.alias(buildTargetJoinAlias(association, (JPASimpleAttribute) jpaPath.getLeaf()));
      result.add(p);

    }
    return result;
  }

  /**
   * Limits in the query will result in LIMIT, SKIP, OFFSET/FETCH or other
   * database specific expression used for pagination of SQL result set. This will
   * affect also all dependent queries for $expand's or @ElementCollection loading
   * related queries.
   *
   * @return TRUE if the resulting query will have limits reflecting the presence
   * of $skip or $top in request.
   * @throws ODataJPAQueryException
   */
  public final boolean hasQueryLimits() throws ODataJPAQueryException {
    return (determineSkipValue() != null || determineTopValue() != null);
  }

  private Integer determineSkipValue() throws ODataJPAQueryException {
    final UriInfoResource uriResource = getUriInfoResource();
    final SkipOption skipOption = uriResource.getSkipOption();
    if (skipOption == null) {
      return null;
    }
    final int skipNumber = skipOption.getValue();
    if (skipNumber >= 0) {
      return Integer.valueOf(skipNumber);
    } else {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
          HttpStatusCode.BAD_REQUEST, Integer.toString(skipNumber), "$skip");
    }
  }

  private Integer determineTopValue() throws ODataJPAQueryException {
    final UriInfoResource uriResource = getUriInfoResource();
    final TopOption topOption = uriResource.getTopOption();
    if (topOption == null) {
      return null;
    }
    final int topNumber = topOption.getValue();
    if (topNumber >= 0) {
      return Integer.valueOf(topNumber);
    } else {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_INVALID_VALUE,
          HttpStatusCode.BAD_REQUEST, Integer.toString(topNumber), "$top");
    }
  }

  /**
   * Applies the $skip and $top options of the OData request to the query. The
   * values are defined as follows:
   * <ul>
   * <li>The $top system query option specifies a non-negative integer n that
   * limits the number of items returned from a collection.
   * <li>The $skip system query option specifies a non-negative integer n that
   * excludes the first n items of the queried collection from the result.
   * </ul>
   * For details see: <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398306"
   * >OData Version 4.0 Part 1 - 11.2.5.3 System Query Option $top</a>
   *
   * @throws ODataApplicationException
   */
  protected final void addTopSkip(final TypedQuery<Tuple> tq) throws ODataApplicationException {
    /*
     * Where $top and $skip are used together, $skip MUST be applied before $top, regardless of the order in which they
     * appear in the request.
     * If no unique ordering is imposed through an $orderby query option, the service MUST impose a stable ordering
     * across requests that include $skip.
     * URL example: http://localhost:8080/BuPa/BuPa.svc/Organizations?$count=true&$skip=5
     */

    final Integer topValue = determineTopValue();
    if (topValue != null) {
      tq.setMaxResults(topValue.intValue());
    }

    final Integer skipValue = determineSkipValue();
    if (skipValue != null) {
      tq.setFirstResult(skipValue.intValue());
    }
  }

  /**
   * All @ElementCollection attributes must be loaded in a separate query for
   * every attribute (being a @ElementCollection).
   *
   * @param completeSelectorList
   *            The list to process (must be a modifiable list to
   *            remove elements from it)
   * @return The elements removed from <i>completeSelectorList</i>, because are
   *         for attributes being a
   *         {@link javax.persistence.ElementCollection @ElementCollection}. The
   *         selector elements are sorted into a map for all selectors starting
   *         from the same attribute (first element in path list).
   */
  protected final Map<JPAAttribute<?>, List<JPASelector>> separateElementCollectionPaths(
      final List<JPASelector> completeSelectorList) {
    final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = new HashMap<>();
    List<JPASelector> elementCollectionList;
    for (int i = completeSelectorList.size(); i > 0; i--) {
      final JPASelector jpaPath = completeSelectorList.get(i - 1);
      final JPAAttribute<?> firstPathElement = jpaPath.getPathElements().get(0);
      if (!firstPathElement.isCollection()) {
        continue;
      }
      elementCollectionList = elementCollectionMap.get(firstPathElement);
      if (elementCollectionList == null) {
        elementCollectionList = new LinkedList<JPASelector>();
        elementCollectionMap.put(firstPathElement, elementCollectionList);
      }
      elementCollectionList.add(completeSelectorList.remove(i - 1));
    }
    return elementCollectionMap;
  }

  protected final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> readElementCollections(
      final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap) throws ODataApplicationException,
  ODataJPAModelException {
    if (elementCollectionMap.isEmpty()) {
      return Collections.emptyMap();
    }

    final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> allResults = new HashMap<>();
    final EdmType edmType = getQueryResultEdmType();
    // build queries with most elements also used for primary entity selection
    // query, but with a few adaptions for the @ElementCollection selection
    for (final Entry<JPAAttribute<?>, List<JPASelector>> entry : elementCollectionMap.entrySet()) {
      // create separate SELECT for every entry (affected attribute)

      final JPAAttribute<?> attribute = entry.getKey();

      final JPAElementCollectionQuery query = new JPAElementCollectionQuery(edmType, attribute,
          entry.getValue(), getContext(), getUriInfoResource(), getEntityManager());
      final JPAQueryElementCollectionResult result = query.execute();
      allResults.put(attribute, result);
    }
    return allResults;
  }

  private List<JPANavigationQuery> buildNavigation() throws ODataJPAModelException, ODataApplicationException {
    final List<UriResource> resourceParts = uriResource.getUriResourceParts();

    // 1. Determine all relevant associations
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(getServiceDocument(), resourceParts);

    // 2. Create the queries and roots
    final List<JPANavigationQuery> navigationQueryList = new ArrayList<JPANavigationQuery>(uriResource
        .getUriResourceParts().size());
    JPAAbstractQuery<?, ?> parent = this;
    for (final JPANavigationPropertyInfo naviInfo : naviPathList) {
      if (naviInfo.getNavigationPath() == null) {
        LOG.log(Level.SEVERE, "Association for navigation path to '"
            + naviInfo.getNavigationUriResource().getType().getName() + "' not found. Cannot resolve target entity");
        continue;
      }

      final JPANavigationQuery navQuery = new JPANavigationQuery(getServiceDocument(), naviInfo
          .getNavigationUriResource(),
          (JPAAssociationPath) naviInfo.getNavigationPath(), parent, getEntityManager());
      navigationQueryList.add(navQuery);
      parent = navQuery;
    }
    return navigationQueryList;
  }

  @Override
  protected javax.persistence.criteria.Expression<Boolean> createWhere() throws ODataApplicationException,
  ODataJPAModelException {

    javax.persistence.criteria.Expression<Boolean> whereCondition = super.createWhere();
    final javax.persistence.criteria.Expression<Boolean> existsSubQuery = buildNavigationSubQueries();
    whereCondition = combineAND(whereCondition, existsSubQuery);

    if (uriResource.getSearchOption() != null && uriResource.getSearchOption().getSearchExpression() != null) {
      whereCondition = combineAND(whereCondition, createWhereFromSearchOption());
    }

    return whereCondition;
  }

  /**
   * Generate sub-queries in order to select the target of a navigation to a different entity
   * <p>
   * In case of multiple navigation steps a inner navigation has a dependency in both directions, to the upper and to
   * the lower query:
   * <p>
   * <code>SELECT * FROM upper WHERE EXISTS( <p>
   * SELECT ... FROM inner WHERE upper = inner<p>
   * AND EXISTS( SELECT ... FROM lower<p>
   * WHERE inner = lower))</code>
   * <p>
   * This is solved by a three steps approach
   * @throws ODataJPAModelException
   */
  private javax.persistence.criteria.Expression<Boolean> buildNavigationSubQueries()
      throws ODataApplicationException, ODataJPAModelException {

    // 3. Create select statements
    Subquery<?> jpaChildQuery = null;

    // reverse order to get the first subquery as direct EXISTS of this parent query
    for (int i = navigationQueryList.size() - 1; i >= 0; i--) {
      final JPANavigationQuery navQuery = navigationQueryList.get(i);
      jpaChildQuery = navQuery.getSubQueryExists(jpaChildQuery);
    }

    if (jpaChildQuery == null) {
      return null;
    }
    return getCriteriaBuilder().exists(jpaChildQuery);
  }

  /**
   * Search at OData:
   * <p>
   * <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/os/part1-protocol/odata-v4.0-os-part1-protocol.html#_Toc372793700">
   * OData Version 4.0 Part 1 - 11.2.5.6 System Query Option $search</a>
   * <p>
   * <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html#_Toc372793865">
   * OData Version 4.0 Part 2 - 5.1.7 System Query Option $search</a>
   *
   * @return
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   */
  private javax.persistence.criteria.Expression<Boolean> createWhereFromSearchOption() throws ODataApplicationException,
  ODataJPAModelException {
    final JPASearchQuery searchQuery = new JPASearchQuery(this);
    final Subquery<?> subquery = searchQuery.getSubQueryExists();
    if (subquery == null) {
      return null;
    }
    return getCriteriaBuilder().exists(subquery);
  }

  protected final List<JPAAssociationAttribute> extractOrderByNaviAttributes() throws ODataApplicationException {

    final OrderByOption orderBy = uriResource.getOrderByOption();
    if (orderBy == null) {
      return Collections.emptyList();
    }
    final JPAEntityType jpaEntityType = getQueryResultType();
    final List<JPAAssociationAttribute> naviAttributes = new ArrayList<JPAAssociationAttribute>();
    for (final OrderByItem orderByItem : orderBy.getOrders()) {
      final Expression expression = orderByItem.getExpression();
      if (!Member.class.isInstance(expression)) {
        continue;
      }
      final UriInfoResource resourcePath = ((Member) expression).getResourcePath();
      for (final UriResource uriResource : resourcePath.getUriResourceParts()) {
        if (!UriResourceNavigation.class.isInstance(uriResource)) {
          continue;
        }
        final EdmNavigationProperty edmNaviProperty = ((UriResourceNavigation) uriResource)
            .getProperty();
        try {
          naviAttributes.add((JPAAssociationAttribute) jpaEntityType
              .getAssociationPath(edmNaviProperty.getName()).getLeaf());
        } catch (final ODataJPAModelException e) {
          throw new ODataJPAQueryException(
              ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
              HttpStatusCode.INTERNAL_SERVER_ERROR, e);
        }
      }
    }
    return naviAttributes;
  }

}
