package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 * 
 * @author Oliver Grande
 *
 */
abstract class JPAAbstractRelationshipQuery extends JPAAbstractQuery<Subquery<?>> {

	private final List<UriParameter> keyPredicates;
	private final JPAAssociationPath association;
	private final Root<?> queryRoot;
	private final Subquery<?> subQuery;
	private final JPAAbstractQuery<?> parentCall;

	public <T extends Object> JPAAbstractRelationshipQuery(final IntermediateServiceDocument sd, final UriResource uriResourceItem,
	        final JPAAbstractQuery<?> parent, final EntityManager em, final JPAAssociationPath association)
	        throws ODataApplicationException {

		super(sd.getEntityType(((UriResourcePartTyped) uriResourceItem).getType()), em);
		this.keyPredicates = determineKeyPredicates(uriResourceItem);
		this.association = association;
		if (association == null) {
			throw new IllegalArgumentException("association required");
		}
		this.parentCall = parent;
		this.subQuery = parent.getQuery().subquery(Integer.class);// we select always '1'
		this.queryRoot = subQuery.from(getQueryResultType().getTypeClass());
	}

	protected JPAAssociationPath getAssociation() {
		return association;
	}

	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	final public Root<?> getRoot() {
		assert queryRoot != null;
		return queryRoot;
	}

	@Override
	final public Subquery<?> getQuery() {
		return subQuery;
	}

	@SuppressWarnings("unchecked")
	public final <T extends Object> Subquery<T> getSubQueryExists(final Subquery<?> childQuery)
	        throws ODataApplicationException {
		final CriteriaBuilder cb = getCriteriaBuilder();

		try {
			// EXISTS subselect needs only a marker select for existence
			((Subquery<T>) getQuery()).select((Expression<T>) getCriteriaBuilder().literal(Integer.valueOf(1)));

			final Expression<Boolean> subqueryWhere = createSubqueryWhereByAssociation();

			Expression<Boolean> whereCondition = extendWhereByKey(queryRoot, subqueryWhere, this.keyPredicates);
			if (childQuery != null) {
				if (whereCondition != null) {
					whereCondition = cb.and(whereCondition, cb.exists(childQuery));
				} else {
					whereCondition = cb.exists(childQuery);
				}
			}
			if (whereCondition == null) {
				throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
				        HttpStatusCode.INTERNAL_SERVER_ERROR,
				        new IllegalStateException("Couldn't determine WHERE condition"), association.getAlias());
			}
			subQuery.where(whereCondition);
			handleAggregation(subQuery, queryRoot);
			return (Subquery<T>) subQuery;
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
			        HttpStatusCode.INTERNAL_SERVER_ERROR, e, association.getAlias());
		}
	}

	abstract protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
	        throws ODataApplicationException, ODataJPAModelException;

	abstract protected Expression<Boolean> createSubqueryWhereByAssociation()
			throws ODataApplicationException, ODataJPAModelException;

	@Override
	final protected Locale getLocale() {
		return parentCall.getLocale();
	}

	@Override
	final JPAODataContext getContext() {
		return parentCall.getContext();
	}
}
