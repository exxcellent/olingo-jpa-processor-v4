package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

public interface ODataBuiltinFunctionCall extends JPAOperator<Expression<?>> {

	public MethodKind getFunctionKind();

	public JPAOperator<?> getParameter(int index);

	public int noParameters();

}