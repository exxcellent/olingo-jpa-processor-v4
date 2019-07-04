package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 *
 * @author Oliver Grande
 *
 */
public abstract class JPAAbstractRelationshipQuery<S extends JPASelector, QT extends From<?, ?>>
extends JPAAbstractQuery<Subquery<Integer>, Integer> {

	private final List<UriParameter> keyPredicates;
	private final S selector;
	private final QT queryRoot;
	private final Subquery<Integer> subQuery;
	private final JPAAbstractQuery<?, ?> parentCall;

	public <T extends Object> JPAAbstractRelationshipQuery(final IntermediateServiceDocument sd,
			final UriResourcePartTyped navigationResource, final S navigationPath,
			final JPAAbstractQuery<?, ?> parent, final EntityManager em)
					throws ODataApplicationException {
		super(sd.getEntityType(navigationResource.getType()), em);
		this.keyPredicates = determineKeyPredicates(navigationResource);
		this.selector = navigationPath;
		if (selector == null) {
			throw new IllegalArgumentException("selector required");
		}
		this.parentCall = parent;
		this.subQuery = parent.getQuery().subquery(Integer.class);// we select always '1'
		this.queryRoot = buildFrom();
	}

	/**
	 *
	 * @return The {@link javax.persistence.criteria.Root Root} or
	 *         {@link javax.persistence.criteria.Join Join} used as primary
	 *         selection type scope of query.
	 */
	@SuppressWarnings("unchecked")
	protected QT buildFrom() {
		return (QT) subQuery.from(getQueryResultType().getTypeClass());
	}

	protected S getSelector() {
		return selector;
	}

	protected JPAAbstractQuery<?, ?> getParentQuery() {
		return parentCall;
	}

	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	final public QT getRoot() {
		assert queryRoot != null;
		return queryRoot;
	}

	@Override
	final public Subquery<Integer> getQuery() {
		return subQuery;
	}

	@SuppressWarnings("unchecked")
	public final <T extends Object> Subquery<T> getSubQueryExists(final Subquery<?> childQuery)
			throws ODataApplicationException {
		final CriteriaBuilder cb = getCriteriaBuilder();

		try {

			final Expression<Boolean> subqueryWhere = createSubqueryWhereByAssociation();

			Expression<Boolean> whereCondition = extendWhereByKey(queryRoot, subqueryWhere, this.keyPredicates);
			if (childQuery != null) {
				if (whereCondition != null) {
					whereCondition = cb.and(whereCondition, cb.exists(childQuery));
				} else {
					whereCondition = cb.exists(childQuery);
				}
			}
			if (whereCondition != null) {
				subQuery.where(whereCondition);
			}
			// Warning: EclipseLink will produce an invalid query if we have a 'group by'
			// without SELECT the column in that 'group by', Hibernate is working properly
			final List<Expression<?>> groupByColumns = handleAggregation(subQuery, queryRoot);
			if (groupByColumns.isEmpty()) {
				// EXISTS subselect needs only a marker select for existence
				((Subquery<T>) getQuery()).select((Expression<T>) getCriteriaBuilder().literal(Integer.valueOf(1)));
			} else if (groupByColumns.size() == 1) {
				// good case
				((Subquery<T>) getQuery()).select((Expression<T>) groupByColumns.get(0));
			} else {
				// a subquery can select only one column, so we have a problem...
				LOG.log(Level.SEVERE,
						"This subquery is using a 'group by' with multiple columns, but can select only one... take the first one only!");
				((Subquery<T>) getQuery()).select((Expression<T>) groupByColumns.get(0));
			}
			return (Subquery<T>) subQuery;
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_NAVI_PROPERTY_UNKNOWN,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e, selector.getAlias());
		}
	}

	/**
	 *
	 * @return The list of expressions used in 'group by', maybe empty, but not
	 *         <code>null</code>.
	 */
	abstract protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, final QT subRoot)
			throws ODataApplicationException, ODataJPAModelException;

	/**
	 *
	 * @return A WHERE condition or <code>null</code> if no explicit condition is
	 *         required (in case of correlated join for example)
	 */
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
