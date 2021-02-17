package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.result.ExpandQueryEntityResult;
import org.apache.olingo.jpa.processor.core.query.result.QueryElementCollectionResult;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public class EntityQueryBuilder extends AbstractCriteriaQueryBuilder<CriteriaQuery<Tuple>, Tuple> {

  protected static final String SELECT_ITEM_SEPARATOR = ",";
  protected static final String SELECT_ALL = "*";

  private final ServiceMetadata serviceMetadata;
  private final CriteriaQuery<Tuple> cq;
  private final Root<?> startFrom;

  /**
   *
   * @param edmEntityTypeScope The starting entity type used as scope for FROM.
   * @param context
   * @param uriInfo
   * @param em
   * @param serviceMetadata
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   */
  public EntityQueryBuilder(final JPAODataRequestContext context, final NavigationIfc uriInfo,
      final EntityManager em,
      final ServiceMetadata serviceMetadata)
          throws ODataApplicationException, ODataJPAModelException {
    super(context, uriInfo, em);
    this.serviceMetadata = serviceMetadata;
    this.cq = getCriteriaBuilder().createTupleQuery();

    // the 'source type' is used as start for relationship/navigation joins, because the reverse navigation is not
    // possible for unidirectional relationships; but the result set entity type will be the 'target type' aka
    // 'joinRoot' (see below)
    this.startFrom = cq.from(getQueryStartType().getTypeClass());

    // now we are ready
    initializeQuery();
  }

  @Override
  public <T> Subquery<T> createSubquery(final Class<T> subqueryResultType) {
    return cq.subquery(subqueryResultType);
  }

  @SuppressWarnings("unchecked")
  @Override
  public From<?, ?> getQueryStartFrom() {
    return startFrom;
  }

  /**
   * @throws SerializerException
   * @see EntityCountQueryBuilder#execute()
   *
   */
  public final <O> O execute(final boolean processExpandOption,
      final Transformation<QueryEntityResult, O> transformer) throws ODataApplicationException,
  ODataJPAModelException, SerializerException {
    final QueryEntityResult queryResult = executeInternal(processExpandOption, null, null);
    return transformer.transform(queryResult);
  }

  protected final QueryEntityResult executeInternal(final boolean processExpandOption, String parentIdAttributeName, List<Object> parentIds)
      throws ODataApplicationException, ODataJPAModelException {
    final UriInfoResource uriResource = getNavigation().getLastStep();
    // Pre-process URI parameter, so they can be used at different places
    // TODO check if Path is also required for OrderBy Attributes, as it is for descriptions

    final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
    final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(orderByNaviAttributes);

    final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(uriResource);
    final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = separateElementCollectionPaths(
        selectionPathDirectMappings);

    // use selection for reduced list
    final List<Selection<?>> joinSelections = createSelectClause(selectionPathDirectMappings);

    cq.multiselect(joinSelections);

    final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
    if (whereClause != null) {
      cq.where(whereClause);
    }

    if (parentIds != null) {
      CriteriaBuilder.In<Boolean> inClause = getCriteriaBuilder().in(getQueryStartFrom().get(parentIdAttributeName).in(parentIds));
      cq.where(combineAND(inClause.getExpression(), whereClause));
    }

    // TODO force orderBy if 'hasLimits'
    cq.orderBy(createOrderByList(resultsetAffectingTables, uriResource.getOrderByOption()));

    if (!orderByNaviAttributes.isEmpty()) {
      cq.groupBy(createGroupBy(selectionPathDirectMappings));
    }

    final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
    if (hasQueryLimits()) {
      addTopSkip(tq);
    }

    final List<Tuple> intermediateResult = tq.getResultList();
    final QueryEntityResult queryResult = new QueryEntityResult(intermediateResult, getQueryResultType());

    // load not yet processed @ElementCollection attribute content
    queryResult.putElementCollectionResults(readElementCollections(elementCollectionMap));

    if (processExpandOption && !intermediateResult.isEmpty()) {
      String idAttributeName = getQueryResultType().getKeyAttributes(false).get(0).getInternalName();
      List<Object> ids = intermediateResult.stream().map(it -> it.get(idAttributeName)).collect(Collectors.toList());
      // generate expand queries only for non empty entity result list
      queryResult.putExpandResults(readExpandEntities(null, idAttributeName, ids));
    }
    return queryResult;
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
   * @param uriInfo
   * @param parentHops
   * @param idAttributeName
   * @param parentIds
   * @return
   * @throws ODataApplicationException
   * @throws ODataJPAModelException
   */
  private Map<JPAAssociationPath, ExpandQueryEntityResult> readExpandEntities(
          final List<JPANavigationPropertyInfo> parentHops, String idAttributeName, List<Object> parentIds)
          throws ODataApplicationException, ODataJPAModelException {

    final Map<JPAAssociationPath, ExpandQueryEntityResult> allExpResults =
        new HashMap<JPAAssociationPath, ExpandQueryEntityResult>();
    // x/a?$expand=b/c($expand=d,e/f)

    final NavigationIfc uriInfo = getNavigation();

    final Map<NavigationViaExpand, JPAAssociationPath> expandMapList = Util.determineExpands(
        getServiceDocument(), uriInfo);

    final JPAODataRequestContext context = getContext();
    final EntityManager em = getEntityManager();

    for (final Entry<NavigationViaExpand, JPAAssociationPath> itemExpand : expandMapList.entrySet()) {
      // an expand is handled as navigation to that entity type, so we can (re)use the entity query
      final EntityQueryBuilder expandQuery = new EntityQueryBuilder(context, itemExpand.getKey(), em, serviceMetadata);
      LOG.log(Level.FINE, "Process $expand for: " + getQueryResultNavigationKeyBuilder().getNavigationLabel() + "#"
          + itemExpand.getValue().getAlias());
      final QueryEntityResult expandResult = expandQuery.executeInternal(true, idAttributeName, parentIds);
      // convert result list to expand entity navigation key mapping structure
      allExpResults.put(itemExpand.getValue(), new ExpandQueryEntityResult(itemExpand.getValue(), expandResult,
          expandQuery
          .getLastAffectingNavigationKeyBuilder()));
    }

    return allExpResults;
  }

  private List<javax.persistence.criteria.Expression<?>> createGroupBy(final List<JPASelector> selectionPathList)
      throws ODataApplicationException {

    final List<javax.persistence.criteria.Expression<?>> groupBy = new ArrayList<javax.persistence.criteria.Expression<?>>();

    for (final JPASelector jpaPath : selectionPathList) {
      final Path<?> path = convertToCriteriaAliasPath(getQueryResultFrom(), jpaPath, null);
      if (path == null) {
        continue;
      }
      groupBy.add(path);
    }

    return groupBy;
  }

  private final List<JPASelector> buildEntityPathList(final JPAEntityType jpaEntity)
      throws ODataApplicationException {

    try {
      return jpaEntity.getPathList();
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
  }

  protected final List<JPASelector> buildSelectionPathList(final UriInfoResource uriResource)
      throws ODataApplicationException {
    final JPAEntityType jpaEntityType = getQueryResultType();
    List<JPASelector> jpaPathList = null;
    // TODO It is also possible to request all actions or functions available for each returned entity:
    // http://host/service/Products?$select=DemoService.*

    // Convert uri select options into a list of jpa attributes
    String selectionText = null;
    final List<UriResource> resources = uriResource.getUriResourceParts();

    selectionText = Util.determinePropertyNavigationPath(resources);
    // TODO Combine path selection and $select e.g. Organizations('4')/Address?$select=Country,Region
    if (selectionText == null || selectionText.isEmpty()) {
      final SelectOption select = uriResource.getSelectOption();
      if (select != null) {
        selectionText = select.getText();
      }
    }

    if (selectionText != null && selectionText.contains(Util.VALUE_RESOURCE)) {
      jpaPathList = buildPathValue(jpaEntityType, selectionText);
    } else if (selectionText != null && !selectionText.equals(SELECT_ALL) && !selectionText.isEmpty()) {
      jpaPathList = buildPathList(jpaEntityType, selectionText);
    } else {
      jpaPathList = buildEntityPathList(jpaEntityType);
    }
    // filter ignored columns here, because we may add later ignored columns to
    // select columns required to $expand
    for (int i = jpaPathList.size(); i > 0; i--) {
      final JPASelector selector = jpaPathList.get(i - 1);
      if (selector.getLeaf().ignore()) {
        jpaPathList.remove(i - 1);
      }
    }

    try {
      if (jpaEntityType.hasStream()) {
        final JPASelector mimeTypeAttribute = jpaEntityType.getContentTypeAttributePath();
        if (mimeTypeAttribute != null) {
          jpaPathList.add(mimeTypeAttribute);
        }
      }
    } catch (final ODataJPAModelException e1) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e1);
    }

    return jpaPathList;

  }

  private List<JPASelector> buildPathValue(final JPAEntityType jpaEntity, final String select)
      throws ODataApplicationException {

    List<JPASelector> jpaPathList = new ArrayList<JPASelector>();
    String selectString;
    try {
      selectString = select.replace(Util.VALUE_RESOURCE, "");
      if (selectString.isEmpty()) {
        // Stream value
        jpaPathList.add(jpaEntity.getStreamAttributePath());
        jpaPathList.addAll(jpaEntity.getKeyPath());
      } else {
        // Property value
        selectString = selectString.substring(0, selectString.length() - 1);
        jpaPathList = buildPathList(jpaEntity, selectString);
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
    return jpaPathList;
  }

  private List<JPASelector> buildPathList(final JPAEntityType jpaEntity, final String select)
      throws ODataApplicationException {

    final String[] selectList = select.split(SELECT_ITEM_SEPARATOR); // OData separator for $select
    return buildPathList(jpaEntity, selectList);
  }

  private List<JPASelector> buildPathList(final JPAEntityType jpaEntity, final String[] selectList)
      throws ODataApplicationException {

    final List<JPASelector> jpaPathList = new ArrayList<JPASelector>();
    try {
      for (final String selectItem : selectList) {
        final JPASelector selectItemPath = jpaEntity.getPath(selectItem);
        if (selectItemPath.getLeaf().isComplex()) {
          // Complex Type
          final List<JPASelector> c = jpaEntity.searchChildPath(selectItemPath);
          jpaPathList.addAll(c);
        } else {
          // Primitive Type
          jpaPathList.add(selectItemPath);
        }
      }
      // add key attributes
      final List<JPASelector> keyPaths = Util.buildKeyPath(jpaEntity);
      for (final JPASelector keyPath : keyPaths) {
        if (!jpaPathList.contains(keyPath)) {
          jpaPathList.add(keyPath);
        }
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
    return jpaPathList;
  }

  /**
   * If asc or desc is not specified, the service MUST order by the specified property in ascending order.
   * See:
   * <a
   * href=
   * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398305"
   * >OData Version 4.0 Part 1 - 11.2.5.2 System Query Option $orderby</a>
   * <p>
   *
   * @throws ODataJPAModelException
   *
   */
  private final List<Order> createOrderByList(final Map<String, From<?, ?>> joinTables,
      final OrderByOption orderByOption)
          throws ODataApplicationException {
    // .../Organizations?$orderby=Address/Country --> one item, two resourcePaths
    // [...ComplexProperty,...PrimitiveProperty]
    // .../Organizations?$orderby=Roles/$count --> one item, two resourcePaths [...NavigationProperty,...Count]
    // .../Organizations?$orderby=Roles/$count desc,Address/Country asc -->two items
    //
    // SQL example to order by number of entities of the
    // SELECT t0."BusinessPartnerID" ,COUNT(t1."BusinessPartnerID")
    // FROM {oj "OLINGO"."org.apache.olingo.jpa::BusinessPartner" t0
    // LEFT OUTER JOIN "OLINGO"."org.apache.olingo.jpa::BusinessPartnerRole" t1
    // ON (t1."BusinessPartnerID" = t0."BusinessPartnerID")}
    // WHERE (t0."Type" = ?)
    // GROUP BY t0."BusinessPartnerID"
    // ORDER BY COUNT(t1."BusinessPartnerID") DESC

    // TODO Functions and orderBy: Part 1 - 11.5.3.1 Invoking a Function

    final List<Order> orders = new ArrayList<Order>();
    if (orderByOption != null) {
      final CriteriaBuilder cb = getCriteriaBuilder();
      final JPAEntityType jpaEntityType = getQueryResultType();

      for (final OrderByItem orderByItem : orderByOption.getOrders()) {
        final Expression expression = orderByItem.getExpression();
        if (expression instanceof Member) {
          final UriInfoResource resourcePath = ((Member) expression).getResourcePath();
          JPAStructuredType type = jpaEntityType;
          Path<?> p = joinTables.get(jpaEntityType.getInternalName());
          assert p != null;
          for (final UriResource uriResource : resourcePath.getUriResourceParts()) {
            if (uriResource instanceof UriResourcePrimitiveProperty) {
              final EdmProperty edmProperty = ((UriResourcePrimitiveProperty) uriResource).getProperty();
              try {
                final JPAAttribute<?> attribute = type.getPath(edmProperty.getName()).getLeaf();
                p = p.get(attribute.getInternalName());
              } catch (final ODataJPAModelException e) {
                throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
              }
              if (orderByItem.isDescending()) {
                orders.add(cb.desc(p));
              } else {
                orders.add(cb.asc(p));
              }
            } else if (uriResource instanceof UriResourceComplexProperty) {
              final EdmProperty edmProperty = ((UriResourceComplexProperty) uriResource).getProperty();
              try {
                final JPAAttribute<?> attribute = type.getPath(edmProperty.getName()).getLeaf();
                p = p.get(attribute.getInternalName());
                type = attribute.getStructuredType();
              } catch (final ODataJPAModelException e) {
                throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
              }
            } else if (uriResource instanceof UriResourceNavigation) {
              final EdmNavigationProperty edmNaviProperty = ((UriResourceNavigation) uriResource).getProperty();
              From<?, ?> join;
              try {
                join = joinTables
                    .get(jpaEntityType.getAssociationPath(edmNaviProperty.getName()).getLeaf()
                        .getInternalName());
              } catch (final ODataJPAModelException e) {
                throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
              }
              if (orderByItem.isDescending()) {
                orders.add(cb.desc(cb.count(join)));
              } else {
                orders.add(cb.asc(cb.count(join)));
              }
            } // else if (uriResource instanceof UriResourceCount) {}
          }
        }
      }
    }
    return orders;
  }

  /**
   * All @ElementCollection attributes must be loaded in a separate query for
   * every attribute (being a @ElementCollection).
   *
   * @param completeSelectorList
   * The list to process (must be a modifiable list to
   * remove elements from it)
   * @return The elements removed from <i>completeSelectorList</i>, because are
   * for attributes being a
   * {@link javax.persistence.ElementCollection @ElementCollection}. The
   * selector elements are sorted into a map for all selectors starting
   * from the same attribute (first element in path list).
   */
  private final Map<JPAAttribute<?>, List<JPASelector>> separateElementCollectionPaths(
      final List<JPASelector> completeSelectorList) {
    final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = new HashMap<>();
    List<JPASelector> elementCollectionList;
    for (int i = completeSelectorList.size(); i > 0; i--) {
      final JPASelector jpaPath = completeSelectorList.get(i - 1);
      final JPAAttribute<?> firstPathElement = jpaPath.getPathElements().get(0);
      if (!firstPathElement.isJoinCollection()) {
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

  private final Map<JPAAttribute<?>, QueryElementCollectionResult> readElementCollections(
      final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap) throws ODataApplicationException,
  ODataJPAModelException {
    if (elementCollectionMap.isEmpty()) {
      return Collections.emptyMap();
    }

    final EdmStructuredType owningType = (EdmStructuredType) getQueryResultEdmType();
    final Map<JPAAttribute<?>, QueryElementCollectionResult> allResults = new HashMap<>();
    // build queries with most elements also used for primary entity selection
    // query, but with a few adaptions for the @ElementCollection selection
    for (final Entry<JPAAttribute<?>, List<JPASelector>> entry : elementCollectionMap.entrySet()) {
      // create separate SELECT for every entry (affected attribute)
      final JPAAttribute<?> attribute = entry.getKey();
      final ElementCollectionQueryBuilder query = new ElementCollectionQueryBuilder(owningType, attribute,
          entry.getValue(), getContext(), getNavigation(), getEntityManager());
      final QueryElementCollectionResult result = query.execute();
      allResults.put(attribute, result);
    }
    return allResults;
  }

}
