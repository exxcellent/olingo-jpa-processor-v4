package org.apache.olingo.jpa.processor.core.database;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;

public final class JPA_HANADatabaseProcessor extends AbstractJPADatabaseProcessor {

	@Override
	protected Expression<?> concat(final Expression<String> operand1, final Expression<String> operand2) throws ODataApplicationException {
		final CriteriaBuilder cb = getCriteriaBuilder();
		return cb.function("CONCAT", String.class, operand1, operand2);
	}

	@Override
	protected Expression<Boolean> contains(final Expression<String> operand, final String literal)
	        throws ODataApplicationException {
		final CriteriaBuilder cb = getCriteriaBuilder();

		// does not work with EclipseLink and Hibernate :-(
		// // replace 'LIKE' with a better variant
		// final StringBuffer contains = new StringBuffer();
		// contains.append('%');
		// contains.append(literal);
		// contains.append('%');
		// return cb.function("CONTAINS", Boolean.class, operand, cb.literal(contains.toString()));

		// use workaround to produce case-insensitive search
		final StringBuffer contains = new StringBuffer();
		contains.append('%');
		contains.append(literal.toLowerCase());
		contains.append('%');
		final Expression<String> lowerOperand = cb.lower(operand);
		return cb.like(lowerOperand, contains.toString());

	}
}
