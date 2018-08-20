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
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
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
			final List<JPAOnConditionItem> conditionItems = association.getJoinConditions();
			if (conditionItems.isEmpty()) {
				return subQuery;
			}
			createSelectClause(subQuery, conditionItems);

			Expression<Boolean> whereCondition = null;
			if (this.keyPredicates == null || this.keyPredicates.isEmpty()) {
				whereCondition = createWhereByAssociation(parentQuery.getRoot(), queryRoot, conditionItems);
			} else {
				whereCondition = cb.and(
						createWhereByKey(queryRoot, null, this.keyPredicates),
						createWhereByAssociation(parentQuery.getRoot(), queryRoot, conditionItems));
			}
			if (childQuery != null) {
				whereCondition = cb.and(whereCondition, cb.exists(childQuery));
			}
			subQuery.where(whereCondition);
			handleAggregation(subQuery, queryRoot, conditionItems);
			return subQuery;
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e, association.getAlias());
		}
	}

	/**
	 * Maybe implemented by sub classes.
	 */
	protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot,
			final List<JPAOnConditionItem> conditionItems) throws ODataApplicationException {}

	@SuppressWarnings("unchecked")
	protected <T> void createSelectClause(final Subquery<T> subQuery, final List<JPAOnConditionItem> conditionItems) {
		Path<?> p = queryRoot;
		if (conditionItems.isEmpty()) {
			throw new IllegalStateException("join conditions required");
		}
		for (final JPAAttribute jpaPathElement : conditionItems.get(0).getLeftPath().getPathElements()) {
			p = p.get(jpaPathElement.getInternalName());
		}
		subQuery.select((Expression<T>) p);
	}

	protected Expression<Boolean> createWhereByAssociation(final From<?, ?> parentFrom, final Root<?> subRoot,
			final List<JPAOnConditionItem> conditionItems) throws ODataApplicationException {
		Expression<Boolean> whereCondition = null;
		for (final JPAOnConditionItem onItem : conditionItems) {
			// 'parentFrom' represents the target of navigation (association), means: the
			// right side
			Path<?> parentPath = parentFrom;
			// do not create a ON condition for a real navigation
			if (!JPAAssociationPath.class.isInstance(onItem.getLeftPath())) {
				for (final JPAElement jpaPathElement : onItem.getRightPath().getPathElements()) {
					parentPath = parentPath.get(jpaPathElement.getInternalName());
				}
			}
			// 'subRoot' is the source of navigation; the left side
			Path<?> subPath = subRoot;
			if (!JPAAssociationPath.class.isInstance(onItem.getRightPath())) {
				for (final JPAElement jpaPathElement : onItem.getLeftPath().getPathElements()) {
					subPath = subPath.get(jpaPathElement.getInternalName());
				}
			}
			final Expression<Boolean> equalCondition = cb.equal(parentPath, subPath);
			if (whereCondition == null) {
				whereCondition = equalCondition;
			} else {
				whereCondition = cb.and(whereCondition, equalCondition);
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
