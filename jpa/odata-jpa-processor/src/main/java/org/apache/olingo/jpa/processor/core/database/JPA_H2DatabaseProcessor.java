package org.apache.olingo.jpa.processor.core.database;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;

public class JPA_H2DatabaseProcessor extends JPA_DefaultDatabaseProcessor {

	@Override
	protected Expression<?> cast(final Expression<?> value, final EdmType type) throws ODataApplicationException {
		if (EdmPrimitiveTypeKind.Date.getFullQualifiedName().equals(type.getFullQualifiedName())) {
			return date(value);
		}
		return super.cast(value, type);
	}

	@Override
	protected Expression<?> date(final Expression<?> value) throws ODataApplicationException {
		final CriteriaBuilder cb = getCriteriaBuilder();
		return cb.function("PARSEDATETIME", value.getJavaType(), value, cb.literal("YYYY-MM-DD"));
	}
}
