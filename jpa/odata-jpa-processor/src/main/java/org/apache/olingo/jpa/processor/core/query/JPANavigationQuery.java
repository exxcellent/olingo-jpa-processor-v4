package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;

/**
 * Creates a sub query for a navigation.
 * @author Oliver Grande
 *
 */
public class JPANavigationQuery extends JPAAbstractRelationshipQuery {

	public <T extends Object> JPANavigationQuery(final IntermediateServiceDocument sd, final UriResource uriResourceItem,
			final JPAAbstractQuery<?> parent, final EntityManager em, final JPAAssociationPath association)
					throws ODataApplicationException {

		super(sd, uriResourceItem, parent, em, association);
	}

	/**
	 * Maybe implemented by sub classes.
	 */
	@Override
	protected void handleAggregation(final Subquery<?> subQuery, final Root<?> subRoot)
			throws ODataApplicationException, ODataJPAModelException {
		// do nothing
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> void createSelectClause(final Subquery<T> subQuery) throws ODataJPAModelException {
		// we select from left side of association, because is the 'subRoot'
		final List<JPASelector> conditionItems = getAssociation().getLeftPaths();
		if (conditionItems.isEmpty()) {
			throw new IllegalStateException("join conditions required");
		}
		for (final JPASelector leftSelector : conditionItems) {
			Path<?> p = getRoot();
			for (final JPAAttribute<?> jpaPathElement : leftSelector.getPathElements()) {
				p = p.get(jpaPathElement.getInternalName());
			}
			// TODO loop useless, because only the latest 'select' come into effect?
			subQuery.select((Expression<T>) p);
		}
	}

	@Override
	protected List<JPASelector> determineSourceSelectors() throws ODataJPAModelException {
		// 'subRoot' is the source of navigation; the left side
		return getAssociation().getLeftPaths();
	}

	@Override
	protected List<JPASelector> determineTargetSelectors() throws ODataJPAModelException {
		// 'parentFrom' represents the target of navigation (association), means: the
		// right side
		return getAssociation().getRightPaths();
	}

}
