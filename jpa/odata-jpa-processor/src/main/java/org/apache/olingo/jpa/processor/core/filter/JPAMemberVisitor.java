package org.apache.olingo.jpa.processor.core.filter;

import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

class JPAMemberVisitor implements ExpressionVisitor<JPASelector> {
	private final List<JPASelector> pathList = new LinkedList<JPASelector>();
	private final JPAEntityType jpaEntityType;

	public JPAMemberVisitor(final JPAEntityType jpaEntityType) {
		super();
		this.jpaEntityType = jpaEntityType;
	}

	public List<JPASelector> get() {
		return pathList;
	}

	@Override
	public JPASelector visitBinaryOperator(final BinaryOperatorKind operator, final JPASelector left,
			final JPASelector right)
			throws ExpressionVisitException, ODataApplicationException {
		return null;
	}

	@Override
	public JPAAttributePath visitUnaryOperator(final UnaryOperatorKind operator, final JPASelector operand)
			throws ExpressionVisitException,
	ODataApplicationException {
		return null;
	}

	@Override
	public JPASelector visitMethodCall(final MethodKind methodCall, final List<JPASelector> parameters)
			throws ExpressionVisitException,
	ODataApplicationException {
		return null;
	}

	@Override
	public JPASelector visitLambdaExpression(final String lambdaFunction, final String lambdaVariable,
			final org.apache.olingo.server.api.uri.queryoption.expression.Expression expression) throws ExpressionVisitException,
	ODataApplicationException {
		return null;
	}

	@Override
	public JPAAttributePath visitLiteral(final Literal literal) throws ExpressionVisitException, ODataApplicationException {
		return null;
	}

	@Override
	public JPASelector visitMember(final Member member) throws ExpressionVisitException, ODataApplicationException {
		final UriResourceKind uriResourceKind = member.getResourcePath().getUriResourceParts().get(0).getKind();

		if (uriResourceKind == UriResourceKind.primitiveProperty || uriResourceKind == UriResourceKind.complexProperty) {
			if (!Util.hasNavigation(member.getResourcePath().getUriResourceParts())) {
				final String path = Util.determineProptertyNavigationPath(member.getResourcePath().getUriResourceParts());
				JPASelector selectItemPath = null;
				try {
					selectItemPath = jpaEntityType.getPath(path);
				} catch (final ODataJPAModelException e) {
					throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
				}
				if (selectItemPath != null) {
					pathList.add(selectItemPath);
					return selectItemPath;
				}
			}
		}
		return null;
	}

	@Override
	public JPAAttributePath visitAlias(final String aliasName) throws ExpressionVisitException, ODataApplicationException {
		return null;
	}

	@Override
	public JPAAttributePath visitTypeLiteral(final EdmType type) throws ExpressionVisitException, ODataApplicationException {
		return null;
	}

	@Override
	public JPAAttributePath visitLambdaReference(final String variableName) throws ExpressionVisitException,
	ODataApplicationException {
		return null;
	}

	@Override
	public JPAAttributePath visitEnum(final EdmEnumType type, final List<String> enumValues) throws ExpressionVisitException,
	ODataApplicationException {
		return null;
	}

}
