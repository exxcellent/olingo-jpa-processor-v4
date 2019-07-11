package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
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

public abstract class JPAAbstractQuery<QT extends AbstractQuery<RT>, RT> {

	protected final static Logger LOG = Logger.getLogger(JPAAbstractQuery.class.getName());

	protected static final String SELECT_ITEM_SEPERATOR = ",";
	protected static final String SELECT_ALL = "*";

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

	protected static List<UriParameter> determineKeyPredicates(final UriResource uriResourceItem)
			throws ODataApplicationException {

		if (uriResourceItem instanceof UriResourceEntitySet) {
			return ((UriResourceEntitySet) uriResourceItem).getKeyPredicates();
		} else if (uriResourceItem instanceof UriResourceNavigation) {
			return ((UriResourceNavigation) uriResourceItem).getKeyPredicates();
		}
		return Collections.emptyList();
	}

	protected final Path<?> convertToCriteriaPath(final From<?, ?> from, final JPASelector jpaPath) {
		if (JPAAssociationPath.class.isInstance(jpaPath)) {
			throw new IllegalStateException("Handling of joins for associations must be happen outside this method");
		}
		Path<?> p = from;
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
					p = from.join(jpaPathElement.getInternalName(), JoinType.INNER);
				}
			} else {
				p = p.get(jpaPathElement.getInternalName());
			}
		}
		return p;
	}

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
	private Join<?, ?> findAlreadyDefinedJoin(final From<?, ?> parentCriteriaPath,
			final JPAAttribute<?> jpaPathElement) {
		for (final Join<?, ?> join : parentCriteriaPath.getJoins()) {
			if (jpaPathElement.getInternalName().equals(join.getAttribute().getName())) {
				return join;
			}
		}
		return null;
	}

	public abstract <TT extends From<T, T>, T> TT getRoot();

	public abstract QT getQuery();

	protected abstract Locale getLocale();

	abstract JPAODataContext getContext();

}