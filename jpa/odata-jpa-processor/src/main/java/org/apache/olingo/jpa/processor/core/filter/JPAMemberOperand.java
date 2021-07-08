package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public class JPAMemberOperand<T> implements JPAExpression<T> {
  private final Member member;
  private final JPAStructuredType jpaEntityType;
  private final From<?, ?> root;

  JPAMemberOperand(final JPAStructuredType jpaEntityType, final From<?, ?> root, final Member member) {
    super();
    this.member = member;
    this.jpaEntityType = jpaEntityType;
    assert jpaEntityType != null;
    this.root = root;
  }

  @Override
  public org.apache.olingo.server.api.uri.queryoption.expression.Expression getQueryExpressionElement() {
    return member;
  }

  public JPAMemberAttribute determineAttribute() throws ODataApplicationException {
    return (JPAMemberAttribute) determineAttributePath().getLeaf();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Expression<T> get() throws ODataApplicationException {
    // we can handle here only simple/complex attribute 'paths' without navigation, they must be detected/handled
    // outside by the visitor
    assert Util.determineNavigations(member.getResourcePath().getUriResourceParts()).isEmpty();

    final JPASelector selectItemPath = determineAttributePath();
    return (Expression<T>) determineCriteriaPath(selectItemPath);
  }

  public Member getMember() {
    return member;
  }

  private JPASelector determineAttributePath() throws ODataApplicationException {
    final String path = Util.determinePropertyPath(member.getResourcePath().getUriResourceParts());
    JPASelector selectItemPath = null;
    try {
      selectItemPath = jpaEntityType.getPath(path);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAFilterException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
    if (selectItemPath == null) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.INTERNAL_SERVER_ERROR, "attribute path '"+path+"' invalid for "+jpaEntityType.getExternalName());
    }
    return selectItemPath;
  }

  private Path<?> determineCriteriaPath(final JPASelector selectItemPath) {
    Path<?> p = root;
    for (final JPAElement jpaPathElement : selectItemPath.getPathElements()) {
      p = p.get(jpaPathElement.getInternalName());
    }
    return p;
  }
}
