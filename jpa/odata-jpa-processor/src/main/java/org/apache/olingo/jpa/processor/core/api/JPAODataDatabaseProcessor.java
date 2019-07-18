package org.apache.olingo.jpa.processor.core.api;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.processor.core.filter.JPAAggregationOperation;
import org.apache.olingo.jpa.processor.core.filter.JPAArithmeticOperator;
import org.apache.olingo.jpa.processor.core.filter.JPABooleanOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAComparisonOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAExpressionElement;
import org.apache.olingo.jpa.processor.core.filter.JPAUnaryBooleanOperator;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

public interface JPAODataDatabaseProcessor {

	public <T extends Number> Expression<T> convert(final JPAArithmeticOperator jpaOperator)
			throws ODataApplicationException;

	public Expression<Boolean> convert(final JPABooleanOperator jpaOperator) throws ODataApplicationException;

	public <T extends Comparable<T>> Expression<Boolean> convert(final JPAComparisonOperator<T> jpaOperator)
			throws ODataApplicationException;

	public Expression<?> convertBuiltinFunction(final MethodKind methodCall,
			final List<JPAExpressionElement<?>> parameters) throws ODataApplicationException;

	public Expression<Boolean> convert(final JPAUnaryBooleanOperator jpaOperator) throws ODataApplicationException;

	public Expression<Long> convert(final JPAAggregationOperation jpaOperator) throws ODataApplicationException;

	List<?> executeFunctionQuery(UriResourceFunction uriResourceFunction, JPAFunction jpaFunction,
			JPAEntityType returnType, EntityManager em) throws ODataApplicationException;

}
