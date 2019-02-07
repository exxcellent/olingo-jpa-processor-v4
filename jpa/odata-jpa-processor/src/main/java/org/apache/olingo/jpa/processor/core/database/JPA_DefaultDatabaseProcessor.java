package org.apache.olingo.jpa.processor.core.database;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;

public class JPA_DefaultDatabaseProcessor extends AbstractJPADatabaseProcessor {

	@Override
	protected Expression<?> cast(final Expression<?> value, final EdmType type) throws ODataApplicationException {
		if (EdmPrimitiveTypeKind.String.getFullQualifiedName().equals(type.getFullQualifiedName())) {
			// CONCAT is supported by the most databases... but not by Derby :-(
			// use implicit type cast to produce a string
			final CriteriaBuilder cb = getCriteriaBuilder();
			return cb.function("CONCAT", String.class, value, cb.literal("''"));
		}
		// delegate -> throw exception
		return super.cast(value, type);
	}

}
