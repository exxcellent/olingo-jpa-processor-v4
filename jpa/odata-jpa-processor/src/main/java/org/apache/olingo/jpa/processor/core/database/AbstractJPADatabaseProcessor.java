package org.apache.olingo.jpa.processor.core.database;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;

import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.filter.JPAAggregationOperation;
import org.apache.olingo.jpa.processor.core.filter.JPAArithmeticOperator;
import org.apache.olingo.jpa.processor.core.filter.JPABooleanOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAComparisonOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAExpressionElement;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralOperator;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralTypeOperator;
import org.apache.olingo.jpa.processor.core.filter.JPAUnaryBooleanOperator;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

/**
 * The base class of all data base processor. This class will handle the life
 * cycle including the stateful properties.
 *
 * @author Ralf Zozmann
 *
 */
public abstract class AbstractJPADatabaseProcessor implements JPAODataDatabaseProcessor {
	private static final String SELECT_BASE_PATTERN = "SELECT * FROM $FUNCTIONNAME$($PARAMETER$)";
	private static final String FUNC_NAME_PLACEHOLDER = "$FUNCTIONNAME$";
	private static final String PARAMETER_PLACEHOLDER = "$PARAMETER$";

	private CriteriaBuilder cb = null;

	protected AbstractJPADatabaseProcessor() {
		super();
	}

	public final void initialize(final CriteriaBuilder cb) {
		this.cb = cb;
	}

	@Override
	public final CriteriaBuilder getCriteriaBuilder() {
		if (cb == null) {
			throw new IllegalStateException("Call initialize() before to prepare cirteria builder");
		}
		return cb;
	}

	@Override
	public Expression<Long> convert(final JPAAggregationOperation jpaOperator) throws ODataApplicationException {
		switch (jpaOperator.getAggregation()) {
		case COUNT:
			return cb.count(jpaOperator.getPath());
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getAggregation().name());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Number> Expression<T> convert(final JPAArithmeticOperator jpaOperator) throws ODataApplicationException {
		switch (jpaOperator.getOperator()) {
		case ADD:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return (Expression<T>) cb.sum(jpaOperator.getLeft(cb), jpaOperator.getRightAsNumber(cb));
			} else {
				return (Expression<T>) cb.sum(jpaOperator.getLeft(cb), jpaOperator.getRightAsExpression());
			}
		case SUB:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return (Expression<T>) cb.diff(jpaOperator.getLeft(cb), jpaOperator.getRightAsNumber(cb));
			} else {
				return (Expression<T>) cb.diff(jpaOperator.getLeft(cb), jpaOperator.getRightAsExpression());
			}
		case DIV:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return (Expression<T>) cb.quot(jpaOperator.getLeft(cb), jpaOperator.getRightAsNumber(cb));
			} else {
				return (Expression<T>) cb.quot(jpaOperator.getLeft(cb), jpaOperator.getRightAsExpression());
			}
		case MUL:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return (Expression<T>) cb.prod(jpaOperator.getLeft(cb), jpaOperator.getRightAsNumber(cb));
			} else {
				return (Expression<T>) cb.prod(jpaOperator.getLeft(cb), jpaOperator.getRightAsExpression());
			}
		case MOD:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return (Expression<T>) cb.mod(jpaOperator.getLeftAsIntExpression(),
						new Integer(jpaOperator.getRightAsNumber(cb).toString()));
			} else {
				return (Expression<T>) cb.mod(jpaOperator.getLeftAsIntExpression(),
						jpaOperator.getRightAsIntExpression());
			}
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getOperator().name());
		}
	}

	@Override
	public Expression<Boolean> convert(final JPABooleanOperator jpaOperator) throws ODataApplicationException {
		switch (jpaOperator.getOperator()) {
		case AND:
			return cb.and(jpaOperator.getLeft(), jpaOperator.getRight());
		case OR:
			return cb.or(jpaOperator.getLeft(), jpaOperator.getRight());
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getOperator().name());
		}
	}

	// TODO check generics!
	@Override
	public <T extends Comparable<T>> Expression<Boolean> convert(final JPAComparisonOperator<T> jpaOperator)
			throws ODataApplicationException {
		switch (jpaOperator.getOperator()) {
		case EQ:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				final JPALiteralOperator right = (JPALiteralOperator) jpaOperator.getRight();
				if (right.isNullLiteral()) {
					return cb.isNull(jpaOperator.getLeft());
				} else {
					return cb.equal(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
				}
			} else {
				return cb.equal(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		case NE:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return cb.notEqual(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
			} else {
				return cb.notEqual(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		case GE:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return cb.greaterThanOrEqualTo(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
			} else {
				return cb.greaterThanOrEqualTo(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		case GT:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return cb.greaterThan(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
			} else {
				return cb.greaterThan(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		case LT:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return cb.lessThan(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
			} else {
				return cb.lessThan(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		case LE:
			if (jpaOperator.getRight() instanceof JPALiteralOperator) {
				return cb.lessThanOrEqualTo(jpaOperator.getLeft(), jpaOperator.getRightAsComparable());
			} else {
				return cb.lessThanOrEqualTo(jpaOperator.getLeft(), jpaOperator.getRightAsExpression());
			}
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getOperator().name());
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public Expression<?> convertBuiltinFunction(final MethodKind methodCall,
			final List<JPAExpressionElement<?>> parameters)
					throws ODataApplicationException {
		switch (methodCall) {
		// First String functions
		case LENGTH:
			return cb.length((Expression<String>) (parameters.get(0).get()));
		case CONTAINS:
			final StringBuffer contains = new StringBuffer();
			contains.append('%');
			contains.append((String) ((JPALiteralOperator) parameters.get(1)).getLiteralValue());
			contains.append('%');
			return cb.like((Expression<String>) (parameters.get(0).get()), contains.toString());
		case ENDSWITH:
			final StringBuffer ends = new StringBuffer();
			ends.append('%');
			ends.append((String) ((JPALiteralOperator) parameters.get(1)).getLiteralValue());
			return cb.like((Expression<String>) (parameters.get(0).get()), ends.toString());
		case STARTSWITH:
			final StringBuffer starts = new StringBuffer();
			starts.append((String) ((JPALiteralOperator) parameters.get(1)).getLiteralValue());
			starts.append('%');
			return cb.like((Expression<String>) (parameters.get(0).get()), starts.toString());
		case INDEXOF:
			final String searchString = ((String) ((JPALiteralOperator) parameters.get(1)).getLiteralValue());
			return cb.locate((Expression<String>) (parameters.get(0).get()), searchString);
		case SUBSTRING:
			// OData defines start position in SUBSTRING as 0 (see
			// http://docs.oasis-open.org/odata/odata/v4.0/os/part2-url-conventions/odata-v4.0-os-part2-url-conventions.html#_Toc372793820)
			// SQL databases respectively use 1 as start position of a string

			final Expression<Integer> start = convertLiteralToExpression(parameters.get(1), 1);
			// final Integer start = new Integer(((JPALiteralOperator)
			// jpaFunction.getParameter(1)).get().toString()) + 1;
			if (parameters.size() == 3) {
				final Expression<Integer> length = convertLiteralToExpression(parameters.get(2), 0);
				return cb.substring((Expression<String>) (parameters.get(0).get()), start, length);
			} else {
				return cb.substring((Expression<String>) (parameters.get(0).get()), start);
			}

		case TOLOWER:
			final Expression<String> tolower;
			if (parameters.get(0) instanceof JPALiteralOperator) {
				tolower = cb.literal((String) parameters.get(0).get());
			} else {
				tolower = (Expression<String>) (parameters.get(0).get());
			}
			return cb.lower(tolower);
		case TOUPPER:
			final Expression<String> toupper = (Expression<String>) (parameters.get(0).get());
			return cb.upper(toupper);
		case TRIM:
			return cb.trim((Expression<String>) (parameters.get(0).get()));
		case CONCAT:
			if (parameters.get(0).get() instanceof String) {
				return cb.concat((String) parameters.get(0).get(), (Expression<String>) (parameters.get(1).get()));
			}
			if (parameters.get(1).get() instanceof String) {
				return cb.concat((Expression<String>) (parameters.get(0).get()), (String) parameters.get(1).get());
			} else {
				return cb.concat((Expression<String>) (parameters.get(0).get()),
						(Expression<String>) (parameters.get(1).get()));
			}
		case CAST:
			if (parameters.size() != 2) {
				throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
						HttpStatusCode.BAD_REQUEST, methodCall.name());
			}
			final Expression<?> valueOperand = (Expression<?>) parameters.get(0).get();
			final JPALiteralTypeOperator typeOperand = (JPALiteralTypeOperator) parameters.get(1);
			return cast(valueOperand, typeOperand.get());
			// Second Date-Time functions
		case NOW:
			return cb.currentTimestamp();
		case TIME:
			// extract time part from a timestamp
			if (parameters.size() != 1) {
				throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
						HttpStatusCode.BAD_REQUEST, methodCall.name());
			}
			return time((Expression<?>) parameters.get(0).get());
		case DATE:
			// extract date part from a timestamp
			if (parameters.size() != 1) {
				throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
						HttpStatusCode.BAD_REQUEST, methodCall.name());
			}
			return date((Expression<?>) parameters.get(0).get());
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, methodCall.name());
		}
	}

	/**
	 *
	 * @param value The expression to cast into a TIME value.
	 * @return the criteria API expression casting/converting the given expression
	 *         value into a TIME.
	 * @throws ODataApplicationException
	 * @see {@link MethodKind#TIME}
	 */
	protected Expression<?> time(final Expression<?> value) throws ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
				HttpStatusCode.NOT_IMPLEMENTED, MethodKind.TIME.name());
	}

	/**
	 *
	 * @param value The expression to cast into a DATE value.
	 * @return the criteria API expression casting/converting the given expression
	 *         value into a DATE.
	 * @throws ODataApplicationException
	 * @see {@link MethodKind#DATE}
	 */
	protected Expression<?> date(final Expression<?> value) throws ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
				HttpStatusCode.NOT_IMPLEMENTED, MethodKind.DATE.name());
	}

	/**
	 * Cast a value into another one with database SQL terms.
	 *
	 * @param value The expression or literal to cast.
	 * @param type  The target type to cast to.
	 * @return The criteria API expression to cast the given <i>value</i> into the
	 *         requested type.
	 * @throws ODataApplicationException
	 * @see {@link MethodKind#CAST}
	 */
	protected Expression<?> cast(final Expression<?> value, final EdmType type) throws ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
				HttpStatusCode.NOT_IMPLEMENTED, MethodKind.CAST.name());
	}

	@Override
	public Expression<Boolean> convert(final JPAUnaryBooleanOperator jpaOperator) throws ODataApplicationException {
		switch (jpaOperator.getOperator()) {
		case NOT:
			return cb.not(jpaOperator.getOperand());
		default:
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getOperator().name());
		}
	}

	@Override
	public Expression<Boolean> createSearchWhereClause(final CriteriaBuilder cb, final CriteriaQuery<?> cq,
			final From<?, ?> root, final JPAEntityType entityType, final SearchOption searchOption)
							throws ODataApplicationException {
		throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.NOT_SUPPORTED_SEARCH,
				HttpStatusCode.NOT_IMPLEMENTED);
	}

	@Override
	public List<?> executeFunctionQuery(final UriResourceFunction uriResourceFunction, final JPAFunction jpaFunction,
			final JPAEntityType returnType, final EntityManager em) throws ODataApplicationException {

		final String queryString = generateQueryString(jpaFunction);
		final Query functionQuery = em.createNativeQuery(queryString, returnType.getTypeClass());
		int count = 1;
		for (final JPAOperationParameter parameter : jpaFunction.getParameter()) {
			final UriParameter uriParameter = findParameterByExternalName(parameter, uriResourceFunction.getParameters());
			final Object value = getValue(uriResourceFunction.getFunction(), parameter, uriParameter.getText());
			functionQuery.setParameter(count, value);
			count += 1;
		}
		return functionQuery.getResultList();
	}

	@SuppressWarnings("unchecked")
	private Expression<Integer> convertLiteralToExpression(final JPAExpressionElement<?> parameter, final int offset)
			throws ODataApplicationException {
		if (parameter instanceof JPAArithmeticOperator) {
			if (offset != 0) {
				return cb.sum((Expression<Integer>) parameter.get(), Integer.valueOf(offset));
			} else {
				return (Expression<Integer>) parameter.get();
			}
		} else if (parameter instanceof JPALiteralOperator) {
			return cb.literal(Integer
					.valueOf(Integer.parseInt(((JPALiteralOperator) parameter).getLiteralValue().toString()) + offset));
		} else {
			return cb.literal(Integer.valueOf(Integer.parseInt(parameter.get().toString()) + offset));
		}
	}

	private UriParameter findParameterByExternalName(final JPAOperationParameter parameter,
			final List<UriParameter> uriParameters) throws ODataApplicationException {
		for (final UriParameter uriParameter : uriParameters) {
			if (uriParameter.getName().equals(parameter.getName())) {
				return uriParameter;
			}
		}
		throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.PARAMETER_MISSING,
				HttpStatusCode.BAD_REQUEST, parameter.getName());
	}

	private String generateQueryString(final JPAFunction jpaFunction) {

		final StringBuffer parameterList = new StringBuffer();
		String queryString = SELECT_BASE_PATTERN;

		queryString = queryString.replace(FUNC_NAME_PLACEHOLDER, jpaFunction.getDBName());
		for (int i = 1; i <= jpaFunction.getParameter().size(); i++) {
			parameterList.append(',');
			parameterList.append('?');
			parameterList.append(i);
		}
		parameterList.deleteCharAt(0);
		return queryString.replace(PARAMETER_PLACEHOLDER, parameterList.toString());
	}

	private Object getValue(final EdmFunction edmFunction, final JPAOperationParameter parameter, final String uriValue)
			throws ODataApplicationException {
		final String value = uriValue.replaceAll("'", "");
		final EdmParameter edmParam = edmFunction.getParameter(parameter.getName());
		try {
			return ((EdmPrimitiveType) edmParam.getType()).valueOfString(value, Boolean.FALSE, parameter.getMaxLength(),
					parameter.getPrecision(), parameter.getScale(), Boolean.TRUE, parameter.getType());
		} catch (final EdmPrimitiveTypeException e) {
			// Unable to convert value %1$s of parameter %2$s
			throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.PARAMETER_CONVERSION_ERROR,
					HttpStatusCode.NOT_IMPLEMENTED, uriValue, parameter.getName());
		}
	}

}
