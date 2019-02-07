package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

class ODataBuiltinFunctionCallImp implements ODataBuiltinFunctionCall {
	private final MethodKind methodCall;
	private final List<JPAExpressionElement<?>> parameters;
	private final JPAODataDatabaseProcessor converter;

	public ODataBuiltinFunctionCallImp(final JPAODataDatabaseProcessor converter, final MethodKind methodCall,
			final List<JPAExpressionElement<?>> parameters) {
		super();
		this.methodCall = methodCall;
		this.parameters = parameters;
		this.converter = converter;
	}

	@Override
	public Expression<?> get() throws ODataApplicationException {
		return converter.convertBuiltinFunction(methodCall, parameters);
	}

	@Override
	public MethodKind getFunctionKind() {
		return methodCall;
	}

	@Override
	public JPAExpressionElement<?> getParameter(final int index) {
		return parameters.get(index);
	}

	@Override
	public int noParameters() {
		return parameters.size();
	}

}
