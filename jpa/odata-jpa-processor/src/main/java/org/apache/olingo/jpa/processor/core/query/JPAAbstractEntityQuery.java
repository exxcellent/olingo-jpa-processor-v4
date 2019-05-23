package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.result.JPAQueryElementCollectionResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;

public abstract class JPAAbstractEntityQuery<QueryType extends CriteriaQuery<?>>
        extends JPAAbstractCriteriaQuery<QueryType> {

	protected JPAAbstractEntityQuery(final EdmEntitySet entitySet, final JPAODataContext context, final UriInfoResource uriInfo,
	        final EntityManager em)
	        throws ODataApplicationException, ODataJPAModelException {

		super(context, entitySet, em, uriInfo);
	}

	protected JPAAbstractEntityQuery(final JPAODataContext context, final JPAEntityType jpaEntityType, final EntityManager em,
	        final UriInfoResource uriResource) throws ODataApplicationException {
		super(context, jpaEntityType, em, uriResource);
	}

	/**
	 * Limits in the query will result in LIMIT, SKIP, OFFSET/FETCH or other
	 * database specific expression used for pagination of SQL result set. This will
	 * affect also all dependent queries for $expand's or @ElementCollection loading
	 * related queries.
	 *
	 * @return TRUE if the resulting query will have limits reflecting the presence
	 *         of $skip or $top in request.
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
	        final Map<JPAAttribute<?>, List<JPASelector>> elementCollectionMap) throws ODataApplicationException {
		if (elementCollectionMap.isEmpty()) {
			return Collections.emptyMap();
		}

		final Map<JPAAttribute<?>, JPAQueryElementCollectionResult> allResults = new HashMap<>();
		final JPAEntityType jpaEntityType = getJPAEntityType();
		// build queries with most elements also used for primary entity selection
		// query, but with a few adaptions for the @ElementCollection selection
		for (final Entry<JPAAttribute<?>, List<JPASelector>> entry : elementCollectionMap.entrySet()) {
			// create separate SELECT for every entry (affected attribute)

			final JPAAttribute<?> attribute = entry.getKey();

			final JPAElementCollectionQuery query = new JPAElementCollectionQuery(jpaEntityType, attribute,
			        entry.getValue(), getContext(), getUriInfoResource(), getEntityManager());
			final JPAQueryElementCollectionResult result = query.execute();
			allResults.put(attribute, result);
		}
		return allResults;
	}

}
