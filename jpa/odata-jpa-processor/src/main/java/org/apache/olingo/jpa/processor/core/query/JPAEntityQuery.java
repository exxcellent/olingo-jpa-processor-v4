package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.CountOption;

public class JPAEntityQuery extends JPAAbstractEntityQuery<CriteriaQuery<Tuple>, Tuple> {

  private final ServiceMetadata serviceMetadata;
  private final CriteriaQuery<Tuple> cq;
  private final Root<?> startFrom;
  //  private final From<?, ?> resultFrom;

  public JPAEntityQuery(final EdmEntityType edmEntityType, final JPAODataContext context, final UriInfo uriInfo,
      final EntityManager em,
      final ServiceMetadata serviceMetadata)
          throws ODataApplicationException, ODataJPAModelException {
    super(edmEntityType, context, uriInfo, em);
    this.serviceMetadata = serviceMetadata;
    this.cq = getCriteriaBuilder().createTupleQuery();

    //    this.resultFrom = cq.from(getQueryResultType().getTypeClass());

    // the 'source type' is used as start for relationship/navigation joins, because the reverse navigation is not
    // possible for unidirectional relationships; but the result set entity type will be the 'target type' aka
    // 'joinRoot' (see below)
    this.startFrom = cq.from(determineStartingEntityType().getTypeClass());

    // now we are ready
    initializeQuery();
  }

  @Override
  public final CriteriaQuery<Tuple> getQuery() {
    return cq;
  }

  @Override
  public From<?, ?> getQueryScopeFrom() {
    return startFrom;
  }

  /**
   * @throws ODataJPAModelException
   * @see JPAEntityCountQuery#execute()
   *
   */
  public final EntityCollection execute(final boolean processExpandOption) throws ODataApplicationException,
  ODataJPAModelException {
    final UriInfoResource uriResource = getUriInfoResource();
    // Pre-process URI parameter, so they can be used at different places
    // TODO check if Path is also required for OrderBy Attributes, as it is for descriptions

    final boolean hasLimits = hasQueryLimits();
    final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
    final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(orderByNaviAttributes);

    final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(uriResource);
    final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = separateElementCollectionPaths(
        selectionPathDirectMappings);

    // use selection for reduced list

    final List<Selection<?>> joinSelections = createSelectClause(selectionPathDirectMappings);
    //    joinSelections = extendSelectionWithEntityKeys(joinSelections);

    cq.multiselect(joinSelections);

    final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }

    // TODO force orderBy if 'hasLimits'
    cq.orderBy(createOrderByList(resultsetAffectingTables, uriResource.getOrderByOption()));

    if (!orderByNaviAttributes.isEmpty()) {
      cq.groupBy(createGroupBy(selectionPathDirectMappings));
    }

    final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
    if (hasLimits) {
      addTopSkip(tq);
    }

    final HashMap<String, List<Tuple>> resultTuples = new HashMap<String, List<Tuple>>(1);
    final List<Tuple> intermediateResult = tq.getResultList();
    resultTuples.put(JPAQueryEntityResult.ROOT_RESULT, intermediateResult);

    final JPAQueryEntityResult queryResult = new JPAQueryEntityResult(resultTuples,
        Long.valueOf(intermediateResult.size()), getQueryResultType());

    // load not yet processed @ElementCollection attribute content
    queryResult.putElementCollectionResults(readElementCollections(elementCollectionMap));

    if (processExpandOption && !intermediateResult.isEmpty()) {
      // generate expand queries only for non empty entity result list
      queryResult.putExpandResults(readExpandEntities(null, uriResource));
    }
    return convertToEntityCollection(queryResult);
  }

  private EntityCollection convertToEntityCollection(final JPAQueryEntityResult result) throws ODataApplicationException {
    // Convert tuple result into an OData Result
    EntityCollection entityCollection;
    try {
      // TODO check cast
      entityCollection = new JPATuple2EntityConverter(getContext().getEdmProvider().getServiceDocument(),
          result.getEntityType(), getOData().createUriHelper(),
          serviceMetadata)
          .convertQueryResult(result);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }

    // Count results if requested
    final CountOption countOption = getUriInfoResource().getCountOption();
    if (countOption != null && countOption.getValue()) {
      entityCollection.setCount(Integer.valueOf(entityCollection.getEntities().size()));
    }

    return entityCollection;
  }

  /**
   * $expand is implemented as a recursively processing of all expands with a DB
   * round trip per expand item. Alternatively also a <i>big</i> join could be
   * created. This would lead to a transport of redundant data, but has only one
   * round trip. It has not been measured under which conditions which solution as
   * the better performance.
   * <p>
   * For a general overview see: <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398298"
   * >OData Version 4.0 Part 1 - 11.2.4.2 System Query Option $expand</a>
   * <p>
   *
   * For a detailed description of the URI syntax see: <a href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398162"
   * >OData Version 4.0 Part 2 - 5.1.2 System Query Option $expand</a>
   *
   * @param headers
   * @param naviStartEdmEntitySet
   * @param parentHops
   * @param uriResourceInfo
   * @return
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   */
  private Map<JPAAssociationPath, JPAQueryEntityResult> readExpandEntities(
      final List<JPANavigationPropertyInfo> parentHops, final UriInfoResource uriResourceInfo)
          throws ODataApplicationException, ODataJPAModelException {

    final Map<JPAAssociationPath, JPAQueryEntityResult> allExpResults = new HashMap<JPAAssociationPath, JPAQueryEntityResult>();
    // x/a?$expand=b/c($expand=d,e/f)

    final List<JPAExpandItemInfo> itemInfoList = JPAExpandItemInfoFactory.buildExpandItemInfo(
        getContext().getEdmProvider().getServiceDocument(),
        uriResourceInfo.getUriResourceParts(), uriResourceInfo.getExpandOption(), parentHops);

    // an expand query is a query selecting the target entity using a id-join for
    // the owning entity
    for (final JPAExpandItemInfo item : itemInfoList) {
      final JPAExpandQuery expandQuery = new JPAExpandQuery(getContext(), getEntityManager(), item);
      final JPAQueryEntityResult expandResult = expandQuery.execute();

      expandResult.putExpandResults(readExpandEntities(item.getHops(), item.getUriInfo()));
      allExpResults.put(item.getExpandAssociation(), expandResult);
    }

    return allExpResults;
  }

  private List<javax.persistence.criteria.Expression<?>> createGroupBy(final List<JPASelector> selectionPathList)
      throws ODataApplicationException {

    final List<javax.persistence.criteria.Expression<?>> groupBy = new ArrayList<javax.persistence.criteria.Expression<?>>();

    for (final JPASelector jpaPath : selectionPathList) {
      final Path<?> path = convertToCriteriaPath(getQueryResultFrom(), jpaPath);
      if (path == null) {
        continue;
      }
      groupBy.add(path);
    }

    return groupBy;
  }

}
