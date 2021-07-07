package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public class JPAMemberOperand<T> implements JPAExpression<T> {
  private final List<UriResource> uriResourceParts;
  private final Member member;
  private final JPAStructuredType jpaEntityType;
  private final From<?, ?> root;
  private final IntermediateServiceDocument sd;
  private final JPAAbstractFilterProcessor<?> jpaComplier;

  JPAMemberOperand(final JPAAbstractFilterProcessor<?> jpaComplier, final Member member) {
    super();
    this.uriResourceParts = jpaComplier.getUriResourceParts();
    this.jpaComplier = jpaComplier;
    this.member = member;
    this.jpaEntityType = jpaComplier.getJpaEntityType();
    assert jpaEntityType != null;
    this.root = jpaComplier.getParent().getQueryResultFrom();
    this.sd = jpaComplier.getSd();
  }

  protected final List<UriResource> getUriResourceParts() {
    return uriResourceParts;
  }

  public JPAMemberAttribute determineAttribute() throws ODataApplicationException {
    return (JPAMemberAttribute) determineAttributePath().getLeaf();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Expression<T> get() throws ODataApplicationException {
    //    final List<UriResource> allUriResourceParts = new ArrayList<UriResource>(getUriResourceParts());
    //    allUriResourceParts.addAll(member.getResourcePath().getUriResourceParts());
    //    final List<JPANavigationPropertyInfo> navigations = Util.determineNavigations(sd, allUriResourceParts);
    //    if (false && !navigations.isEmpty()) {
    //
    //      final List<UriResource> subList = new LinkedList<>(getUriResourceParts());
    //      subList.add(member.getResourcePath().getUriResourceParts().get(0));// TODO add all but simple sub member part
    //      final JPAAbstractFilterProcessor<Boolean> filterProcessor = new JPAEntityFilterProcessor<Boolean>(jpaComplier
    //          .getOdata(), sd, jpaComplier.getEntityManager(), jpaComplier.getJpaEntityType(), jpaComplier.getConverter(),
    //          subList, member, jpaComplier.getParent());
    //
    //      final JPANavigationMember nav = new JPANavigationMember(filterProcessor, member);
    //      return (Expression<T>) nav.get();
    //
    //    } else {
    final JPASelector selectItemPath = determineAttributePath();
    return (Expression<T>) determineCriteriaPath(selectItemPath);
    //    }
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
