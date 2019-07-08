package org.apache.olingo.jpa.processor.core.database;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;

public final class JPA_HANADatabaseProcessor extends JPA_DefaultDatabaseProcessor {

	@Override
	public Expression<Boolean> createSearchWhereClause(final CriteriaBuilder cb, final CriteriaQuery<?> cq,
			final From<?, ?> root, final JPAEntityType entityType, final SearchOption searchOption)
					throws ODataApplicationException {
		/*
		 * The following code generates a sub-query to filter on the values that matches the search term. This looks
		 * cumbersome, but there were problems using the straight forward solution:
		 * return cb.function("CONTAINS", Boolean.class, root.get("name"), cb.literal(term.getSearchTerm()));
		 * in case $search was combined with $filter. In this case the processing aborts with the following error:
		 * "org.eclipse.persistence.internal.jpa.querydef.FunctionExpressionImpl cannot be cast to
		 * org.eclipse.persistence.internal.jpa.querydef.CompoundExpressionImpl"
		 */
		List<JPASelector> searchableAttributes = null;
		JPAAttributePath keyPath = null;
		try {
			searchableAttributes = entityType.getSearchablePath();
			final List<JPAAttributePath> keyPathList = entityType.getKeyPath();
			if (keyPathList.size() == 1) {
				keyPath = keyPathList.get(0);
			} else {
				throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.WRONG_NO_KEY_PROP,
						HttpStatusCode.INTERNAL_SERVER_ERROR);
			}
		} catch (final ODataJPAModelException e) {
			throw new ODataJPADBAdaptorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		if (!searchableAttributes.isEmpty()) {
			final SearchTerm term = searchOption.getSearchExpression().asSearchTerm();
			@SuppressWarnings("unchecked")
			final Subquery<Object> sq = (Subquery<Object>) cq.subquery(entityType.getKeyType());
			final Root<?> sr = sq.from(root.getJavaType());
			final Expression<Object> sel = sr.get(keyPath.getPathElements().get(0).getInternalName());
			sq.select(sel);
			Path<?> attributePath = sr;
			for (final JPASelector searchableAttribute : searchableAttributes) {
				for (final JPAElement pathItem : searchableAttribute.getPathElements()) {
					attributePath = attributePath.get(pathItem.getInternalName());
				}
			}
			sq.where(cb.function("CONTAINS", Boolean.class, attributePath, cb.literal(term.getSearchTerm())));
			return cb.in(root.get(keyPath.getPathElements().get(0).getInternalName())).value(sq);
		}
		return null;
	}

}
