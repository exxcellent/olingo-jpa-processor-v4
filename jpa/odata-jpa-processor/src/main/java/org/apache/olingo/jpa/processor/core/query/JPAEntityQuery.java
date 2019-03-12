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

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.CountOption;

public class JPAEntityQuery extends JPAAbstractEntityQuery<CriteriaQuery<Tuple>> {

	private final ServiceMetadata serviceMetadata;
	private final CriteriaQuery<Tuple> cq;
	private final Root<?> root;

	public JPAEntityQuery(final OData odata, final EdmEntitySet entitySet, final JPAODataSessionContextAccess context,
			final UriInfo uriInfo, final EntityManager em, final Map<String, List<String>> requestHeaders,
			final ServiceMetadata serviceMetadata)
					throws ODataApplicationException, ODataJPAModelException {
		super(odata, context, context.getEdmProvider().getServiceDocument().getEntitySetType(entitySet.getName()), em,
				requestHeaders, uriInfo);
		this.serviceMetadata = serviceMetadata;
		this.cq = cb.createTupleQuery();
		this.root = cq.from(jpaEntityType.getTypeClass());
	}

	@Override
	public CriteriaQuery<Tuple> getQuery() {
		return cq;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Root<?> getRoot() {
		return root;
	}

	/**
	 * @see JPAEntityCountQuery#execute()
	 *
	 */
	public EntityCollection execute(final boolean processExpandOption) throws ODataApplicationException {
		// Pre-process URI parameter, so they can be used at different places
		// TODO check if Path is also required for OrderBy Attributes, as it is for descriptions

		final List<JPAAssociationAttribute> orderByNaviAttributes = extractOrderByNaviAttributes();
		final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(orderByNaviAttributes);

		final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(this.uriResource);
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
	 */
	private Map<JPAAssociationPath, JPAQueryResult> readExpandEntities(
			final List<JPANavigationProptertyInfo> parentHops, final UriInfoResource uriResourceInfo)
					throws ODataApplicationException {

		final Map<JPAAssociationPath, JPAQueryResult> allExpResults = new HashMap<JPAAssociationPath, JPAQueryResult>();
		// x/a?$expand=b/c($expand=d,e/f)

		final List<JPAExpandItemInfo> itemInfoList = new JPAExpandItemInfoFactory().buildExpandItemInfo(sd,
				uriResourceInfo.getUriResourceParts(), uriResourceInfo.getExpandOption(), parentHops);

		// an expand query is a query selecting the target entity using a id-join for
		// the owning entity
		for (final JPAExpandItemInfo item : itemInfoList) {
			final JPAExpandQuery expandQuery = new JPAExpandQuery(getOData(), context, em, item, getRequestHeaders());
			final JPAQueryResult expandResult = expandQuery.execute();

			expandResult.putChildren(readExpandEntities( item.getHops(), item.getUriInfo()));
			allExpResults.put(item.getExpandAssociation(), expandResult);
		}

		return allExpResults;
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

}