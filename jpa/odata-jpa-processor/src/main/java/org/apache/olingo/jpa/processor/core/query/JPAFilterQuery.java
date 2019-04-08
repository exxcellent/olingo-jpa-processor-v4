package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
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
			final JPAAbstractQuery<?> parent, final EntityManager em, final JPAAssociationPath association)
					throws ODataApplicationException {
		super(sd, uriResourceItem, parent, em, association);
		this.filter = null;
	}

	public JPAFilterQuery(final OData odata, final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery<?> parent, final EntityManager em, final JPAAssociationPath association,
			final VisitableExpression expression) throws ODataApplicationException {
		super(sd, uriResourceItem, parent, em, association);
		this.filter = new JPANavigationFilterProcessor(odata, sd, em, getJPAEntityType(),
				getContext().getDatabaseProcessor(), null, this, expression);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> void createSelectClause(final Subquery<T> subQuery) throws ODataJPAModelException {
		// the order of 'root' entity and 'subquery' entity is switched for $filter, so
		// we have to select the right to get the correct selection columns
		final List<JPASelector> conditionItems = getAssociation().getRightPaths();
		for (final JPASelector rightSelector : conditionItems) {
			Path<?> p = getRoot();
			for (final JPAAttribute<?> jpaPathElement : rightSelector.getPathElements()) {
				p = p.get(jpaPathElement.getInternalName());
			}
			// TODO loop useless, because only the latest 'select' come into effect?
			subQuery.select((Expression<T>) p);
		}

	}

	@Override
	protected Expression<Boolean> createSubqueryWhereByAssociation(final From<?, ?> parentFrom, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
		Expression<Boolean> whereCondition = super.createSubqueryWhereByAssociation(parentFrom, subRoot);

		if (filter != null && getAggregationType(this.filter.getExpressionMember()) == null) {
			try {
				if (filter.getExpressionMember() != null) {
					whereCondition = getCriteriaBuilder().and(whereCondition, filter.compile());
				}
			} catch (final ExpressionVisitException e) {
				throw new ODataJPAQueryException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		}

		return whereCondition;
	}

	@Override
	protected List<JPASelector> determineSourceSelectors() throws ODataJPAModelException {
		// 'parentFrom' represents the target of navigation (association), means: the
		// right side
		return getAssociation().getRightPaths();
	}

	@Override
	protected List<JPASelector> determineTargetSelectors() throws ODataJPAModelException {
		// 'subRoot' is the source of navigation; the left side
		return getAssociation().getLeftPaths();
	}


	@Override
	protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {

		if (filter == null) {
			return;
		}
		if (getAggregationType(this.filter.getExpressionMember()) == null) {
			return;
		}
		final List<Expression<?>> groupByLIst = new ArrayList<Expression<?>>();
		final List<JPASelector> navigationSourceSelectors = determineSourceSelectors();
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
