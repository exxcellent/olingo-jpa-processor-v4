package org.apache.olingo.jpa.processor.core.query;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.api.JPAServiceDebugger;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

public abstract class JPAAbstractQuery {

	protected final static Logger LOG = Logger.getLogger(JPAAbstractQuery.class.getName());

	private static final int CONTAINY_ONLY_LANGU = 1;
	private static final int CONTAINS_LANGU_COUNTRY = 2;
	protected static final String SELECT_ITEM_SEPERATOR = ",";
	protected static final String SELECT_ALL = "*";
	protected final EntityManager em;
	protected final CriteriaBuilder cb;
	protected final JPAEntityType jpaEntityType;
	protected final IntermediateServiceDocument sd;
	protected Locale locale;

	public JPAAbstractQuery(final IntermediateServiceDocument sd, final JPAEntityType jpaEntityType, final EntityManager em)
			throws ODataApplicationException {
		super();
		this.em = em;
		this.cb = em.getCriteriaBuilder();
		this.sd = sd;
		this.jpaEntityType = jpaEntityType;
	}

	public JPAAbstractQuery(final IntermediateServiceDocument sd, final EdmEntityType edmEntityType, final EntityManager em)
			throws ODataApplicationException {
		super();
		this.em = em;
		this.cb = em.getCriteriaBuilder();
		this.sd = sd;
		try {
			this.jpaEntityType = sd.getEntityType(edmEntityType);
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
		}
	}

	public JPAAbstractQuery(final IntermediateServiceDocument sd, final JPAEntityType jpaEntityType, final EntityManager em,
			final JPAServiceDebugger debugger) {
		super();
		this.em = em;
		this.cb = em.getCriteriaBuilder();
		this.sd = sd;
		this.jpaEntityType = jpaEntityType;
	}

	public JPAEntityType getJPAEntityType() {
		return jpaEntityType;
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
				equalCondition = cb.equal(root.get(jpaEntityType.getPath(keyPredicate.getName()).getLeaf()
						.getInternalName()), eliminateApostrophe(keyPredicate.getText()));
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

	public abstract AbstractQuery<?> getQuery();

	protected abstract Locale getLocale();

	protected final Locale determineLocale(final Map<String, List<String>> headers) {
		// TODO Make this replaceable so the default can be overwritten
		// http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html (14.4 accept language header
		// example: Accept-Language: da, en-gb;q=0.8, en;q=0.7)
		final List<String> languageHeaders = headers.get("accept-language");
		if (languageHeaders != null) {
			final String languageHeader = languageHeaders.get(0);
			if (languageHeader != null) {
				final String[] localeList = languageHeader.split(SELECT_ITEM_SEPERATOR);
				final String locale = localeList[0];
				final String[] languCountry = locale.split("-");
				if (languCountry.length == CONTAINS_LANGU_COUNTRY) {
					return new Locale(languCountry[0], languCountry[1]);
				} else if (languCountry.length == CONTAINY_ONLY_LANGU) {
					return new Locale(languCountry[0]);
				} else {
					return Locale.ENGLISH;
				}
			}
		}
		return Locale.ENGLISH;
	}

	abstract JPAODataSessionContextAccess getContext();

}