package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

/**
 * Compiles just one Expression. Mainly build for filter on navigation
 * @author Oliver Grande
 *
 */
//TODO handle $it ...
public class JPANavigationFilterProcessor extends JPAAbstractFilterProcessor {
	private final JPAODataDatabaseProcessor converter;
	final EntityManager em;
	final OData odata;
	final IntermediateServiceDocument sd;
	final List<UriResource> uriResourceParts;
	final JPAAbstractQuery<?> parent;

	public JPANavigationFilterProcessor(final OData odata, final IntermediateServiceDocument sd, final EntityManager em,
			final JPAEntityType jpaEntityType, final JPAODataDatabaseProcessor converter,
			final List<UriResource> uriResourceParts, final JPAAbstractQuery<?> parent,
			final VisitableExpression expression) {

		super(jpaEntityType, expression);
		this.converter = converter;
		this.em = em;
		this.odata = odata;
		this.sd = sd;
		this.uriResourceParts = uriResourceParts;
		this.parent = parent;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<Boolean> compile() throws ExpressionVisitException, ODataApplicationException {

		final ExpressionVisitor<JPAExpressionElement<?>> visitor = new JPAVisitor(this);
		final Expression<Boolean> finalExpression = (Expression<Boolean>) getExpression().accept(visitor).get();

		return finalExpression;
	}

	@Override
	public JPAODataDatabaseProcessor getConverter() {
		return converter;
	}

	@Override
	public EntityManager getEntityManager() {
		return em;
	}

	@Override
	public OData getOdata() {
		return odata;
	}

	@Override
	public IntermediateServiceDocument getSd() {
		return sd;
	}

	@Override
	public List<UriResource> getUriResourceParts() {
		return uriResourceParts;
	}

	@Override
	public JPAAbstractQuery<?> getParent() {
		return parent;
	}

}
