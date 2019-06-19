package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
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

	private static final int CONTAINY_ONLY_LANGU = 1;
	private static final int CONTAINS_LANGU_COUNTRY = 2;

	private final EntityManager em;
	private final CriteriaBuilder cb;
	private final JPAEntityType jpaEntityType;

	protected JPAAbstractQuery(final JPAEntityType jpaResultType, final EntityManager em)
			throws ODataApplicationException {
		super();
		this.em = em;
		this.cb = em.getCriteriaBuilder();
		this.jpaEntityType = jpaResultType;
	}

	public final JPAEntityType getQueryResultType() {
		return jpaEntityType;
	}

	protected final EntityManager getEntityManager() {
		return em;
	}

	protected final CriteriaBuilder getCriteriaBuilder() {
		return cb;
	}

	/**
	 * @return A unique and reproducible alias name to access the attribute value in
	 *         the result set after loading
	 */
	static final String buildTargetJoinAlias(final JPAAssociationPath association,
			final JPASimpleAttribute targetAttribute) {
		return association.getAlias().concat("_").concat(targetAttribute.getInternalName());
	}

	/**
	 * The value of the $select query option is a comma-separated list of
	 * <b>properties</b>, qualified action names, qualified function names, the
	 * <b>star operator (*)</b>, or the star operator prefixed with the namespace or
	 * alias of the schema in order to specify all operations defined in the schema.
	 * See: <a href=
	 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398297"
	 * >OData Version 4.0 Part 1 - 11.2.4.1 System Query Option $select</a>
	 * <p>
	 * See also: <a href=
	 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398163"
	 * >OData Version 4.0 Part 2 - 5.1.3 System Query Option $select</a>
	 *
	 * @throws ODataApplicationException
	 */
	protected final List<Selection<?>> createSelectClause(final Collection<? extends JPASelector> jpaPathList)
			throws ODataJPAQueryException {

		final List<Selection<?>> selections = new LinkedList<Selection<?>>();

		// Build select clause
		for (final JPASelector jpaPath : jpaPathList) {
			// TODO 2. move logic into 'convertToCriteriaPath()'?

			// special join case for not mapped join columns
			if (JPAAssociationPath.class.isInstance(jpaPath)) {
				// happens for $expand queries without join columns mapped as attribute(s)
				final JPAAssociationPath asso = ((JPAAssociationPath) jpaPath);
				try {
					// join all the key attributes from target side table so we can build a 'result
					// key' from the tuples in the result set
					// TODO don't select path's multiple times
					final List<JPASimpleAttribute> keys = asso.getTargetType().getKeyAttributes(true);
					final Join<?, ?> join = getRoot()
							.join(asso.getSourceType().getAssociationByPath(asso).getInternalName());
					for (final JPASimpleAttribute jpaAttribute : keys) {
						final Path<?> p = join.get(jpaAttribute.getInternalName());
						p.alias(buildTargetJoinAlias(asso, jpaAttribute));
						selections.add(p);
					}
				} catch (final ODataJPAModelException e) {
					throw new ODataJPAQueryException(e, HttpStatusCode.NOT_ACCEPTABLE);
				}
				continue;
			}

			// default case
			final Path<?> p = convertToCriteriaPath(getRoot(), jpaPath);
			if (p == null) {
				continue;
			}
			// TODO 1. move 'alias' setting into 'convertToCriteriaPath()'?
			p.alias(jpaPath.getAlias());
			selections.add(p);
		}

		return selections;
	}

	protected static final Path<?> convertToCriteriaPath(final Root<?> root, final JPASelector jpaPath) {
		if (JPAAssociationPath.class.isInstance(jpaPath)) {
			throw new IllegalStateException("Handling of joins for associations must be happen outside this method");
		}
		Path<?> p = root;
		Join<?, ?> existingJoin;
		for (final JPAAttribute<?> jpaPathElement : jpaPath.getPathElements()) {
			if (jpaPathElement.isCollection()) {
				// we can cast, because 'p' is the Root<> or another Join<>
				existingJoin = findAlreadyDefinedJoin((From<?, ?>) p, jpaPathElement);
				if (existingJoin != null) {
					p = existingJoin;
				} else {
					// @ElementCollection's are loaded in separate queries, so an INNER JOIN is ok
					// to suppress results for not existing joined rows
					p = root.join(jpaPathElement.getInternalName(), JoinType.INNER);
				}
			} else {
				p = p.get(jpaPathElement.getInternalName());
			}
		}
		return p;
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

	protected final javax.persistence.criteria.Expression<Boolean> extendWhereByKey(final From<?, ?> root,
			final javax.persistence.criteria.Expression<Boolean> whereCondition, final List<UriParameter> keyPredicates)
					throws ODataApplicationException {
		// .../Organizations('3')
		// .../BusinessPartnerRoles(BusinessPartnerID='6',RoleCategory='C')
		if (keyPredicates == null || keyPredicates.isEmpty()) {
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

	/**
	 * Find an already created {@link Join} expression for the given attribute. The
	 * given attribute must be:
	 * <ul>
	 * <li>an attribute annotated with @ElementCollection</li>
	 * <li>the first path element in a {@link JPASelector}</li>
	 * </ul>
	 *
	 * @return The already existing {@link Join} or <code>null</code>.
	 */
	protected static final Join<?, ?> findAlreadyDefinedJoin(final From<?, ?> parentCriteriaPath,
			final JPAAttribute<?> jpaPathElement) {
		for (final Join<?, ?> join : parentCriteriaPath.getJoins()) {
			if (jpaPathElement.getInternalName().equals(join.getAttribute().getName())) {
				return join;
			}
		}
		return null;
	}

}