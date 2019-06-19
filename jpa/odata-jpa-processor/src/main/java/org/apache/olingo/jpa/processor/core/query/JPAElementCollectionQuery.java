package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryElementCollectionResult;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;

class JPAElementCollectionQuery extends JPAAbstractCriteriaQuery<CriteriaQuery<Tuple>> {
	private final CriteriaQuery<Tuple> cq;
	private final Root<?> root;
	@SuppressWarnings("unused")
	private final JPAAttribute<?> attribute;
	private final List<JPASelector> paths;

	JPAElementCollectionQuery(final OData odata, final JPAEntityType jpaEntityType,
			final JPAAttribute<?> attribute, final List<JPASelector> paths,
			final JPAODataSessionContextAccess context,
			final UriInfoResource uriInfo, final EntityManager em, final Map<String, List<String>> requestHeaders)
					throws ODataApplicationException {
		super(odata, context, jpaEntityType, em, requestHeaders, uriInfo);
		cq = getCriteriaBuilder().createTupleQuery();
		root = cq.from(jpaEntityType.getTypeClass());
		this.attribute = attribute;
		if (!attribute.isCollection()) {
			throw new IllegalStateException("The element is not a @ElementCollection, bug!");
		}
		this.paths = paths;
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

	public JPAQueryElementCollectionResult execute() throws ODataApplicationException {
		try {
			final List<Selection<?>> joinSelections = createSelectClause(paths);

			// add the key columns to selection, to build the key to map results for element
			// collection entries to owning entity
			// TODO don't select path's multiple times
			final List<? extends JPAAttribute<?>> jpaKeyList = getQueryResultType().getKeyAttributes(true);
			final List<JPASelector> listKeyPaths = new ArrayList<JPASelector>(jpaKeyList.size());
			for (final JPAAttribute<?> key : jpaKeyList) {
				final JPASelector keyPath = getQueryResultType().getPath(key.getExternalName());
				listKeyPaths.add(keyPath);
			}
			joinSelections.addAll(createSelectClause(listKeyPaths));

			cq.multiselect(joinSelections);
			final Expression<Boolean> where = createWhere();
			if (where != null) {
				cq.where(where);
			}
			final TypedQuery<Tuple> tq = getEntityManager().createQuery(cq);
			// FIXME how to add TOP or SKIP for elements of another table? (do not work as
			// in JPAExpandQuery, because we have to avoid loading of too much rows)
			final List<Tuple> intermediateResult = tq.getResultList();
			final Map<String, List<Tuple>> result = convertResult(intermediateResult, listKeyPaths);
			return new JPAQueryElementCollectionResult(result, listKeyPaths);
		} catch (final ODataJPAModelException e) {
			throw new ODataApplicationException(e.getMessage(), HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(),
					Locale.ENGLISH, e);
		}
	}

	private Map<String, List<Tuple>> convertResult(final List<Tuple> intermediateResult,
			final List<JPASelector> listKeyPath)
					throws ODataApplicationException {

		List<Tuple> subResult;
		String actualKey;
		final Map<String, List<Tuple>> convertedResult = new HashMap<String, List<Tuple>>();
		for (final Tuple row : intermediateResult) {
			// build key using the key columns from owning entity to assign to that entity
			// instances
			actualKey = buildTargetResultKey(row, listKeyPath);
			subResult = convertedResult.get(actualKey);
			if (subResult == null) {
				subResult = new LinkedList<Tuple>();
				convertedResult.put(actualKey, subResult);
			}
			subResult.add(row);
		}
		return convertedResult;
	}

	private String buildTargetResultKey(final Tuple row, final List<JPASelector> joinColumns) {
		final StringBuffer buffer = new StringBuffer();
		for (final JPASelector item : joinColumns) {
			buffer.append(JPASelector.PATH_SEPERATOR);
			buffer.append(row.get(item.getAlias()));
		}
		buffer.deleteCharAt(0);
		return buffer.toString();
	}

}
