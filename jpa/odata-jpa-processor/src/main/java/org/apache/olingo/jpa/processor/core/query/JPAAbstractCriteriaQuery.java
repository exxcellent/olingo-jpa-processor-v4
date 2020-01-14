package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAEntityFilterProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public abstract class JPAAbstractCriteriaQuery<QT extends CriteriaQuery<RT>, RT> extends JPAAbstractQuery<QT, RT> {

  private final JPAEntityFilterProcessor filter;
  private final JPAODataContext context;
  private final UriResourceEntitySet queryStartUriResource;

  public JPAAbstractCriteriaQuery(final JPAODataContext context, final EdmType edmType, final EntityManager em,
      final UriInfoResource uriInfo)
          throws ODataApplicationException, ODataJPAModelException {
    super(context.getEdmProvider().getServiceDocument(), edmType, em);
    queryStartUriResource = Util.determineStartingEntityUriResource(uriInfo);

    this.filter = new JPAEntityFilterProcessor(context.getOdata(), context.getEdmProvider().getServiceDocument(), em,
        getQueryScopeType(),
        context.getDatabaseProcessor(), uriInfo, this);
    this.context = context;
  }

  protected final OData getOData() {
    return context.getOdata();
  }

  protected final JPAEntityFilterProcessor getFilter() {
    return filter;
  }

  protected final List<JPASelector> buildEntityPathList(final JPAEntityType jpaEntity)
      throws ODataApplicationException {

    try {
      return jpaEntity.getPathList();
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
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

    final String[] selectList = select.split(SELECT_ITEM_SEPERATOR); // OData separator for $select
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
          final List<JPAAttributePath> c = jpaEntity.searchChildPath(selectItemPath);
          jpaPathList.addAll(c);
        } else {
          // Primitive Type
          jpaPathList.add(selectItemPath);
        }
      }
      Collections.sort(jpaPathList);
      final List<JPASelector> keyPaths = buildKeyPath(jpaEntity);
      for (final JPASelector keyPath : keyPaths) {
        final int insertAt = Collections.binarySearch(jpaPathList, keyPath);
        if (insertAt < 0) {
          LOG.log(Level.WARNING,
              "OData-JPA-Adapter doesn't support $select without including of all key attributes, will add '"
                  + keyPath.getAlias() + "' as part of result");
          jpaPathList.add((insertAt * -1) - 1, keyPath);
        }
      }
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
    }
    return jpaPathList;
  }

  /**
   *
   * @return The list of selectors for all key attributes (exploded!).
   * @see JPAEntityType#getKeyAttributes(boolean)
   */
  protected static List<JPASelector> buildKeyPath(final JPAStructuredType jpaType) throws ODataJPAModelException {
    final List<? extends JPAAttribute<?>> jpaKeyList = jpaType.getKeyAttributes(true);
    final List<JPASelector> jpaPathList = new ArrayList<JPASelector>();
    for (final JPAAttribute<?> key : jpaKeyList) {
      final JPASelector keyPath = jpaType.getPath(key.getExternalName());
      jpaPathList.add(keyPath);
    }
    return jpaPathList;
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

    // TODO select that fields only if $expand option is given
    // TODO use JPAExpandItemInfoFactory
    // Add also fields that are required for $expand (for the key building to merge
    // results from sub queries)
    final Map<JPAExpandItemWrapper, JPAAssociationPath> associationPathList = Util.determineExpands(
        getServiceDocument(),
        uriResource.getUriResourceParts(), uriResource.getExpandOption());
    if (!associationPathList.isEmpty()) {
      Collections.sort(jpaPathList);
      for (final Entry<JPAExpandItemWrapper, JPAAssociationPath> entry : associationPathList.entrySet()) {
        //        try {
        // FIXME test only
        //        final int insertIndex = Collections.binarySearch(jpaPathList, entry.getValue());
        //        if (insertIndex < 0) {
        //          jpaPathList.add(/* Math.abs(insertIndex), */ entry.getValue());
        //        }

        //          for (final JPASelector leftSelector : entry.getValue().getLeftPaths()) {
        //            if (JPAAssociationPath.class.isInstance(leftSelector) && LOG.isLoggable(Level.FINEST)) {
        //              // we have to expand a relationship without mapped join columns...
        //              LOG.log(Level.FINEST, "The $expand must join the association target of '"
        //                  + JPAAssociationPath.class.cast(leftSelector).getAlias()
        //                  + "', because the join column is not mapped as attribute... but we need the key attribute values from the other side!");
        //            }
        //            final int insertIndex = Collections.binarySearch(jpaPathList, leftSelector);
        //            if (insertIndex < 0) {
        //              jpaPathList.add(/* Math.abs(insertIndex), */ leftSelector);
        //            }
        //          }
        //        } catch (final ODataJPAModelException e) {
        //          throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
        //        }
      }
    }
    return jpaPathList;

  }

  @Override
  protected final UriResource getQueryScopeUriInfoResource() {
    return queryStartUriResource;
  }

  /**
   *
   * @param orderByTarget
   * @param queryRoot
   * @return
   * @throws ODataApplicationException
   */
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
  protected final List<Order> createOrderByList(final Map<String, From<?, ?>> joinTables,
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

    // Build select clause
    for (final JPASelector jpaPath : jpaPathList) {
      // TODO 2. move logic into 'convertToCriteriaPath()'?

      // special join case for not mapped join columns
      if (JPAAssociationPath.class.isInstance(jpaPath)) {
        // happens for $expand queries without join columns mapped as attribute(s)
        final JPAAssociationPath asso = ((JPAAssociationPath) jpaPath);
        try {
          // join all the key attributes from target side table so we can build a 'result
          // key' from the tuples in the result set
          final List<JPASimpleAttribute> keys = asso.getTargetType().getKeyAttributes(true);
          final From<?, ?> join = getQueryResultFrom()
              .join(asso.getSourceType().getAssociationByPath(asso).getInternalName());
          for (final JPASimpleAttribute jpaAttribute : keys) {
            final Path<?> p = join.get(jpaAttribute.getInternalName());
            p.alias(buildTargetJoinAlias(asso, jpaAttribute));
            selections.add(p);
          }
        } catch (final ODataJPAModelException e) {
          throw new ODataJPAQueryException(e, HttpStatusCode.NOT_ACCEPTABLE);
        }
        continue;
      }

      // default case
      final Path<?> p = convertToCriteriaPath(getQueryResultFrom(), jpaPath);
      if (p == null) {
        continue;
      }
      // TODO 1. move 'alias' setting into 'convertToCriteriaPath()'?
      p.alias(jpaPath.getAlias());
      selections.add(p);
    }

    return selections;
  }

  /**
   * @return A unique and reproducible alias name to access the attribute value in
   *         the result set after loading
   */
  static final String buildTargetJoinAlias(final JPAAssociationPath association,
      final JPASimpleAttribute targetAttribute) {
    return association.getAlias().concat("_").concat(targetAttribute.getInternalName());
  }

  /**
   *
   * @return The identifiers for {@link #getQueryScopeFrom()}
   */
  protected abstract List<UriParameter> getKeyPredicates();

  protected final javax.persistence.criteria.Expression<Boolean> createWhereFromKeyPredicates()
      throws ODataApplicationException {
    //    javax.persistence.criteria.Expression<Boolean> whereCondition = null;

    //    final List<UriResource> resources = uriResource.getUriResourceParts();
    final List<UriParameter> keyPredicates = getKeyPredicates();
    // keys are always for start table, not for target after joins
    final From<?, ?> root = getQueryScopeFrom();
    // Given key: Organizations('1')
    final javax.persistence.criteria.Expression<Boolean> whereCondition = extendWhereByKey(root, null, keyPredicates);

    //    UriResource resourceItem = null;
    //    // Given key: Organizations('1')
    //    if (resources != null) {
    //      for (int i = resources.size() - 1; i >= 0; i--) {
    //        resourceItem = resources.get(i);
    //        if (resourceItem instanceof UriResourceEntitySet || resourceItem instanceof UriResourceNavigation) {
    //          break;
    //        }
    //      }
    //    final List<UriParameter> keyPredicates = determineKeyPredicates(resourceItem);
    //      whereCondition = extendWhereByKey(root, whereCondition, keyPredicates);
    //    }
    return whereCondition;
  }

  private javax.persistence.criteria.Expression<Boolean> createWhereConditionFromAccessConditioner() throws ODataApplicationException {
    final DataAccessConditioner<?> dac = getQueryScopeType().getDataAccessConditioner();
    if (dac == null) {
      return null;
    }
    context.getDependencyInjector().injectFields(dac);
    return dac.buildSelectCondition(getEntityManager(), getQueryResultFrom());
  }

  protected javax.persistence.criteria.Expression<Boolean> createWhere() throws ODataApplicationException,
  ODataJPAModelException {

    javax.persistence.criteria.Expression<Boolean> whereCondition = createWhereFromKeyPredicates();
    final javax.persistence.criteria.Expression<Boolean> accessConditionerClause = createWhereConditionFromAccessConditioner();
    whereCondition = combineAND(whereCondition, accessConditionerClause);

    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
    // http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398094
    // https://tools.oasis-open.org/version-control/browse/wsvn/odata/trunk/spec/ABNF/odata-abnf-construction-rules.txt
    try {
      whereCondition = combineAND(whereCondition, filter.compile());
    } catch (final ExpressionVisitException e) {
      throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
          HttpStatusCode.BAD_REQUEST, e);
    }

    return whereCondition;
  }

  @Override
  protected final Locale getLocale() {
    return context.getLocale();
  }

  @Override
  final JPAODataContext getContext() {
    return context;
  }


}
