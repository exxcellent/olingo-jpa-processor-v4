package org.apache.olingo.jpa.processor.core.api;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.processor.core.filter.JPAAggregationOperation;
import org.apache.olingo.jpa.processor.core.filter.JPAArithmeticOperator;
import org.apache.olingo.jpa.processor.core.filter.JPABooleanOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAExpressionElement;
import org.apache.olingo.jpa.processor.core.filter.JPAUnaryBooleanOperator;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;

public interface JPAODataDatabaseProcessor {

	/**
	 * Database specific functionality to implement $search from OData specification.
	 *
	 * @param search
	 *            The earch term given by client.
	 * @param searchColumns
	 *            The list of columns to inspect for search term
	 * @return The WHERE condition part containing the expression.
	 * @throws ODataApplicationException
	 */
	public Expression<Boolean> createSearchExpression(SearchTerm search, List<Path<?>> searchColumns)
	        throws ODataApplicationException;

	public <T extends Number> Expression<T> convert(final JPAArithmeticOperator jpaOperator)
	        throws ODataApplicationException;

	public Expression<Boolean> convert(final JPABooleanOperator jpaOperator) throws ODataApplicationException;

	/**
	 *
	 * @param operator
	 * @param operand1
	 *            Depending on operator maybe <code>null</code> for 'IS NULL' check
	 * @param operand2
	 *            Depending on operator maybe <code>null</code> for 'IS NULL' check
	 * @return
	 * @throws ODataApplicationException
	 */
	public <Y extends Comparable<? super Y>> Expression<Boolean> createComparison(BinaryOperatorKind operator, Expression<Y> operand1,
	        Expression<Y> operand2)
	        throws ODataApplicationException;

	public Expression<?> convertBuiltinFunction(final MethodKind methodCall,
	        final List<JPAExpressionElement<?>> parameters) throws ODataApplicationException;

	public Expression<Boolean> convert(final JPAUnaryBooleanOperator jpaOperator) throws ODataApplicationException;

	public Expression<Long> convert(final JPAAggregationOperation jpaOperator) throws ODataApplicationException;

	List<?> executeFunctionQuery(UriResourceFunction uriResourceFunction, JPAFunction jpaFunction,
	        JPAEntityType returnType, EntityManager em) throws ODataApplicationException;

}
