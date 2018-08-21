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

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public class JPAQuery extends JPAAbstractEntityQuery {

	private final ServiceMetadata serviceMetadata;

	public JPAQuery(final OData odata, final EdmEntitySet entitySet, final JPAODataSessionContextAccess context,
			final UriInfo uriInfo, final EntityManager em, final Map<String, List<String>> requestHeaders,
			final ServiceMetadata serviceMetadata)
					throws ODataApplicationException, ODataJPAModelException {
		super(odata, context, context.getEdmProvider().getServiceDocument().getEntitySetType(entitySet.getName()), em,
				requestHeaders, uriInfo);
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * Counts the number of results to be expected by a query. The method shall fulfill the requirements of the $count
	 * query option. This is defined as follows:<p>
	 * <i>The $count system query option ignores any $top, $skip, or $expand query options, and returns the total count
	 * of results across all pages including only those results matching any specified $filter and $search.</i><p>
	 * For details see: <a href=
	 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398308"
	 * >OData Version 4.0 Part 1 - 11.2.5.5 System Query Option $count</a>
	 *
	 * @return Number of results
	 * @throws ODataApplicationException
	 * @throws ExpressionVisitException
	 */
	public Long countResults() throws ODataApplicationException {
		/*
		 * URL example:
		 * .../Organizations?$count=true
		 * .../Organizations/count
		 * .../Organizations('3')/Roles/$count
		 */
		final HashMap<String, From<?, ?>> joinTables = new HashMap<String, From<?, ?>>();

		final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		// root = cq.from(jpaEntity.getTypeClass());

		joinTables.put(jpaEntityType.getTypeClass().getCanonicalName(), root);

		final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
		if (whereClause != null) {
			cq.where(whereClause);
		}
		cq.select(cb.count(root));
		return em.createQuery(cq).getSingleResult();
	}

	public EntityCollection execute(final boolean processExpandOption) throws ODataApplicationException {
		// Pre-process URI parameter, so they can be used at different places
		// TODO check if Path is also required for OrderBy Attributes, as it is for descriptions

		final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
		final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(this.uriResource);
		final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(orderByNaviAttributes);

		cq.multiselect(createSelectClause(selectionPathDirectMappings));

		final javax.persistence.criteria.Expression<Boolean> whereClause = createWhere();
		if (whereClause != null) {
			cq.where(whereClause);
		}

		cq.orderBy(createOrderByList(resultsetAffectingTables, uriResource.getOrderByOption()));

		if (!orderByNaviAttributes.isEmpty()) {
			cq.groupBy(createGroupBy(selectionPathDirectMappings));
		}

		final TypedQuery<Tuple> tq = em.createQuery(cq);
		addTopSkip(tq);

		final HashMap<String, List<Tuple>> resultTuples = new HashMap<String, List<Tuple>>(1);
		final List<Tuple> intermediateResult = tq.getResultList();
		resultTuples.put(JPAQueryResult.ROOT_RESULT, intermediateResult);

		final JPAQueryResult queryResult = new JPAQueryResult(resultTuples,
				Long.valueOf(intermediateResult.size()), jpaEntityType);
		if (processExpandOption) {
			queryResult.putChildren(readExpandEntities(null, uriResource));
		}
		return convertToEntityCollection(queryResult);
	}

	private EntityCollection convertToEntityCollection(final JPAQueryResult result) throws ODataApplicationException {
		// Convert tuple result into an OData Result
		EntityCollection entityCollection;
		try {
			entityCollection = new JPATupleResultConverter(sd, result, getOData().createUriHelper(), serviceMetadata)
					.convertQueryResult();
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

		// Count results if requested
		final CountOption countOption = uriResource.getCountOption();
		if (countOption != null && countOption.getValue()) {
			entityCollection.setCount(Integer.valueOf(countResults().intValue()));
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
	 */
	private Map<JPAAssociationPath, JPAQueryResult> readExpandEntities(
			final List<JPANavigationProptertyInfo> parentHops, final UriInfoResource uriResourceInfo)
					throws ODataApplicationException {

		final Map<JPAAssociationPath, JPAQueryResult> allExpResults = new HashMap<JPAAssociationPath, JPAQueryResult>();
		// x/a?$expand=b/c($expand=d,e/f)

		final List<JPAExpandItemInfo> itemInfoList = new JPAExpandItemInfoFactory().buildExpandItemInfo(sd,
				uriResourceInfo.getUriResourceParts(), uriResourceInfo.getExpandOption(), parentHops);

		for (final JPAExpandItemInfo item : itemInfoList) {
			final JPAExpandQuery expandQuery = new JPAExpandQuery(getOData(), context, em, item, getRequestHeaders());
			final JPAQueryResult expandResult = expandQuery.execute();

			expandResult.putChildren(readExpandEntities( item.getHops(), item.getUriInfo()));
			allExpResults.put(item.getExpandAssociation(), expandResult);
		}

		return allExpResults;
	}

	public SelectOption getSelectOption() {
		return uriResource.getSelectOption();
	}

	private List<javax.persistence.criteria.Expression<?>> createGroupBy(final List<JPASelector> selectionPathList)
			throws ODataApplicationException {

		final List<javax.persistence.criteria.Expression<?>> groupBy =
				new ArrayList<javax.persistence.criteria.Expression<?>>();

		for (final JPASelector jpaPath : selectionPathList) {
			final Path<?> path = convertToCriteriaPath(jpaPath);
			if (path == null) {
				continue;
			}
			groupBy.add(path);
		}

		return groupBy;
	}

	private List<JPAAssociationAttribute> extractOrderByNaviAttributes() throws ODataApplicationException {
		final List<JPAAssociationAttribute> naviAttributes = new ArrayList<JPAAssociationAttribute>();

		final OrderByOption orderBy = uriResource.getOrderByOption();
		if (orderBy != null) {
			for (final OrderByItem orderByItem : orderBy.getOrders()) {
				final Expression expression = orderByItem.getExpression();
				if (expression instanceof Member) {
					final UriInfoResource resourcePath = ((Member) expression).getResourcePath();
					for (final UriResource uriResource : resourcePath.getUriResourceParts()) {
						if (uriResource instanceof UriResourceNavigation) {
							final EdmNavigationProperty edmNaviProperty = ((UriResourceNavigation) uriResource).getProperty();
							try {
								naviAttributes.add((JPAAssociationAttribute) jpaEntityType
										.getAssociationPath(edmNaviProperty.getName())
										.getLeaf());
							} catch (final ODataJPAModelException e) {
								throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_CONV_ERROR,
										HttpStatusCode.INTERNAL_SERVER_ERROR, e);
							}
						}
					}
				}
			}
		}
		return naviAttributes;
	}

}
