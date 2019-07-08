package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 * @author Oliver Grande
 *
 */
public class JPANavigationQuery extends JPAAbstractRelationshipQuery<JPAAssociationPath, Root<?>> {

	public <T extends Object> JPANavigationQuery(final IntermediateServiceDocument sd,
			final UriResourcePartTyped navigationResource, final JPAAssociationPath association,
			final JPAAbstractQuery<?, ?> parent, final EntityManager em)
					throws ODataApplicationException {

		super(sd, navigationResource, association, parent, em);
	}

	@Override
	protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
		// do nothing
		return Collections.emptyList();
	}

	@Override
	protected Expression<Boolean> createSubqueryWhereByAssociation()
			throws ODataApplicationException, ODataJPAModelException {
		final CriteriaBuilder cb = getCriteriaBuilder();

		final JPASelector association = getSelector();
		final From<?, ?> parentFrom = getParentQuery().getRoot();
		final Root<?> subRoot = getRoot();

		From<?, ?> subFrom = subRoot;
		for (final JPAAttribute<?> a : association.getPathElements()) {
			subFrom = subFrom.join(a.getInternalName());
		}

		// the last path element is the relationship of same type as the parent query
		// root and we have to join our subselect with that
		/* final From<?, ?> correlatedRoot = */ getQuery().correlate(subRoot);
		return cb.equal(parentFrom, subFrom);

	}

}
