package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 * @author Oliver Grande
 *
 */
abstract class JPAAbstractRelationshipQuery extends JPAAbstractQuery<Subquery<?>> {
	private final List<UriParameter> keyPredicates;
	private final JPAAssociationPath association;
	private final Root<?> queryRoot;
	private final Subquery<?> subQuery;
	private final JPAAbstractQuery<?> parentQuery;

	public <T extends Object> JPAAbstractRelationshipQuery(final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery<?> parent, final EntityManager em, final JPAAssociationPath association)
					throws ODataApplicationException {

		super(sd.getEntityType(((UriResourcePartTyped) uriResourceItem).getType()), em);
		this.keyPredicates = determineKeyPredicates(uriResourceItem);
		this.association = association;
		if (association == null) {
			throw new IllegalArgumentException("association required");
		}
		this.parentQuery = parent;
		this.subQuery = parent.getQuery().subquery(getJPAEntityType().getKeyType());
		this.queryRoot = subQuery.from(getJPAEntityType().getTypeClass());
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
		final Subquery<T> subQuery = (Subquery<T>) this.subQuery;
		final CriteriaBuilder cb = getCriteriaBuilder();

		try {
			createSelectClause(subQuery);

			Expression<Boolean> whereCondition = null;
			if (this.keyPredicates == null || this.keyPredicates.isEmpty()) {
				whereCondition = createSubqueryWhereByAssociation(parentQuery.getRoot(), queryRoot);
			} else {
				whereCondition = cb.and(
						createWhereByKey(queryRoot, null, this.keyPredicates),
						createSubqueryWhereByAssociation(parentQuery.getRoot(), queryRoot));
			}
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
			return subQuery;
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e, association.getAlias());
		}
	}

	abstract protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException;

	abstract protected <T> void createSelectClause(final Subquery<T> subQuery) throws ODataJPAModelException;

	protected Expression<Boolean> createSubqueryWhereByAssociation(final From<?, ?> parentFrom, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
		final CriteriaBuilder cb = getCriteriaBuilder();
		Expression<Boolean> whereCondition = null;

		final JPAAssociationPath association = getAssociation();
		final boolean joinTableInBetween = association.hasJoinTableBetweenSourceAndTarget();

		if (joinTableInBetween) {
			// trigger complete JOIN expression by JPA for our subselect
			final Path<?> subPath = subRoot
					.join(association.getSourceType().getAssociationByPath(association).getInternalName());
			whereCondition = cb.equal(parentFrom, subPath);
		} else {
			final List<JPASelector> navigationSourceSelectors = determineSourceSelectors();
			final List<JPASelector> navigationTargetSelectors = determineTargetSelectors();
			assert navigationSourceSelectors.size() == navigationTargetSelectors.size();
			for (int index = 0; index < navigationSourceSelectors.size(); index++) {
				Path<?> subPath = subRoot;
				Path<?> parentPath = parentFrom;

				final JPASelector sourceSelector = navigationSourceSelectors.get(index);
				final JPASelector targetSelector = navigationTargetSelectors.get(index);

				if (JPAAssociationPath.class.isInstance(sourceSelector)
						|| JPAAssociationPath.class.isInstance(targetSelector)) {
					// the JPA framework will do the correct things for navigation of n:1 or 1:n
					// property
					subPath = subRoot
							.join(association.getSourceType().getAssociationByPath(association).getInternalName());
					return cb.equal(parentFrom, subPath);
				}
				for (final JPAElement jpaPathElement : sourceSelector.getPathElements()) {
					subPath = subPath.get(jpaPathElement.getInternalName());
				}
				for (final JPAElement jpaPathElement : targetSelector.getPathElements()) {
					parentPath = parentPath.get(jpaPathElement.getInternalName());
				}
				final Expression<Boolean> equalCondition = cb.equal(parentPath, subPath);
				if (whereCondition == null) {
					whereCondition = equalCondition;
				} else {
					whereCondition = cb.and(whereCondition, equalCondition);
				}
			}
		}

		return whereCondition;
	}

	/**
	 *
	 * @return The selectors for the source side (the 'subRoot')
	 */
	abstract protected List<JPASelector> determineSourceSelectors() throws ODataJPAModelException;

	/**
	 *
	 * @return The selectors for the target side (the 'parentFrom')
	 */
	abstract protected List<JPASelector> determineTargetSelectors() throws ODataJPAModelException;

	@Override
	final protected Locale getLocale() {
		return parentQuery.getLocale();
	}

	@Override
	final JPAODataSessionContextAccess getContext() {
		return parentQuery.getContext();
	}
}
