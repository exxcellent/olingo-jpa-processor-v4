package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;

public class JPALiteralTypeOperand implements JPAExpressionElement<EdmType> {
	private final EdmType type;

	public JPALiteralTypeOperand(final EdmType type) {
		this.type = type;
	}

	@Override
	public EdmType get() throws ODataApplicationException {
		return type;
	}

}
