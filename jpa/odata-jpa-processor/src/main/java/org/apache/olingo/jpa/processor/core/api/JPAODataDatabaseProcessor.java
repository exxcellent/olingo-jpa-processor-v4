package org.apache.olingo.jpa.processor.core.api;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;

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
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

public interface JPAODataDatabaseProcessor {

	public CriteriaBuilder getCriteriaBuilder();

	public <T extends Number> Expression<T> convert(final JPAArithmeticOperator jpaOperator)
			throws ODataApplicationException;

	public Expression<Boolean> convert(final JPABooleanOperator jpaOperator) throws ODataApplicationException;

	public <T extends Comparable<T>> Expression<Boolean> convert(final JPAComparisonOperator<T> jpaOperator)
			throws ODataApplicationException;

	public Expression<?> convertBuiltinFunction(final MethodKind methodCall,
			final List<JPAExpressionElement<?>> parameters) throws ODataApplicationException;

	public Expression<Boolean> convert(final JPAUnaryBooleanOperator jpaOperator) throws ODataApplicationException;

	public Expression<Long> convert(final JPAAggregationOperation jpaOperator) throws ODataApplicationException;

	/**
	 * Search at OData:
	 * <p>
	 * <a href=
	 * "http://docs.oasis-open.org/odata/odata/v4.0/os/part1-protocol/odata-v4.0-os-part1-protocol.html#_Toc372793700">
	 * OData Version 4.0 Part 1 - 11.2.5.6 System Query Option $search</a>
	 * <p>
	 * <a href=
	 * "http://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html#_Toc372793865">
	 * OData Version 4.0 Part 2 - 5.1.7 System Query Option $search</a>
	 *
	 * @param cb
	 * @param cq
	 * @param from         The database table representing the entity to search in.
	 * @param entityType
	 * @param searchOption
	 * @return
	 * @throws ODataApplicationException
	 * @Deprecated The logic what to do for $search should not be implemented by the
	 *             database processor, we have to find another solution where the
	 *             logic is outside.
	 */
	@Deprecated
	Expression<Boolean> createSearchWhereClause(CriteriaBuilder cb, CriteriaQuery<?> cq, From<?, ?> from,
			JPAEntityType entityType, SearchOption searchOption) throws ODataApplicationException;

	List<?> executeFunctionQuery(UriResourceFunction uriResourceFunction, JPAFunction jpaFunction,
			JPAEntityType returnType, EntityManager em) throws ODataApplicationException;

}
