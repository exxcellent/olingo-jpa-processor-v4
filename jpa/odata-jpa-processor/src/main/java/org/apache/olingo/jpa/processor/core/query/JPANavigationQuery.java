package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
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
public class JPANavigationQuery extends JPAAbstractQuery {
	private final List<UriParameter> keyPredicates;
	private final JPAAssociationPath association;
	private final Root<?> queryRoot;
	private final Subquery<?> subQuery;
	private final JPAAbstractQuery parentQuery;

	public <T extends Object> JPANavigationQuery(final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery parent, final EntityManager em, final JPAAssociationPath association)
					throws ODataApplicationException {

		super(sd, (EdmEntityType) ((UriResourcePartTyped) uriResourceItem).getType(), em);
		this.keyPredicates = determineKeyPredicates(uriResourceItem);
		this.association = association;
		this.parentQuery = parent;
		this.subQuery = parent.getQuery().subquery(this.jpaEntityType.getKeyType());
		this.queryRoot = subQuery.from(this.jpaEntityType.getTypeClass());
		this.locale = parent.getLocale();
	}

	protected JPAAssociationPath getAssociation() {
		return association;
	}

	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Root<?> getRoot() {
		assert queryRoot != null;
		return queryRoot;
	}

	@Override
	public AbstractQuery<?> getQuery() {
		return subQuery;
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> Subquery<T> getSubQueryExists(final Subquery<?> childQuery)
			throws ODataApplicationException {
		final Subquery<T> subQuery = (Subquery<T>) this.subQuery;

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
				whereCondition = cb.and(whereCondition, cb.exists(childQuery));
			}
			subQuery.where(whereCondition);
			handleAggregation(subQuery, queryRoot);
			return subQuery;
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e, association.getAlias());
		}
	}

	/**
	 * Maybe implemented by sub classes.
	 */
	protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
	}

	@SuppressWarnings("unchecked")
	protected <T> void createSelectClause(final Subquery<T> subQuery) throws ODataJPAModelException {
		final List<JPASelector> conditionItems = getAssociation().getLeftPaths();
		if (conditionItems.isEmpty()) {
			throw new IllegalStateException("join conditions required");
		}
		for (final JPASelector leftSelector : conditionItems) {
			Path<?> p = queryRoot;
			for (final JPAAttribute jpaPathElement : leftSelector.getPathElements()) {
				p = p.get(jpaPathElement.getInternalName());
			}
			subQuery.select((Expression<T>) p);
		}
	}

	protected Expression<Boolean> createSubqueryWhereByAssociation(final From<?, ?> parentFrom, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
		Expression<Boolean> whereCondition = null;
		//		final List<JPAOnConditionItem> conditionItems = getAssociation().getJoinConditions();// FIXME
		//		for (final JPAOnConditionItem onItem : conditionItems) {
		//			// the right side path maybe NULL in case of a @JoinTable (of a 1:n or m:n
		//			// relationship)
		//			final boolean joinTableInBetween = association.hasJoinTableBetweenSourceAndTarget();
		//			if (onItem.getRightPath() == null && !joinTableInBetween) {
		//				throw new IllegalStateException(
		//						"Missing right join condition is only allowed for a relationship with @JoinTable between: "
		//								+ association.getSourceType().getExternalName() + "#" + association.getAlias());
		//			}
		//			// 'parentFrom' represents the target of navigation (association), means: the
		//			// right side
		//			Path<?> parentPath = parentFrom;
		//			// do not create a ON condition for a real navigation
		//			if (!JPAAssociationPath.class.isInstance(onItem.getLeftPath())) {
		//				for (final JPAElement jpaPathElement : onItem.getRightPath().getPathElements()) {
		//					parentPath = parentPath.get(jpaPathElement.getInternalName());
		//				}
		//			}
		//			// 'subRoot' is the source of navigation; the left side
		//			Path<?> subPath = subRoot;
		//			if (!JPAAssociationPath.class.isInstance(onItem.getRightPath())) {
		//				if (onItem.getRightPath() == null) {
		//					// trigger complete JOIN expression by JPA for our subselect
		//					subPath = subRoot
		//							.join(association.getSourceType().getAssociationByPath(association).getInternalName());
		//				} else {
		//					for (final JPAElement jpaPathElement : onItem.getLeftPath().getPathElements()) {
		//						subPath = subPath.get(jpaPathElement.getInternalName());
		//					}
		//				}
		//			}
		//			final Expression<Boolean> equalCondition = cb.equal(parentPath, subPath);
		//			if (whereCondition == null) {
		//				whereCondition = equalCondition;
		//			} else {
		//				whereCondition = cb.and(whereCondition, equalCondition);
		//			}
		//		}

		final List<JPASelector> leftSelectors = getAssociation().getLeftPaths();
		final List<JPASelector> rightSelectors = getAssociation().getRightPaths();
		for (final JPASelector right : rightSelectors) {
			// 'parentFrom' represents the target of navigation (association), means: the
			// right side
			Path<?> parentPath = parentFrom;
			// do not create a ON condition for a real navigation
			if (!JPAAssociationPath.class.isInstance(right)) {
				for (final JPAElement jpaPathElement : right.getPathElements()) {
					parentPath = parentPath.get(jpaPathElement.getInternalName());
				}
			}
		}
		final boolean joinTableInBetween = association.hasJoinTableBetweenSourceAndTarget();
		if (joinTableInBetween) {
			Path<?> subPath = subRoot;
			// trigger complete JOIN expression by JPA for our subselect
			subPath = subRoot.join(association.getSourceType().getAssociationByPath(association).getInternalName());
			whereCondition = cb.equal(parentFrom, subPath);
		} else {
			for (final JPASelector left : leftSelectors) {
				// 'subRoot' is the source of navigation; the left side
				Path<?> subPath = subRoot;
				for (final JPAElement jpaPathElement : left.getPathElements()) {
					subPath = subPath.get(jpaPathElement.getInternalName());
				}
				final Expression<Boolean> equalCondition = cb.equal(parentFrom, subPath);
				if (whereCondition == null) {
					whereCondition = equalCondition;
				} else {
					whereCondition = cb.and(whereCondition, equalCondition);
				}
			}
		}

		return whereCondition;
	}

	@Override
	protected Locale getLocale() {
		return locale;
	}

	@Override
	JPAODataSessionContextAccess getContext() {
		return parentQuery.getContext();
	}
}
