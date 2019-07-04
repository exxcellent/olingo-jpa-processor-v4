package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
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
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.queryoption.expression.Binary;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

public class JPAFilterQuery extends JPAAbstractRelationshipQuery<JPANavigationPath, Join<?, ?>> {

	private final JPANavigationFilterProcessor filter;
	private From<?, ?> correlatedRoot;// initialized by constructor calls
	//	private final From<?, ?> subqueryFrom;

	public JPAFilterQuery(final OData odata, final IntermediateServiceDocument sd,
			final UriResourcePartTyped navigationResource, final JPANavigationPath association,
			final JPAAbstractQuery<?, ?> parent, final EntityManager em)
					throws ODataApplicationException {
		super(sd, navigationResource, association, parent, em);
		//		subqueryFrom = getQuery().from(association.getLeaf().getStructuredType().getTypeClass());
		this.filter = null;
	}

	public JPAFilterQuery(final OData odata, final IntermediateServiceDocument sd,
			final UriResourcePartTyped navigationResource, final JPANavigationPath association,
			final JPAAbstractQuery<?, ?> parent, final EntityManager em,
			final VisitableExpression expression) throws ODataApplicationException {
		super(sd, navigationResource, association, parent, em);
		//		subqueryFrom = getQuery().from(association.getLeaf().getStructuredType().getTypeClass());
		// the target of the navigation is the type context for the filter processor
		this.filter = new JPANavigationFilterProcessor(odata, sd, em,
				association.getLeaf().getStructuredType() /* getQueryResultType() */,
				getContext().getDatabaseProcessor(), null, this, expression);
	}

	@Override
	protected Join<?, ?> buildFrom() {
		final From<?, ?> parentFrom = getParentQuery().getRoot();
		//		From<?, ?> correlatedRoot;
		if (parentFrom instanceof Root) {
			correlatedRoot = getQuery().correlate((Root<?>) parentFrom);
		} else {
			// part of another subquery?!
			correlatedRoot = getQuery().correlate((Join<?, ?>) parentFrom);
		}
		final JPASelector association = getSelector();
		From<?, ?> subFrom = correlatedRoot;
		for (final JPAAttribute<?> a : association.getPathElements()) {
			subFrom = subFrom.join(a.getInternalName());
		}
		return (Join<?, ?>) subFrom;
	}

	@Override
	protected Expression<Boolean> createSubqueryWhereByAssociation()
			throws ODataApplicationException, ODataJPAModelException {
		Expression<Boolean> whereCondition = null;

		if (filter != null && getAggregationType(this.filter.getExpression()) == null) {
			try {
				if (filter.getExpression() != null) {
					whereCondition = filter.compile();
				}
			} catch (final ExpressionVisitException e) {
				throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}

		return whereCondition;
	}

	@Override
	protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, final Join<?, ?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {

		if (filter == null) {
			return Collections.emptyList();
		}
		if (getAggregationType(this.filter.getExpression()) == null) {
			return Collections.emptyList();
		}
		final List<Expression<?>> groupByLIst = new LinkedList<>();
		// FIXME
		final List<JPASelector> navigationSourceSelectors = ((JPAAssociationPath) getSelector()).getRightPaths();
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
		return groupByLIst;

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
