package org.apache.olingo.jpa.processor.core.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
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

class JPAMemberVisitor implements ExpressionVisitor<JPAAttributePath> {
  private final ArrayList<JPAAttributePath> pathList = new ArrayList<JPAAttributePath>();
  private final JPAEntityType jpaEntityType;

  public JPAMemberVisitor(JPAEntityType jpaEntityType) {
    super();
    this.jpaEntityType = jpaEntityType;
  }

  public List<JPAAttributePath> get() {
    return pathList;
  }

  @Override
  public JPAAttributePath visitBinaryOperator(BinaryOperatorKind operator, JPAAttributePath left, JPAAttributePath right)
      throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitUnaryOperator(UnaryOperatorKind operator, JPAAttributePath operand) throws ExpressionVisitException,
      ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitMethodCall(MethodKind methodCall, List<JPAAttributePath> parameters) throws ExpressionVisitException,
      ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitLambdaExpression(String lambdaFunction, String lambdaVariable,
      org.apache.olingo.server.api.uri.queryoption.expression.Expression expression) throws ExpressionVisitException,
      ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
    UriResourceKind uriResourceKind = member.getResourcePath().getUriResourceParts().get(0).getKind();

    if (uriResourceKind == UriResourceKind.primitiveProperty || uriResourceKind == UriResourceKind.complexProperty) {
      if (!Util.hasNavigation(member.getResourcePath().getUriResourceParts())) {
        final String path = Util.determineProptertyNavigationPath(member.getResourcePath().getUriResourceParts());
        JPAAttributePath selectItemPath = null;
        try {
          selectItemPath = jpaEntityType.getPath(path);
        } catch (ODataJPAModelException e) {
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
  public JPAAttributePath visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitLambdaReference(String variableName) throws ExpressionVisitException,
      ODataApplicationException {
    return null;
  }

  @Override
  public JPAAttributePath visitEnum(EdmEnumType type, List<String> enumValues) throws ExpressionVisitException,
      ODataApplicationException {
    return null;
  }

}
