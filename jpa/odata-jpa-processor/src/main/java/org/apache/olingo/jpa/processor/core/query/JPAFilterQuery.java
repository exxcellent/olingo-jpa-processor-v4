package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.filter.JPAFilterExpression;
import org.apache.olingo.jpa.processor.core.filter.JPAMemberOperator;
import org.apache.olingo.jpa.processor.core.filter.JPANavigationFilterProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

public class JPAFilterQuery extends JPAAbstractRelationshipQuery {

	private final JPANavigationFilterProcessor filter;

	public JPAFilterQuery(final OData odata, final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery<?> parent, final EntityManager em,
			final JPAAssociationPath association)
					throws ODataApplicationException {
		super(sd, uriResourceItem, parent, em, association);
		this.filter = null;
	}

	public JPAFilterQuery(final OData odata, final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery<?> parent, final EntityManager em,
			final JPAAssociationPath association,
			final VisitableExpression expression) throws ODataApplicationException {
		super(sd, uriResourceItem, parent, em, association);
		this.filter = new JPANavigationFilterProcessor(odata, sd, em, getQueryResultType(),
				getContext().getDatabaseProcessor(), null, this, expression);
	}

	private Expression<Boolean> createSubqueryWhereByAssociationPart()
			throws ODataApplicationException, ODataJPAModelException {
		final CriteriaBuilder cb = getCriteriaBuilder();

		final JPAAssociationPath association = getAssociation();
		final Root<?> parentFrom = getParentQuery().getRoot();
		final Root<?> subRoot = getRoot();

		// we have to start navigation from source (using the parent query root) to join
		// association target (subquery root)
		final Root<?> correlatedRoot = getQuery().correlate(parentFrom);
		From<?, ?> subFrom = correlatedRoot;
		for (final JPAAttribute<?> a : association.getPathElements()) {
			subFrom = subFrom.join(a.getInternalName());
		}

		return cb.equal(subFrom, subRoot);
	}

	@Override
	protected Expression<Boolean> createSubqueryWhereByAssociation()
			throws ODataApplicationException, ODataJPAModelException {
		Expression<Boolean> whereCondition = createSubqueryWhereByAssociationPart();

		if (filter != null && getAggregationType(this.filter.getExpression()) == null) {
			try {
				if (filter.getExpression() != null) {
					if (whereCondition != null) {
						whereCondition = getCriteriaBuilder().and(whereCondition, filter.compile());
					} else {
						whereCondition = filter.compile();
					}
				}
			} catch (final ExpressionVisitException e) {
				throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}

		return whereCondition;
	}

	@Override
	protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {

		if (filter == null) {
			return;
		}
		if (getAggregationType(this.filter.getExpression()) == null) {
			return;
		}
		final List<Expression<?>> groupByLIst = new ArrayList<Expression<?>>();
		// 'subRoot' is the source of navigation; the left side
		final List<JPASelector> navigationSourceSelectors = getAssociation().getRightPaths();
		for (int index = 0; index < navigationSourceSelectors.size(); index++) {
			Path<?> subPath = subRoot;
			final JPASelector sourceSelector = navigationSourceSelectors.get(index);
			for (final JPAElement jpaPathElement : sourceSelector.getPathElements()) {
				subPath = subPath.get(jpaPathElement.getInternalName());
			}
			groupByLIst.add(subPath);
		}
		subQuery.groupBy(groupByLIst);
		try {
			subQuery.having(this.filter.compile());
		} catch (final ExpressionVisitException e) {
			throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}

	}

	private UriResourceKind getAggregationType(final VisitableExpression expression) {
		UriInfoResource member = null;
		if (expression != null && expression instanceof Binary) {
			if (((Binary) expression).getLeftOperand() instanceof JPAMemberOperator) {
				member = ((JPAMemberOperator) ((Binary) expression).getLeftOperand()).getMember().getResourcePath();
			} else if (((Binary) expression).getRightOperand() instanceof JPAMemberOperator) {
				member = ((JPAMemberOperator) ((Binary) expression).getRightOperand()).getMember().getResourcePath();
			}
		} else if (expression != null && expression instanceof JPAFilterExpression) {
			member = ((JPAFilterExpression) expression).getMember();
		}

		if (member != null) {
			for (final UriResource r : member.getUriResourceParts()) {
				if (r.getKind() == UriResourceKind.count) {
					return r.getKind();
				}
			}
		}
		return null;
	}
}
