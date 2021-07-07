package org.apache.olingo.jpa.processor.core.database;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmParameter;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.filter.JPAAggregationOperation;
import org.apache.olingo.jpa.processor.core.filter.JPAArithmeticOperation;
import org.apache.olingo.jpa.processor.core.filter.JPABooleanOperation;
import org.apache.olingo.jpa.processor.core.filter.JPAExpressionElement;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralOperand;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralTypeOperand;
import org.apache.olingo.jpa.processor.core.filter.JPAUnaryBooleanOperation;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;

/**
 * The base class of all data base processor. This class will handle the life
 * cycle including the stateful properties.
 *
 * @author Ralf Zozmann
 *
 */
public abstract class AbstractJPADatabaseProcessor implements JPAODataDatabaseProcessor {

  protected final static Logger LOG = Logger.getLogger(JPAODataDatabaseProcessor.class.getName());
  protected final static char ESCAPE_CHARACTER = '\\';

  private final static Character[] LIKE_RESERVED_CHARACTERS = new Character[] { Character.valueOf('%'), Character
      .valueOf('_') };
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

  protected final CriteriaBuilder getCriteriaBuilder() {
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
  public <Y extends Number> Expression<Number> createCalculation(final BinaryOperatorKind operator,
      final Expression<Y> operand1,
      final Expression<Y> operand2) throws ODataApplicationException {
    switch (operator) {
    case ADD:
      return cb.sum(operand1, operand2);
    case SUB:
      return cb.diff(operand1, operand2);
    case DIV:
      return cb.quot(operand1, operand2);
    case MUL:
      return cb.prod(operand1, operand2);
    case MOD:
      return ((Expression<Number>) (Expression<?>) cb.mod((Expression<Integer>) operand1,
          (Expression<Integer>) operand2));
    default:
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
          HttpStatusCode.NOT_IMPLEMENTED, operator.name());
    }
  }

  @Override
  public Expression<Boolean> convert(final JPABooleanOperation jpaOperator) throws ODataApplicationException {
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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public <Y extends Comparable> Expression<Boolean> createComparison(final BinaryOperatorKind operator,
      final Expression<Y> operand1,
      final Expression<Y> operand2)
          throws ODataApplicationException {
    switch (operator) {
    case EQ:
      if (operand1 == null) {
        return cb.isNull(operand2);
      } else if (operand2 == null) {
        return cb.isNull(operand1);
      } else {
        return cb.equal(operand1, operand2);
      }
    case NE:
      if (operand1 == null) {
        return cb.isNotNull(operand2);
      } else if (operand2 == null) {
        return cb.isNotNull(operand1);
      } else {
        return cb.notEqual(operand1, operand2);
      }
    case GE:
      return cb.greaterThanOrEqualTo(operand1, operand2);
    case GT:
      return cb.greaterThan(operand1, operand2);
    case LT:
      return cb.lessThan(operand1, operand2);
    case LE:
      return cb.lessThanOrEqualTo(operand1, operand2);
    default:
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
          HttpStatusCode.NOT_IMPLEMENTED, operator.name());
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
      final Expression<String> operand = (Expression<String>) parameters.get(0).get();
      final String literal = (String) ((JPALiteralOperand) parameters.get(1)).get();
      return contains(operand, literal);
    case ENDSWITH:
      final StringBuffer ends = new StringBuffer();
      ends.append('%');
      ends.append(((JPALiteralOperand) parameters.get(1)).get());
      return cb.like((Expression<String>) (parameters.get(0).get()), ends.toString());
    case STARTSWITH:
      final StringBuffer starts = new StringBuffer();
      starts.append(((JPALiteralOperand) parameters.get(1)).get());
      starts.append('%');
      return cb.like((Expression<String>) (parameters.get(0).get()), starts.toString());
    case INDEXOF:
      final String searchString = (String) (((JPALiteralOperand) parameters.get(1)).get());
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
      if (parameters.get(0) instanceof JPALiteralOperand) {
        tolower = (Expression<String>) (Expression<?>) ((JPALiteralOperand) parameters.get(0)).getLiteralExpression();
      } else {
        tolower = (Expression<String>) (parameters.get(0).get());
      }
      return cb.lower(tolower);
    case TOUPPER:
      final Expression<String> toupper;
      if (parameters.get(0) instanceof JPALiteralOperand) {
        toupper = (Expression<String>) (Expression<?>) ((JPALiteralOperand) parameters.get(0)).getLiteralExpression();
      } else {
        toupper = (Expression<String>) (parameters.get(0).get());
      }
      return cb.upper(toupper);
    case TRIM:
      return cb.trim((Expression<String>) (parameters.get(0).get()));
    case CONCAT:
      Expression<String> operand1;
      if (parameters.get(0).get() instanceof String) {
        operand1 = cb.literal((String) parameters.get(0).get());
      } else {
        operand1 = (Expression<String>) parameters.get(0).get();
      }
      Expression<String> operand2;
      if (parameters.get(1).get() instanceof String) {
        operand2 = cb.literal((String) parameters.get(1).get());
      } else {
        operand2 = (Expression<String>) parameters.get(1).get();
      }
      return concat(operand1, operand2);
    case CAST:
      if (parameters.size() != 2) {
        throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
            HttpStatusCode.BAD_REQUEST, methodCall.name());
      }
      final Expression<?> valueOperand = (Expression<?>) parameters.get(0).get();
      final JPALiteralTypeOperand typeOperand = (JPALiteralTypeOperand) parameters.get(1);
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
      return cast2Time((Expression<?>) parameters.get(0).get());
    case DATE:
      // extract date part from a timestamp
      if (parameters.size() != 1) {
        throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
            HttpStatusCode.BAD_REQUEST, methodCall.name());
      }
      return cast2Date((Expression<?>) parameters.get(0).get());
    default:
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
          HttpStatusCode.NOT_IMPLEMENTED, methodCall.name());
    }
  }

  /**
   *
   * @param value
   *            The expression to cast into a TIME value.
   * @return the criteria API expression casting/converting the given expression
   *         value into a TIME.
   * @throws ODataApplicationException
   * @see {@link MethodKind#TIME}
   */
  protected Expression<?> cast2Time(final Expression<?> value) throws ODataApplicationException {
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
        HttpStatusCode.NOT_IMPLEMENTED, MethodKind.TIME.name());
  }

  /**
   *
   * @param value
   *            The expression to cast into a DATE value.
   * @return the criteria API expression casting/converting the given expression
   *         value into a DATE.
   * @throws ODataApplicationException
   * @see {@link MethodKind#DATE}
   */
  protected Expression<?> cast2Date(final Expression<?> value) throws ODataApplicationException {
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
        HttpStatusCode.NOT_IMPLEMENTED, MethodKind.DATE.name());
  }

  /**
   * Cast a value into another one with database SQL terms.
   *
   * @param value
   *            The expression or literal to cast.
   * @param type
   *            The target type to cast to.
   * @return The criteria API expression to cast the given <i>value</i> into the
   *         requested type.
   * @throws ODataApplicationException
   * @see {@link MethodKind#CAST}
   */
  protected Expression<?> cast(final Expression<?> value, final EdmType type) throws ODataApplicationException {
    if (EdmPrimitiveTypeKind.String.getFullQualifiedName().equals(type.getFullQualifiedName())) {
      return cast2String(value);
    }
    if (EdmPrimitiveTypeKind.Date.getFullQualifiedName().equals(type.getFullQualifiedName())) {
      return cast2Date(value);
    }
    throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
        HttpStatusCode.NOT_IMPLEMENTED, MethodKind.CAST.name());
  }

  protected Expression<Boolean> contains(final Expression<String> operand, final String literal)
      throws ODataApplicationException {
    final Collection<Character> reserved = findReservedCharacters(literal);
    final StringBuffer contains = new StringBuffer();
    contains.append('%');
    contains.append(escapeCharacters(literal, reserved));
    contains.append('%');
    if (reserved.isEmpty()) {
      return cb.like(operand, contains.toString());
    } else {
      return cb.like(operand, contains.toString(), ESCAPE_CHARACTER);
    }
  }

  private String escapeCharacters(final String literal, final Collection<Character> chars) {
    String result = literal;
    for (final Character c : chars) {
      final String cS = c.toString();
      result = result.replace(cS, String.valueOf(ESCAPE_CHARACTER).concat(cS));
    }
    return result;
  }

  private Collection<Character> findReservedCharacters(final String literal) {
    if (literal == null || literal.isEmpty()) {
      return Collections.emptyList();
    }
    final List<Character> foundCharacters = new LinkedList<>();
    for (final Character c : reservedLIKECharacters()) {
      if (literal.indexOf(c.charValue()) < 0) {
        continue;
      }
      foundCharacters.add(c);
    }
    return foundCharacters;
  }

  /**
   *
   * @return The collection of all reserved characters that have to be escaped in a query using LIKE. These are '_' and
   * '%' for standard SQL.
   */
  protected Collection<Character> reservedLIKECharacters() {
    return Arrays.asList(LIKE_RESERVED_CHARACTERS);
  }

  protected Expression<?> concat(final Expression<String> operand1, final Expression<String> operand2)
      throws ODataApplicationException {
    return cb.concat(operand1, operand2);
  }

  @SuppressWarnings("unchecked")
  protected Expression<?> cast2String(final Expression<?> value) throws ODataApplicationException {
    final CriteriaBuilder cb = getCriteriaBuilder();
    // handle the cast to string as concatenation...
    return concat((Expression<String>) value, cb.literal("''"));
  }

  @Override
  public Expression<Boolean> convert(final JPAUnaryBooleanOperation jpaOperator) throws ODataApplicationException {
    switch (jpaOperator.getOperator()) {
    case NOT:
      return cb.not(jpaOperator.getOperand());
    default:
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
          HttpStatusCode.NOT_IMPLEMENTED, jpaOperator.getOperator().name());
    }
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
    if (parameter instanceof JPAArithmeticOperation) {
      if (offset != 0) {
        return cb.sum((Expression<Integer>) parameter.get(), Integer.valueOf(offset));
      } else {
        return (Expression<Integer>) parameter.get();
      }
    } else if (parameter instanceof JPALiteralOperand) {
      return cb.literal(Integer
          .valueOf(Integer.parseInt(((JPALiteralOperand) parameter).getODataLiteral().getText()) + offset));
    } else {
      // should never habppen?
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

  @SuppressWarnings("unchecked")
  private final Expression<Boolean> buildColumnCondition(final Path<?> pathColumn,
      final SearchTerm term) throws ODataApplicationException {
    final Class<?> javaType = pathColumn.getJavaType();
    if (javaType == String.class || javaType == UUID.class || javaType == URI.class || javaType == URL.class) {
      return createSearchStringColumnExpression((Expression<String>) pathColumn, term);
    } else if (javaType == Boolean.class || javaType == boolean.class) {
      return createSearchBooleanColumnExpression((Expression<Boolean>) pathColumn, term);
    } else if (Number.class.isAssignableFrom(javaType)) {
      return createSearchNumberColumnExpression((Expression<Number>) pathColumn, term);
    }
    LOG.log(Level.WARNING,
        "Unsupported data type " + javaType.getName() + " in '" + pathColumn.getAlias() + "' for @"
            + EdmSearchable.class.getSimpleName());
    return null;
  }

  @SuppressWarnings("unchecked")
  private Expression<Boolean> createSearchNumberColumnExpression(final Expression<Number> memberOperand, final SearchTerm term)
      throws ODataApplicationException {
    final Expression<?> operand = memberOperand;
    // 1. try to cast number to string (optional)
    // try {
    // // wrap into cast expression
    // operand = cast(memberOperand, EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String));
    // } catch (final ODataApplicationException ex) {
    // LOG.log(Level.WARNING, "CAST not supported to convert " + memberOperand.getJavaType()
    // + " into String for $search... try without cast, but that may fail on some databases!");
    // }
    // 2. generate LIKE expression for number
    return contains((Expression<String>) operand, term.getSearchTerm());
  }

  private Expression<Boolean> createSearchStringColumnExpression(final Expression<String> memberOperand,
      final SearchTerm term) throws ODataApplicationException {
    return contains(memberOperand, term.getSearchTerm());
  }

  private Expression<Boolean> createSearchBooleanColumnExpression(final Expression<Boolean> memberOperand,
      final SearchTerm term) throws ODataApplicationException {
    if ("true".equalsIgnoreCase(term.getSearchTerm()) || "1".equalsIgnoreCase(term.getSearchTerm())) {
      return getCriteriaBuilder().isTrue(memberOperand);
    }
    if ("false".equalsIgnoreCase(term.getSearchTerm()) || "0".equalsIgnoreCase(term.getSearchTerm())) {
      return getCriteriaBuilder().isFalse(memberOperand);
    }
    // do not search for the default boolean behaviour 'false' if not parsable
    return null;
  }

  private javax.persistence.criteria.Expression<Boolean> combineOR(
      javax.persistence.criteria.Expression<Boolean> whereCondition,
      final javax.persistence.criteria.Expression<Boolean> additionalExpression) {

    if (additionalExpression != null) {
      if (whereCondition == null) {
        whereCondition = additionalExpression;
      } else {
        whereCondition = getCriteriaBuilder().or(whereCondition, additionalExpression);
      }
    }
    return whereCondition;
  }

  @Override
  public Expression<Boolean> createSearchExpression(final SearchTerm search, final List<Path<?>> searchColumns)
      throws ODataApplicationException {
    javax.persistence.criteria.Expression<Boolean> whereCondition = null;
    javax.persistence.criteria.Expression<Boolean> condition;
    for (final Path<?> path : searchColumns) {
      condition = buildColumnCondition(path, search);
      whereCondition = combineOR(whereCondition, condition);
    }
    return whereCondition;
  }
}
