package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

public abstract class JPAAbstractQuery<QueryType extends AbstractQuery<?>> {

	protected final static Logger LOG = Logger.getLogger(JPAAbstractQuery.class.getName());

	protected static final String SELECT_ITEM_SEPERATOR = ",";
	protected static final String SELECT_ALL = "*";

	private final EntityManager em;
	private final CriteriaBuilder cb;
	private final JPAEntityType jpaEntityType;

	protected JPAAbstractQuery(final JPAEntityType jpaEntityType, final EntityManager em)
	        throws ODataApplicationException {
		super();
		this.em = em;
		this.cb = em.getCriteriaBuilder();
		this.jpaEntityType = jpaEntityType;
	}

	public final JPAEntityType getJPAEntityType() {
		return jpaEntityType;
	}

	protected final EntityManager getEntityManager() {
		return em;
	}

	protected final CriteriaBuilder getCriteriaBuilder() {
		return cb;
	}

	private Path<?> buildPath(final From<?, ?> from, final UriParameter keyPredicate) throws ODataJPAModelException {
		Path<?> path = from;
		final JPASelector selector = jpaEntityType.getPath(keyPredicate.getName());
		for (final JPAAttribute<?> attribute : selector.getPathElements()) {
			path = path.get(attribute.getInternalName());
		}
		if (path == from) {
			throw new ODataJPAModelException(MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
		}
		return path;
	}

	protected javax.persistence.criteria.Expression<Boolean> createWhereByKey(final From<?, ?> root,
	        final javax.persistence.criteria.Expression<Boolean> whereCondition, final List<UriParameter> keyPredicates)
	        throws ODataApplicationException {
		// .../Organizations('3')
		// .../BusinessPartnerRoles(BusinessPartnerID='6',RoleCategory='C')
		if (keyPredicates == null) {
			return whereCondition;
		}
		javax.persistence.criteria.Expression<Boolean> compundCondition = whereCondition;

		for (final UriParameter keyPredicate : keyPredicates) {
			javax.persistence.criteria.Expression<Boolean> equalCondition;
			try {
				final Path<?> path = buildPath(root, keyPredicate);
				equalCondition = cb.equal(path, eliminateApostrophe(keyPredicate.getText()));
			} catch (final ODataJPAModelException e) {
				throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
			}
			if (compundCondition == null) {
				compundCondition = equalCondition;
			} else {
				compundCondition = cb.and(compundCondition, equalCondition);
			}
		}
		return compundCondition;
	}

	private String eliminateApostrophe(final String text) {
		return text.replaceAll("'", "");
	}

	protected List<UriParameter> determineKeyPredicates(final UriResource uriResourceItem)
	        throws ODataApplicationException {

		if (uriResourceItem instanceof UriResourceEntitySet) {
			return ((UriResourceEntitySet) uriResourceItem).getKeyPredicates();
		} else if (uriResourceItem instanceof UriResourceNavigation) {
			return ((UriResourceNavigation) uriResourceItem).getKeyPredicates();
		} else {
			throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
			        HttpStatusCode.BAD_REQUEST,
			        uriResourceItem.getKind().name());
		}
	}

	public abstract <T> Root<T> getRoot();

	public abstract QueryType getQuery();

	protected abstract Locale getLocale();

	abstract JPAODataContext getContext();

}