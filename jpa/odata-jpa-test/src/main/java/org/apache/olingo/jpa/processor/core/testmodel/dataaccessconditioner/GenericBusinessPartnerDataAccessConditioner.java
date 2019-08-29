package org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.server.api.ODataApplicationException;

public class GenericBusinessPartnerDataAccessConditioner implements DataAccessConditioner<GenericBusinessPartner> {

	public enum SelectionStrategy {
		ALL, OnlyPersons, OnlyOrganizations;
	}

	/**
	 * Can be changed by tests to produce changing WHERE expressions.
	 */
	public static SelectionStrategy SelectStrategy = SelectionStrategy.ALL;

	@Inject
	private EntityManager injectedEntityManager;

	@Override
	public Expression<Boolean> buildSelectCondition(final EntityManager em, final Root<GenericBusinessPartner> from)
	        throws ODataApplicationException {
		if (injectedEntityManager == null)
			throw new IllegalStateException("EntityManager not injected");
		if (injectedEntityManager != em)
			throw new IllegalStateException("Given EntityManager not the same as injected one");
		final CriteriaBuilder cb = em.getCriteriaBuilder();
		switch (SelectStrategy) {
		case ALL:
			return null;
		case OnlyPersons:
			return cb.equal(from.get("type"), "1");
		case OnlyOrganizations:
			return cb.equal(from.get("type"), "2");
		default:
			throw new UnsupportedOperationException();
		}
	}

}
