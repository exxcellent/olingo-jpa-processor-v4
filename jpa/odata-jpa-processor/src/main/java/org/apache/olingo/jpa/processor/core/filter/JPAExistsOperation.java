package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;

abstract class JPAExistsOperation implements JPAExpression<Expression<Boolean>> {

	protected final JPAODataDatabaseProcessor converter;
	protected final List<UriResource> uriResourceParts;
	protected final JPAAbstractQuery<?, ?> root;
	protected final IntermediateServiceDocument sd;
	protected final EntityManager em;
	protected final OData odata;

	JPAExistsOperation(final JPAAbstractFilterProcessor jpaComplier) {

		this.uriResourceParts = jpaComplier.getUriResourceParts();
		this.root = jpaComplier.getParent();
		this.sd = jpaComplier.getSd();
		this.em = jpaComplier.getEntityManager();
		this.converter = jpaComplier.getConverter();
		this.odata = jpaComplier.getOdata();
	}

	@Override
	public Expression<Boolean> get() throws ODataApplicationException {
		final Subquery<?> subquery = buildFilterSubQueries();
		if (subquery == null) {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_PREPARATION_FILTER_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		return converter.getCriteriaBuilder().exists(subquery);
	}

	abstract Subquery<?> buildFilterSubQueries() throws ODataApplicationException;

}