package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAEntityFilterProcessor;
import org.apache.olingo.jpa.processor.core.query.result.NavigationKeyBuilder;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

public abstract class AbstractCriteriaQueryBuilder<QT extends CriteriaQuery<DT>, DT> extends AbstractQueryBuilder {

  protected static enum InitializationState {
    NotInitialized, Initialized;
  }

  /**
   * Helper class to access the right entity ('FROM') for filter expression
   *
   */
  protected class FilterQueryBuilderContext implements FilterContextQueryBuilderIfc {

    private final JPAEntityType scopeEntity;
    private final From<?, ?> filterFrom;

    FilterQueryBuilderContext(final JPAEntityType filterEntity, final From<?, ?> filterFrom) {
      this.scopeEntity = filterEntity;
      this.filterFrom = filterFrom;
    }

    @Override
    public JPAODataRequestContext getContext() {
      return AbstractCriteriaQueryBuilder.this.context;
    }

    @Override
    public EntityManager getEntityManager() {
      return AbstractCriteriaQueryBuilder.this.getEntityManager();
    }

    @Override
    public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
      return AbstractCriteriaQueryBuilder.this.createSubquery(subqueryResultType);
    }

    @Override
    public JPAEntityType getQueryResultType() {
      return scopeEntity;
    }

    @Override
    public From<?, ?> getQueryResultFrom() {
      return filterFrom;
    }

  }

  private final JPAODataRequestContext context;
  private final NavigationIfc uriNavigation;
  private final EdmType edmType;
  private final UriResourceEntitySet queryStartUriResource;
  private final JPAEntityType jpaStartEntityType;
  private final List<UriParameter> keyPredicates;
  private final NavigationKeyBuilder jpaStartNavigationKeyBuilder;
  private List<NavigationBuilder> navigationQueryList = null;
  private InitializationState initStateType = InitializationState.NotInitialized;

  protected AbstractCriteriaQueryBuilder(final JPAODataRequestContext context, final NavigationIfc uriInfo,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(em);
    this.uriNavigation = uriInfo;
    this.context = context;
    this.queryStartUriResource = Util.determineStartingEntityUriResource(uriInfo.getFirstStep());
    assert queryStartUriResource != null;
    this.keyPredicates = determineKeyPredicates(queryStartUriResource);
    this.edmType = queryStartUriResource.getType();
    this.jpaStartEntityType = context.getEdmProvider().getServiceDocument().getEntityType(edmType);
    assert jpaStartEntityType != null;
    jpaStartNavigationKeyBuilder = new NavigationKeyBuilder(jpaStartEntityType);
  }

  protected abstract <T> Subquery<T> createSubquery(Class<T> subqueryResultType);

  protected void assertInitialized() {
    if (initStateType != InitializationState.Initialized) {
      throw new IllegalStateException("Not initialized");
    }
  }

  protected final IntermediateServiceDocument getServiceDocument() {
    return context.getEdmProvider().getServiceDocument();
  }

  protected final OData getOData() {
    return context.getOdata();
  }

  protected final JPAODataRequestContext getContext() {
    return context;
  }

  /**
   *
   * @return The entries from {@link UriInfoResource#getUriResourceParts()} that can be navigated, to avoid
   * simple/complex properties at end of path.
   */
  private static List<UriResource> extractNavigableResourcePath(final IntermediateServiceDocument sd,
      final List<UriResource> resourceParts) throws ODataApplicationException {
    if (!Util.hasNavigation(resourceParts)) {
      return resourceParts;
    }
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(sd, resourceParts);
    return resourceParts.subList(0, naviPathList.size());
  }

  /**
   *
   * @return The key builder matching rows from this query and expanded entities via navigation. The returned is not the
   * last key builder of navigation path, but that one, that will come into effect as key builder to map entities
   * from @ElementCollection or $expand queries to results of the query represented by this query builder.
   * @see #getQueryResultNavigationKeyBuilder()
   */
  protected final NavigationKeyBuilder getLastAffectingNavigationKeyBuilder() {
    assertInitialized();
    if (navigationQueryList.isEmpty() || navigationQueryList.size() < 2) {
      return jpaStartNavigationKeyBuilder;
    }
    // ignore the last navigation entry, because not necessary for navigation key building
    return navigationQueryList.get(navigationQueryList.size() - 2).getNavigationKeyBuilder();

  }

  private NavigationBuilder determineLastWorkingNavigationBuilder() {
    // ignore the last navigation entry, because not necessary for navigation key building
    // ignore also dummy nav queries (for @ElementCollection with navigation to property)
    for (int i = navigationQueryList.size(); i > 0; i--) {
      final NavigationBuilder navBuilder = navigationQueryList.get(i - 1);
      if (navBuilder.isWorking()) {
        return navBuilder;
      }
    }
    return null;
  }

  /**
   *
   * @return The key builder of last navigation element in query. This can be an 'not working' navigation builder.
   * @see #getLastAffectingNavigationKeyBuilder()
   */
  protected final NavigationKeyBuilder getQueryResultNavigationKeyBuilder() {
    assertInitialized();
    final NavigationBuilder last = determineLastWorkingNavigationBuilder();
    if (last == null) {
      return jpaStartNavigationKeyBuilder;
    }
    return last.getNavigationKeyBuilder();
  }

  // TODO try to remove this separate init step, maybe we can do that in the constructor again...
  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    navigationQueryList = createNavigationElements();
    if (initStateType == InitializationState.Initialized) {
      throw new IllegalStateException("Already initialized");
    }
    initStateType = InitializationState.Initialized;
  }

  /**
   *
   * @return The {@link #getQueryStartFrom() starting} query entity type.
   */
  public final JPAEntityType getQueryStartType() {
    return jpaStartEntityType;
  }

  protected final NavigationIfc getNavigation() {
    return uriNavigation;
  }

  /**
   *
   * @return The initial {@link From starting} entity table before first join.
   */
  public abstract <T> From<T, T> getQueryStartFrom();

  @SuppressWarnings("unchecked")
  public final From<DT, DT> getQueryResultFrom() {
    assertInitialized();

    final NavigationBuilder last = determineLastWorkingNavigationBuilder();
    // the resulting type is always the end navigation path or if not given the starting 'from'
    if (last == null) {
      return getQueryStartFrom();
    }
    return (From<DT, DT>) last.getQueryResultFrom();
  }

  public final JPAEntityType getQueryResultType() {
    assertInitialized();
    final NavigationBuilder last = determineLastWorkingNavigationBuilder();
    if (last == null) {
      return getQueryStartType();
    }
    return (JPAEntityType) last.getQueryResultType();
  }

  protected final EdmType getQueryResultEdmType() {
    assertInitialized();
    final NavigationBuilder last = determineLastWorkingNavigationBuilder();
    if (last == null) {
      return edmType;
    }
    return last.getNavigationUriInfoResource().getType();
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
  protected final boolean hasQueryLimits() throws ODataJPAQueryException {
    return (determineSkipValue() != null || determineTopValue() != null);
  }

  private Integer determineSkipValue() throws ODataJPAQueryException {
    final UriInfoResource uriResource = getNavigation().getLastStep();
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
    final UriInfoResource uriResource = getNavigation().getLastStep();
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

  private List<NavigationBuilder> createNavigationElements() throws ODataJPAModelException, ODataApplicationException {

    final List<UriResource> resourceParts = uriNavigation.getUriResourceParts();

    // 1. Determine all relevant associations
    final List<JPANavigationPropertyInfo> naviPathList = Util.determineNavigations(getServiceDocument(), resourceParts);

    // 2. Create the queries and roots
    final List<NavigationBuilder> navigationQueryList = new ArrayList<NavigationBuilder>(uriNavigation
        .getUriResourceParts().size());
    From<?, ?> parentFrom = getQueryStartFrom();// ==scope before navigation
    NavigationKeyBuilder keyBuilderParent = jpaStartNavigationKeyBuilder;
    for (final JPANavigationPropertyInfo naviInfo : naviPathList) {
      if (naviInfo.getNavigationPath() == null) {
        LOG.log(Level.SEVERE, "Association for navigation path to '"
            + naviInfo.getNavigationUriResource().getType().getName() + "' not found. Cannot resolve target entity");
        continue;
      }
      final NavigationBuilder navQuery = new NavigationBuilder(naviInfo.getNavigationUriResource(), naviInfo
          .getNavigationPath(), parentFrom, keyBuilderParent, getEntityManager());
      navigationQueryList.add(navQuery);
      parentFrom = navQuery.getQueryResultFrom();
      keyBuilderParent = navQuery.getNavigationKeyBuilder();
    }
    return navigationQueryList;
  }

  @SuppressWarnings("unchecked")
  private javax.persistence.criteria.Expression<Boolean> createWhereFromAccessConditioner()
      throws ODataApplicationException {
    final DataAccessConditioner<Object> dac = (DataAccessConditioner<Object>) getQueryResultType()
        .getDataAccessConditioner();
    if (dac == null) {
      return null;
    }
    getContext().getDependencyInjector().injectDependencyValues(dac);
    return dac.buildSelectCondition(getEntityManager(), (From<Object, Object>) getQueryResultFrom());
  }

  private final javax.persistence.criteria.Expression<Boolean> createWhereFromFilter(
      final FilterContextQueryBuilderIfc filterContext, final List<UriResource> navPath, final FilterOption filterOption)
          throws ExpressionVisitException, ODataApplicationException {

    // determine the navigation builder matching the filter affecting path element
    final VisitableExpression filterExpression = filterOption.getExpression();
    if (filterExpression == null) {
      return null;
    }

    final JPAEntityFilterProcessor<Boolean> filter = new JPAEntityFilterProcessor<Boolean>(getContext().getOdata(),
        getContext()
        .getEdmProvider().getServiceDocument(), getEntityManager(),
        filterContext.getQueryResultType(), getContext().getDatabaseProcessor(), navPath, filterExpression,
        filterContext);

    return filter.compile();
  }

  protected javax.persistence.criteria.Expression<Boolean> createWhere() throws ODataApplicationException,
  ODataJPAModelException {

    javax.persistence.criteria.Expression<Boolean> whereCondition = createWhereFromKeyPredicates();
    final javax.persistence.criteria.Expression<Boolean> accessConditionerClause =
        createWhereFromAccessConditioner();
    whereCondition = combineAND(whereCondition, accessConditionerClause);

    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398094
    // https://tools.oasis-open.org/version-control/browse/wsvn/odata/trunk/spec/ABNF/odata-abnf-construction-rules.txt

    try {
      // 1. check filter for current entity
      final FilterOption filterOption = uriNavigation.getFilterOption(queryStartUriResource);
      if (filterOption != null/* isLastNavigableElementInUriResource(uriInfo, queryStartUriResource) */) {
        // no navigation, so we may have an filter directly for this entity
        final List<UriResource> navPath = extractNavigableResourcePath(context.getEdmProvider().getServiceDocument(),
            uriNavigation.getFirstStep().getUriResourceParts());
        final FilterQueryBuilderContext filterContext = new FilterQueryBuilderContext(jpaStartEntityType,
            getQueryStartFrom());
        final javax.persistence.criteria.Expression<Boolean> filterCondition = createWhereFromFilter(filterContext,
            navPath,
            filterOption);
        whereCondition = combineAND(whereCondition, filterCondition);
      }
      // 2. check filter for navigation elements also
      for (final NavigationBuilder navQuery : navigationQueryList) {
        if (!navQuery.isWorking()) {
          continue;
        }
        final FilterOption navFilterOption = uriNavigation.getFilterOption(navQuery.getNavigationUriInfoResource());
        if (navFilterOption == null) {
          continue;
        }
        // TODO type cast ok? -> prefer JPAStructuredType
        final FilterQueryBuilderContext navFilterContext = new FilterQueryBuilderContext((JPAEntityType) navQuery
            .getQueryResultType(),
            navQuery.getQueryResultFrom());
        // build a navigation (sub) path up to the navigation element resource
        final List<UriResource> navResourcePath = new LinkedList<UriResource>();
        for (final UriResource current : uriNavigation.getUriResourceParts()) {
          navResourcePath.add(current);
          if (current == navQuery.getNavigationUriInfoResource()) {
            break;
          }
        }
        final List<UriResource> navFilterPath = extractNavigableResourcePath(context.getEdmProvider()
            .getServiceDocument(), navResourcePath);

        final javax.persistence.criteria.Expression<Boolean> navFilterCondition = createWhereFromFilter(
            navFilterContext, navFilterPath, navFilterOption);
        whereCondition = combineAND(whereCondition, navFilterCondition);
      }
    } catch (final ExpressionVisitException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.BAD_REQUEST, e);
    }

    final javax.persistence.criteria.Expression<Boolean> existsSubQuery = buildNavigationWhereClause();
    whereCondition = combineAND(whereCondition, existsSubQuery);
    whereCondition = combineAND(whereCondition, createWhereFromSearchOption(uriNavigation.getLastStep()
        .getSearchOption()));

    return whereCondition;
  }

  /**
   *
   * @return The identifiers for {@link #getQueryStartFrom()}
   */
  private final List<UriParameter> getKeyPredicates() {
    return keyPredicates;
  }

  private final javax.persistence.criteria.Expression<Boolean> createWhereFromKeyPredicates()
      throws ODataApplicationException {
    // javax.persistence.criteria.Expression<Boolean> whereCondition = null;

    // final List<UriResource> resources = uriResource.getUriResourceParts();
    final List<UriParameter> keyPredicates = getKeyPredicates();
    // keys are always for start table, not for target after joins
    final From<?, ?> root = getQueryStartFrom();
    final JPAEntityType rootType = getQueryStartType();
    // Given key: Organizations('1')
    return extendWhereByKey(root, rootType, keyPredicates);
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
  private javax.persistence.criteria.Expression<Boolean> buildNavigationWhereClause()
      throws ODataApplicationException, ODataJPAModelException {

    // 3. Create select statements
    javax.persistence.criteria.Expression<Boolean> whereCondition = null;
    for (final NavigationBuilder navQuery : navigationQueryList) {
      if (!navQuery.isWorking()) {
        continue;
      }
      final javax.persistence.criteria.Expression<Boolean> where = navQuery.buildJoinWhere();
      whereCondition = combineAND(whereCondition, where);
    }
    return whereCondition;
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
  private javax.persistence.criteria.Expression<Boolean> createWhereFromSearchOption(final SearchOption searchOption)
      throws ODataApplicationException,
      ODataJPAModelException {
    final FilterQueryBuilderContext filterHelper = new FilterQueryBuilderContext(getQueryResultType(),
        getQueryResultFrom());
    final SearchSubQueryBuilder searchQuery = new SearchSubQueryBuilder(filterHelper, searchOption);
    final Subquery<?> subquery = searchQuery.getSubQueryExists();
    if (subquery == null) {
      return null;
    }
    return getCriteriaBuilder().exists(subquery);
  }

  protected final List<JPAAssociationAttribute> extractOrderByNaviAttributes() throws ODataApplicationException {

    // TODO useless functionality, because we are joining already all navigation parts?!

    final OrderByOption orderBy = uriNavigation.getLastStep().getOrderByOption();
    if (orderBy == null) {
      return Collections.emptyList();
    }
    final JPAStructuredType jpaEntityType = getQueryResultType();
    final List<JPAAssociationAttribute> naviAttributes = new ArrayList<JPAAssociationAttribute>();
    for (final OrderByItem orderByItem : orderBy.getOrders()) {
      final Expression expression = orderByItem.getExpression();
      if (!Member.class.isInstance(expression)) {
        LOG.log(Level.WARNING, "OrderBy is supported only for Member expresssions not for " + expression.getClass()
        .getSimpleName());
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

  /**
   * The value of the $select query option is a comma-separated list of <b>properties</b>, qualified action names,
   * qualified function names, the <b>star operator (*)</b>, or the star operator prefixed with the namespace or alias
   * of the schema in order to specify all operations defined in the schema. See:
   * <a
   * href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398297"
   * >OData Version 4.0 Part 1 - 11.2.4.1 System Query Option $select</a>
   * <p>
   * See also:
   * <a
   * href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398163"
   * >OData Version 4.0 Part 2 - 5.1.3 System Query Option $select</a>
   *
   * @param select
   * @return
   * @throws ODataApplicationException
   */
  protected final List<Selection<?>> createSelectClause(final Collection<? extends JPASelector> jpaPathList)
      throws ODataJPAQueryException {

    final List<Selection<?>> selections = new LinkedList<Selection<?>>();

    // add keys from this query also if navigation exists
    if (!navigationQueryList.isEmpty()) {
      final List<JPASelector> listAssociationJoinKeyPaths = jpaStartNavigationKeyBuilder.getNavigationKeyPaths();
      for (final JPASelector jPath : listAssociationJoinKeyPaths) {
        // use FROM of starting entity, because the keys are all from that starting entity
        final Path<?> p = convertToCriteriaAliasPath(getQueryStartFrom(), jPath, jpaStartNavigationKeyBuilder
            .getNavigationAliasPrefix());
        if (p == null) {
          continue;
        }
        selections.add(p);
      }
    }

    // Build select clause
    for (final JPASelector jpaPath : jpaPathList) {
      final Path<?> p = convertToCriteriaAliasPath(getQueryResultFrom(), jpaPath, null);
      if (p == null) {
        continue;
      }
      selections.add(p);
    }

    // add selection of all JOIN table keys, required to handle $expand entity mapping
    selections.addAll(buildNavigationKeySelectionClauses());

    return selections;
  }

  private List<Selection<?>> buildNavigationKeySelectionClauses() throws ODataJPAQueryException {
    final List<Selection<?>> selections = new LinkedList<Selection<?>>();
    // ignore the last navigation, because not neccessary to build entity navigation key
    for (int i = 0; i < navigationQueryList.size() - 1; i++) {
      final NavigationBuilder navQuery = navigationQueryList.get(i);
      if (!navQuery.isWorking()) {
        continue;
      }
      try {
        final List<Selection<?>> joinSelection = navQuery.buildNavigationKeySelection();
        selections.addAll(joinSelection);
      } catch (ODataJPAModelException | ODataApplicationException e) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
            HttpStatusCode.INTERNAL_SERVER_ERROR, e);
      }
    }
    return selections;
  }

  protected final Map<String, From<?, ?>> createFromClause(final List<JPAAssociationAttribute> orderByTarget)
      throws ODataApplicationException {
    final HashMap<String, From<?, ?>> joinTables = new HashMap<String, From<?, ?>>();
    final From<?, ?> root = getQueryResultFrom();
    // 1. Create root
    final JPAEntityType jpaEntityType = getQueryResultType();
    joinTables.put(jpaEntityType.getInternalName(), root);

    // 2. OrderBy navigation property
    for (final JPAAssociationAttribute orderBy : orderByTarget) {
      final Join<?, ?> join = root.join(orderBy.getInternalName(), JoinType.LEFT);
      // Take on condition from JPA metadata; no explicit on
      joinTables.put(orderBy.getInternalName(), join);
    }

    return joinTables;
  }

  /**
   * @return A unique and reproducible alias name to access the attribute value in
   *         the result set after loading
   */
  @Deprecated
  protected static final String buildTargetJoinAlias(final JPAAssociationPath association,
      final JPAMemberAttribute targetAttribute) {
    return association.getAlias().concat("_").concat(targetAttribute.getInternalName());
  }

}
