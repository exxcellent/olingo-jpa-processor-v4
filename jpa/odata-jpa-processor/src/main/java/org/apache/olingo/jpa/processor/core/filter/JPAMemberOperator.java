package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

public class JPAMemberOperator implements JPAExpression<Path<?>> {
	private final Member member;
	private final JPAStructuredType jpaEntityType;
	private final From<?, ?> root;

	JPAMemberOperator(final JPAStructuredType jpaEntityType, final JPAAbstractQuery<?, ?> parent,
			final Member member) {
		super();
		this.member = member;
		this.jpaEntityType = jpaEntityType;
		this.root = parent.getRoot();
	}

	public JPASimpleAttribute determineAttribute() throws ODataApplicationException {
		return (JPASimpleAttribute) determineAttributePath().getLeaf();
	}

	@Override
	public Path<?> get() throws ODataApplicationException {
		final JPASelector selectItemPath = determineAttributePath();
		return determineCriteriaPath(selectItemPath);
	}

	public Member getMember() {
		return member;
	}

	private JPASelector determineAttributePath() throws ODataApplicationException {
		final String path = Util.determineProptertyNavigationPath(member.getResourcePath().getUriResourceParts());
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
