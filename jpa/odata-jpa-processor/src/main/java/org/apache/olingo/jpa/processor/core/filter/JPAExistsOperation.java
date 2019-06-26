package org.apache.olingo.jpa.processor.core.filter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.query.JPAAbstractQuery;
import org.apache.olingo.jpa.processor.core.query.JPANavigationPropertyInfo;
import org.apache.olingo.jpa.processor.core.query.Util;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

abstract class JPAExistsOperation implements JPAExpression<Expression<Boolean>> {

	protected final JPAODataDatabaseProcessor converter;
	protected final List<UriResource> uriResourceParts;
	protected final JPAAbstractQuery<?> root;
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

	public static boolean hasNavigation(final List<UriResource> uriResourceParts) {
		if (uriResourceParts == null) {
			return false;
		}
		for (final UriResource resourcePart : uriResourceParts) {
			if (resourcePart instanceof UriResourceNavigation) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Expression<Boolean> get() throws ODataApplicationException {
		return converter.getCriteriaBuilder().exists(buildFilterSubQueries());
	}

	abstract Subquery<?> buildFilterSubQueries() throws ODataApplicationException;

	protected List<JPANavigationPropertyInfo> determineAssoziations(final IntermediateServiceDocument sd,
			final List<UriResource> resourceParts) throws ODataApplicationException {
		if (!hasNavigation(resourceParts)) {
			return Collections.emptyList();
		}
		final List<JPANavigationPropertyInfo> pathList = new LinkedList<JPANavigationPropertyInfo>();

		StringBuffer associationName = null;
		UriResourceNavigation navigation = null;
		// for (int i = 0; i < resourceParts.size(); i++) {
		for (int i = resourceParts.size() - 1; i >= 0; i--) {
			final UriResource resourcePart = resourceParts.get(i);
			if (resourcePart instanceof UriResourceNavigation) {
				if (navigation != null) {
					pathList.add(new JPANavigationPropertyInfo(navigation,
							Util.determineAssoziationPath(sd, ((UriResourcePartTyped) resourceParts.get(i)), associationName)));
				} else {
					navigation = (UriResourceNavigation) resourcePart;
					associationName = new StringBuffer();
					associationName.insert(0, navigation.getProperty().getName());
				}
			}
			if (navigation != null) {
				if (resourcePart instanceof UriResourceComplexProperty) {
					associationName.insert(0, JPASelector.PATH_SEPERATOR);
					associationName.insert(0, ((UriResourceComplexProperty) resourcePart).getProperty().getName());
				}
				if (resourcePart instanceof UriResourceEntitySet) {
					pathList.add(new JPANavigationPropertyInfo(navigation,
							Util.determineAssoziationPath(sd, ((UriResourcePartTyped) resourcePart),
									associationName)));
				}
			}
		}
		return pathList;
	}
}