package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;

/**
 * A query to retrieve the expand entities.
 * <p>
 * According to
 * <a href=
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398162"
 * >OData Version 4.0 Part 2 - 5.1.2 System Query Option $expand</a> the following query options are allowed:
 * <ul>
 * <li>expandCountOption = <b>filter</b>/ search
 * <p>
 * <li>expandRefOption = expandCountOption/ <b>orderby</b> / <b>skip</b> / <b>top</b> / inlinecount
 * <li>expandOption = expandRefOption/ <b>select</b>/ <b>expand</b> / levels
 * <p>
 * </ul>
 * As of now only the bold once are supported
 * <p>
 *
 * @author Oliver Grande
 *
 */
class JPAExpandQuery extends JPAAbstractEntityQuery<CriteriaQuery<Tuple>, Tuple> {

  private final JPAAssociationPath association;
  private final JPAExpandItemInfo item;
  private final CriteriaQuery<Tuple> cq;
  //  private final From<?, ?> expandTargetRoot;
  private final Root<?> from;

  public JPAExpandQuery(final JPAODataContext context, final EntityManager em,
      final JPAExpandItemInfo item) throws ODataApplicationException, ODataJPAModelException {
    super(item.getTargetEntitySet().getEntityType(), context, item.getUriInfo(), em);
    this.association = item.getExpandAssociation();
    this.item = item;
    this.cq = getCriteriaBuilder().createTupleQuery();

    // the 'source type' is used as start for relationship/navigation joins, because the reverse navigation is not
    // possible for unidirectional relationships; but the result set entity type will be the 'target type' aka
    // 'joinRoot' (see below)
    this.from = cq.from(association.getSourceType().getTypeClass());
    // we are in a $expand query; that means the results from the association target entity (right side) are only useful
    // if an entity instance exists as source (left side)
    // so we can use use a LEFT JOIN, because having results for source entity without target entities can be handled by
    // result set converter

    //    this.expandTargetRoot = buildJoinPath(from, association);

    // this.expandTargetRoot = expandSourceRoot.join(association.getAlias(), JoinType.LEFT);

    // this.expandTargetRoot = cq.from(getQueryResultType().getTypeClass());

    // now we are ready
    initializeQuery();
  }

  @Override
  public CriteriaQuery<Tuple> getQuery() {
    return cq;
  }

  @Override
  public Root<?> getQueryScopeFrom() {
    return from;
  }

  //  @SuppressWarnings("unchecked")
  //  @Override
  //  public From<?, ?> getQueryResultFrom() {
  //    return expandTargetRoot;
  //  }

  /**
   *
   * @param referenceList The list used as reference having entries that must be
   *                      not present in <i>filterList</i>.
   * @param filterList    The list to take only the entries not already present in
   *                      <i>referenceList</i>.
   * @return The collection containing the selectors not already present in
   *         <i>referenceList</i>.
   */
  private Collection<JPASelector> determineUnselectedAttributes(final Collection<JPASelector> referenceList,
      final List<JPASelector> filterList) {
    final List<JPASelector> listNotDuplicated = new LinkedList<JPASelector>();
    final Set<JPASelector> setReferences = new HashSet<>(referenceList);
    JPASelector current;
    for (int i = filterList.size(); i > 0; i--) {
      current = filterList.get(i - 1);
      if (setReferences.contains(current)) {
        continue;
      }
      listNotDuplicated.add(current);
    }
    return listNotDuplicated;
  }

  /**
   * Process a expand query, which contains a $skip and/or a $top option.<p>
   * This is a tricky problem, as it can not be done easily with SQL. It could be that a database offers special
   * solutions.
   * There is an worth reading blog regards this topic:
   * <a href="http://www.xaprb.com/blog/2006/12/07/how-to-select-the-firstleastmax-row-per-group-in-sql/">How to select
   * the first/least/max row per group in SQL</a>
   *
   * @return query result
   * @throws ODataApplicationException
   */
  public JPAQueryEntityResult execute() throws ODataApplicationException {

    LOG.log(Level.FINE, "Process $expand for: " + association.getSourceType().getExternalName() + " -[" + association
        .getAlias() + "]-> " + association.getTargetType().getExternalName());
    try {
      long skip = 0;
      long top = Long.MAX_VALUE;
      final UriInfoResource uriResource = getUriInfoResource();

      final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(Collections.emptyList());

      final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(uriResource);
      final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = separateElementCollectionPaths(
          selectionPathDirectMappings);

      List<Selection<?>> joinSelections = createSelectClause(selectionPathDirectMappings);

      // add the key columns of source entity to selection, to build the key to map
      // results for entries to owning entity
      // FIXME
      //      			final List<JPASelector> listAssociationJoinKeyPaths = association.getRightPaths();
      //      final List<JPASelector> listAssociationJoinKeyPaths = association.getLeftPaths();
      final List<JPASelector> listAssociationJoinKeyPaths = buildKeyPath(association.getSourceType());

      //			final Collection<JPASelector> additionalJoinKeyColumns = determineUnselectedAttributes(selectionPathDirectMappings,
      //					listAssociationJoinKeyPaths);
      //
      //			// join with owning entity (target of association aka right side)
      //			joinSelections.addAll(createSelectClause(additionalJoinKeyColumns));

      //      for (final JPASelector jpaPath : listAssociationJoinKeyPaths) {
      //        final Path<?> p = convertToCriteriaPath(from, jpaPath);
      //        if (p == null) {
      //          continue;
      //        }
      //        p.alias(buildTargetJoinAlias(association, (JPASimpleAttribute) jpaPath.getLeaf()));
      //        joinSelections.add(p);
      //      }

      joinSelections = extendSelectionWithEntityKeys(joinSelections);

      cq.multiselect(joinSelections);

      cq.where(createWhere());

      final List<Order> orderBy = createOrderByJoinCondition(association);
      orderBy.addAll(createOrderByList(resultsetAffectingTables, uriResource.getOrderByOption()));
      cq.orderBy(orderBy);
      final TypedQuery<Tuple> tupleQuery = getEntityManager().createQuery(cq);

      // Simplest solution for the problem. Read all and throw away, what is not requested
      final List<Tuple> intermediateResult = tupleQuery.getResultList();
      if (uriResource.getSkipOption() != null) {
        skip = uriResource.getSkipOption().getValue();
      }
      if (uriResource.getTopOption() != null) {
        top = uriResource.getTopOption().getValue();
      }

      // FIXME
      // use association self for key building
      final Map<String, List<Tuple>> result = convertResult(intermediateResult, skip, top, Collections.singletonList(
          association));
      //      final Map<String, List<Tuple>> result = convertResult(intermediateResult, skip, top, listAssociationJoinKeyPaths);
      final JPAQueryEntityResult queryResult = new JPAQueryEntityResult(result, count(), getQueryScopeType());
      // load not yet processed @ElementCollection attribute content
      queryResult.putElementCollectionResults(readElementCollections(elementCollectionMap));

      return queryResult;
    } catch (final ODataJPAModelException e) {
      throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
          Locale.ENGLISH, e);
    }
  }

  private Long count() {
    // TODO Count and Expand -> Olingo
    return null;
  }

  Map<String, List<Tuple>> convertResult(final List<Tuple> intermediateResult, final long skip, final long top,
      final List<JPASelector> listJoinKeyPath)
          throws ODataApplicationException {
    String joinKey = "";
    long skiped = 0;
    long taken = 0;

    List<Tuple> subResult = null;
    String actuallKey;
    final Map<String, List<Tuple>> convertedResult = new HashMap<String, List<Tuple>>();
    for (final Tuple row : intermediateResult) {
      try {
        // build key using target side + target side join columns, resulting key must be
        // identical for source side + source side join columns
        actuallKey = JPATupleAbstractConverter.buildOwningEntityKey(row, listJoinKeyPath);
      } catch (final ODataJPAModelException e) {
        throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
      } catch (final IllegalArgumentException e) {
        LOG.log(Level.SEVERE,
            "Problem converting database result for entity type " + item.getTargetEntitySet().getName(), e);
        throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
      }

      subResult = convertedResult.get(actuallKey);
      if (!actuallKey.equals(joinKey)) {
        subResult = new LinkedList<Tuple>();
        convertedResult.put(actuallKey, subResult);
        joinKey = actuallKey;
        skiped = taken = 0;
      }
      if (skiped >= skip && taken < top) {
        taken += 1;
        subResult.add(row);
      } else {
        skiped += 1;
      }
    }
    return convertedResult;
  }

  private List<Order> createOrderByJoinCondition(final JPAAssociationPath a) throws ODataApplicationException {
    final List<Order> orders = new ArrayList<Order>();

    try {
      Path<?> path;
      for (final JPASelector j : a.getRightPaths()) {
        path = null;
        for (final JPAAttribute<?> attr : j.getPathElements()) {
          if (path == null) {
            path = getQueryResultFrom().get(attr.getInternalName());
          } else {
            path = path.get(attr.getInternalName());
          }
        }
        if (path == null) {
          throw new IllegalStateException("Invalid model; cannot build join for "
              + a.getSourceType().getExternalName() + "#" + a.getAlias());
        }
        orders.add(getCriteriaBuilder().asc(path));
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
    return orders;
  }

  // TODO replace by super class implementation
  @Override
  protected Expression<Boolean> createWhere() throws ODataApplicationException, ODataJPAModelException {

    final CriteriaBuilder cb = getCriteriaBuilder();

    Expression<Boolean> whereCondition = null;
    try {
      whereCondition = getFilter().compile();
    } catch (final ExpressionVisitException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.BAD_REQUEST, e);
    }

    whereCondition = combineAND(whereCondition, buildNavigationSubQueries());
    //    if (whereCondition == null) {
    //      whereCondition = cb.exists(buildNavigationSubQueries());
    //    } else {
    //      whereCondition = cb.and(whereCondition, cb.exists(buildNavigationSubQueries()));
    //    }

    return whereCondition;
  }

  // TODO merge with JPAAbstractCriteriaQuery#buildNavigationSubQueries
  private javax.persistence.criteria.Expression<Boolean>/* Subquery<?> */ buildNavigationSubQueries()
      throws ODataApplicationException, ODataJPAModelException {
    final Subquery<?> childQuery = null;

    final List<UriResource> resourceParts = getUriInfoResource().getUriResourceParts();
    final IntermediateServiceDocument sd = getContext().getEdmProvider().getServiceDocument();

    // 1. Determine all relevant associations
    // TODO Are possible existing navigations always the same as the 'hops', so this
    // is a useless call here?!
    List<JPANavigationPropertyInfo> expandPathList = Util.determineNavigations(sd, resourceParts);
    expandPathList = new LinkedList<JPANavigationPropertyInfo>(expandPathList);
    expandPathList.addAll(item.getHops());

    // 2. Create the queries and roots
    From<?, ?> parentFrom = getQueryResultFrom();
    final List<JPAQueryNavigation> queryList = new ArrayList<JPAQueryNavigation>();

    for (final JPANavigationPropertyInfo naviInfo : expandPathList) {
      final JPAQueryNavigation newQuery = new JPAQueryNavigation(sd,
          naviInfo.getNavigationUriResource(),
          (JPAAssociationPath) naviInfo.getNavigationPath(), parentFrom, getEntityManager());
      queryList.add(newQuery);
      parentFrom = newQuery.getQueryResultFrom();
    }
    // 3. Create select statements
    //    for (int i = queryList.size() - 1; i >= 0; i--) {
    //      childQuery = queryList.get(i).getSubQueryExists(childQuery);
    //    }
    //    return childQuery;

    javax.persistence.criteria.Expression<Boolean> whereCondition = null;
    for (final JPAQueryNavigation navQuery : queryList) {
      final javax.persistence.criteria.Expression<Boolean> where = navQuery.buildJoinWhere();
      whereCondition = combineAND(whereCondition, where);
    }
    return whereCondition;

  }
}
