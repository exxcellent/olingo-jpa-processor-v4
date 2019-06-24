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
class JPAExpandQuery extends JPAAbstractEntityQuery<CriteriaQuery<Tuple>> {

	private final JPAAssociationPath association;
	private final JPAExpandItemInfo item;
	private final CriteriaQuery<Tuple> cq;
	private final Root<?> root;

	public JPAExpandQuery(final JPAODataContext context, final EntityManager em,
	        final JPAExpandItemInfo item) throws ODataApplicationException {

		super(context, item.getEntityType(), em, item.getUriInfo());
		this.association = item.getExpandAssociation();
		this.item = item;
		this.cq = getCriteriaBuilder().createTupleQuery();
		this.root = cq.from(getQueryResultType().getTypeClass());
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
	 * Process a expand query, which contains a $skip and/or a $top option.
	 * <p>
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
			long skip = 0;
			long top = Long.MAX_VALUE;
			// TODO merge with implementation in JPAExpandQuery#execute()
			final UriInfoResource uriResource = getUriInfoResource();

			final Map<String, From<?, ?>> resultsetAffectingTables = createFromClause(Collections.emptyList());

			final List<JPASelector> selectionPathDirectMappings = buildSelectionPathList(uriResource);
			final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap = separateElementCollectionPaths(
					selectionPathDirectMappings);

			final List<Selection<?>> joinSelections = createSelectClause(selectionPathDirectMappings);

			// add the key columns of target entity to selection, to build the key to map
			// results for entries to owning entity
			final List<JPASelector> listAssociationJoinKeyPaths = association.getRightPaths();
			final Collection<JPASelector> additionalJoinKeyColumns = filterDuplicates(selectionPathDirectMappings,
					listAssociationJoinKeyPaths);

			// join with owning entity (target of association aka right side)
			joinSelections.addAll(createSelectClause(additionalJoinKeyColumns));

			cq.multiselect(joinSelections);

			cq.where(createWhere());

			final List<Order> orderBy = createOrderByJoinCondition(association);
			orderBy.addAll(createOrderByList(resultsetAffectingTables, uriResource.getOrderByOption()));
			cq.orderBy(orderBy);
			// TODO group by also at $expand
			final TypedQuery<Tuple> tupleQuery = getEntityManager().createQuery(cq);

			// Simplest solution for the problem. Read all and throw away, what is not requested
			final List<Tuple> intermediateResult = tupleQuery.getResultList();
			if (uriResource.getSkipOption() != null) {
				skip = uriResource.getSkipOption().getValue();
			}
			if (uriResource.getTopOption() != null) {
				top = uriResource.getTopOption().getValue();
			}

			final Map<String, List<Tuple>> result = convertResult(intermediateResult, skip, top);
			final JPAQueryEntityResult queryResult = new JPAQueryEntityResult(result, count(), getQueryResultType());
			// load not yet processed @ElementCollection attribute content
			queryResult.putElementCollectionResults(readElementCollections(elementCollectionMap));

			return queryResult;
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
				actuallKey = buildTargetResultKey(row, listJoinKeyPath);
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
			} catch (final IllegalArgumentException e) {
				LOG.log(Level.SEVERE,
				        "Problem converting database result for entity type " + item.getEntityType().getInternalName(),
				        e);
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

	private String buildTargetResultKey(final Tuple row, final List<JPASelector> joinColumns) {
		final StringBuffer buffer = new StringBuffer();
		for (final JPASelector item : joinColumns) {
			buffer.append(JPASelector.PATH_SEPERATOR);
			if (JPAAssociationPath.class.isInstance(item)) {
				// special case for relationships without join columns mapped as attribute -> we
				// have to take all the key attributes from the joined source to build an
				// 'result key'; see
				final boolean b = true;// FIXME
				throw new IllegalStateException("UPS");
			} else {
				// default simple case
				buffer.append(row.get(item.getAlias()));
			}
		}
		buffer.deleteCharAt(0);
		return buffer.toString();
	}

	private List<Order> createOrderByJoinCondition(final JPAAssociationPath a) throws ODataApplicationException {
		final List<Order> orders = new ArrayList<Order>();

		try {
			Path<?> path;
			for (final JPASelector j : a.getRightPaths()) {
				path = null;
				for (final JPAAttribute<?> attr : j.getPathElements()) {
					if (path == null) {
						path = root.get(attr.getInternalName());
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

	@Override
	protected Expression<Boolean> createWhere() throws ODataApplicationException {

		final CriteriaBuilder cb = getCriteriaBuilder();

		Expression<Boolean> whereCondition = null;
		try {
			whereCondition = getFilter().compile();
		} catch (final ExpressionVisitException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
			        HttpStatusCode.BAD_REQUEST, e);
		}

		if (whereCondition == null) {
			whereCondition = cb.exists(buildNavigationSubQueries());
		} else {
			whereCondition = cb.and(whereCondition, cb.exists(buildNavigationSubQueries()));
		}

		return whereCondition;
	}

	// TODO merge with JPAAbstractCriteriaQuery#buildNavigationSubQueries()
	private Subquery<?> buildNavigationSubQueries() throws ODataApplicationException {
		Subquery<?> childQuery = null;

		final List<UriResource> resourceParts = getUriInfoResource().getUriResourceParts();
		final IntermediateServiceDocument sd = getContext().getEdmProvider().getServiceDocument();

		// 1. Determine all relevant associations
		final List<JPANavigationPropertyInfo> expandPathList = Util.determineAssoziations(sd, resourceParts);
		expandPathList.addAll(item.getHops());

		// 2. Create the queries and roots
		JPAAbstractQuery<?> parent = this;
		final List<JPANavigationQuery> queryList = new ArrayList<JPANavigationQuery>();

		for (final JPANavigationPropertyInfo naviInfo : expandPathList) {
			final JPANavigationQuery newQuery = new JPANavigationQuery(sd, naviInfo.getUriResiource(), parent,
			        getEntityManager(),
			        naviInfo.getAssociationPath());
			queryList.add(newQuery);
			parent = newQuery;
		}
		// 3. Create select statements
		for (int i = queryList.size() - 1; i >= 0; i--) {
			childQuery = queryList.get(i).getSubQueryExists(childQuery);
		}
		return childQuery;
	}
}
