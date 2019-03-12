package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;

class JPAVisitor implements ExpressionVisitor<JPAExpressionElement<?>> {

	private final JPAAbstractFilterProcessor jpaComplier;

	/**
	 * @param jpaFilterCrossComplier
	 */
	JPAVisitor(final JPAAbstractFilterProcessor jpaFilterCrossComplier) {
		this.jpaComplier = jpaFilterCrossComplier;
	}

	@Override
	public JPAExpression<?> visitAlias(final String aliasName)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
				HttpStatusCode.NOT_IMPLEMENTED, "Alias");
	}

	@Override
	public JPAExpressionElement<?> visitBinaryOperator(final BinaryOperatorKind operator,
			final JPAExpressionElement<?> left, final JPAExpressionElement<?> right)
					throws ExpressionVisitException, ODataApplicationException {

		if (hasNavigation(left) || hasNavigation(right)) {
			return new JPANavigationOperation(this.jpaComplier, operator, left, right);
		}
		if (operator == BinaryOperatorKind.EQ
				|| operator == BinaryOperatorKind.NE
				|| operator == BinaryOperatorKind.GE
				|| operator == BinaryOperatorKind.GT
				|| operator == BinaryOperatorKind.LT
				|| operator == BinaryOperatorKind.LE) {
			return new JPAComparisonOperatorImp<>(this.jpaComplier.getConverter(), operator, left, right);
		} else if (operator == BinaryOperatorKind.AND || operator == BinaryOperatorKind.OR) {
			return new JPABooleanOperatorImp(this.jpaComplier.getConverter(), operator,
					checkBooleanExpressionOperand(left), checkBooleanExpressionOperand(right));
		} else if (operator == BinaryOperatorKind.ADD
				|| operator == BinaryOperatorKind.SUB
				|| operator == BinaryOperatorKind.MUL
				|| operator == BinaryOperatorKind.DIV
				|| operator == BinaryOperatorKind.MOD) {
			return new JPAArithmeticOperatorImp(this.jpaComplier.getConverter(), operator, left, right);
		}
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
				HttpStatusCode.NOT_IMPLEMENTED, operator.name());
	}

	@SuppressWarnings("unchecked")
	private JPAExpression<Expression<Boolean>> checkBooleanExpressionOperand(final JPAExpressionElement<?> operator)
			throws ODataJPAFilterException {
		if (JPAExpressionOperator.class.isInstance(operator)) {
			return JPAExpressionOperator.class.cast(operator);
		} else if (ODataBuiltinFunctionCall.class.isInstance(operator)) {
			// only a few builtin functions are of result type 'boolean'
			switch (ODataBuiltinFunctionCall.class.cast(operator).getFunctionKind()) {
			case CONTAINS:
			case STARTSWITH:
			case ENDSWITH:
				return (JPAExpression<Expression<Boolean>>) operator;
			default:
				// throw exception at end of method
			}
		}
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
				HttpStatusCode.NOT_IMPLEMENTED, operator.getClass().getSimpleName());

	}

	@Override
	public JPAExpressionElement<?> visitEnum(final EdmEnumType type, final List<String> enumValues)
			throws ExpressionVisitException,
			ODataApplicationException {
		if (enumValues.isEmpty()) {
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
					HttpStatusCode.NOT_IMPLEMENTED, "Empty Enumeration value");
		}
		if (enumValues.size() > 1) {
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
					HttpStatusCode.NOT_IMPLEMENTED, "Multiple Enumeration values");
		}
		final Literal literal = new LiteralImpl(enumValues.get(0), type);
		return new JPALiteralOperator(getOdata(), getCriteriaBuilder(), literal);
	}

	@Override
	public JPAExpressionElement<Expression<?>> visitLambdaExpression(final String lambdaFunction,
			final String lambdaVariable,
			final org.apache.olingo.server.api.uri.queryoption.expression.Expression expression)
					throws ExpressionVisitException, ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
				HttpStatusCode.NOT_IMPLEMENTED, "Lambda Expression");
	}

	@Override
	public JPAExpressionElement<Expression<?>> visitLambdaReference(final String variableName)
			throws ExpressionVisitException,
			ODataApplicationException {
		throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER,
				HttpStatusCode.NOT_IMPLEMENTED, "Lambda Reference");
	}

	@Override
	public JPAExpressionElement<?> visitLiteral(final Literal literal)
			throws ExpressionVisitException, ODataApplicationException {
		return new JPALiteralOperator(getOdata(), getCriteriaBuilder(), literal);
	}

	@Override
	public JPAExpressionElement<?> visitMember(final Member member)
			throws ExpressionVisitException, ODataApplicationException {

		if (getLambdaType(member.getResourcePath()) == UriResourceKind.lambdaAny) {
			return new JPALambdaAnyOperation(this.jpaComplier, member);
		} else if (getLambdaType(member.getResourcePath()) == UriResourceKind.lambdaAll) {
			return new JPALambdaAllOperation(this.jpaComplier, member);
		} else if (isAggregation(member.getResourcePath())) {
			return new JPAAggregationOperationCountImp(jpaComplier.getParent().getRoot(), jpaComplier.getConverter());
		} else if (isCustomFunction(member.getResourcePath())) {
			final UriResource resource = member.getResourcePath().getUriResourceParts().get(0);
			final JPAFunction jpaFunction = this.jpaComplier.getSd().getFunction(((UriResourceFunction) resource)
					.getFunction());
			final List<UriParameter> odataParams = ((UriResourceFunction) resource).getParameters();
			return new JPAFunctionOperator(this, odataParams, jpaFunction);
			// , this.jpaComplier.getParent().getRoot(), jpaComplier.getConverter().cb);
		}
		return new JPAMemberOperator(this.jpaComplier.getJpaEntityType(), this.jpaComplier.getParent(), member);
	}

	@Override
	public JPAExpressionElement<?> visitMethodCall(final MethodKind methodCall,
			final List<JPAExpressionElement<?>> parameters)
					throws ExpressionVisitException, ODataApplicationException {
		return new ODataBuiltinFunctionCallImp(this.jpaComplier.getConverter(), methodCall, parameters);
	}

	@Override
	public JPAExpressionElement<?> visitTypeLiteral(final EdmType type)
			throws ExpressionVisitException, ODataApplicationException {
		return new JPALiteralTypeOperator(type);
	}

	@Override
	public JPAExpressionElement<?> visitUnaryOperator(final UnaryOperatorKind operator,
			final JPAExpressionElement<?> operand)
					throws ExpressionVisitException, ODataApplicationException {
		if (operator == UnaryOperatorKind.NOT) {
			return new JPAUnaryBooleanOperatorImp(this.jpaComplier.getConverter(), operator,
					checkBooleanExpressionOperand(operand));
		} else {
			throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_OPERATOR,
					HttpStatusCode.NOT_IMPLEMENTED, operator.name());
		}
	}

	UriResourceKind getLambdaType(final UriInfoResource member) {
		for (final UriResource r : member.getUriResourceParts()) {
			if (r.getKind() == UriResourceKind.lambdaAny
					|| r.getKind() == UriResourceKind.lambdaAll) {
				return r.getKind();
			}
		}
		return null;
	}

	boolean hasNavigation(final JPAExpressionElement<?> operand) {
		if (operand instanceof JPAMemberOperator) {
			final List<UriResource> uriResourceParts = ((JPAMemberOperator) operand).getMember().getResourcePath()
					.getUriResourceParts();
			for (int i = uriResourceParts.size() - 1; i >= 0; i--) {
				if (uriResourceParts.get(i) instanceof UriResourceNavigation) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isAggregation(final UriInfoResource resourcePath) {
		if (resourcePath.getUriResourceParts().size() == 1 && resourcePath.getUriResourceParts().get(0)
				.getKind() == UriResourceKind.count) {
			return true;
		}
		return false;
	}

	private boolean isCustomFunction(final UriInfoResource resourcePath) {
		if (resourcePath.getUriResourceParts().size() > 0 && resourcePath.getUriResourceParts().get(
				0) instanceof UriResourceFunction) {
			return true;
		}
		return false;
	}

	CriteriaBuilder getCriteriaBuilder() {
		return jpaComplier.getConverter().getCriteriaBuilder();
	}

	IntermediateServiceDocument getSd() {
		return jpaComplier.getSd();
	}

	Root<?> getRoot() {
		return jpaComplier.getParent().getRoot();
	}

	public OData getOdata() {
		return jpaComplier.getOdata();
	}
}