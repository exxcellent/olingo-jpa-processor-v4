package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

abstract class AbstractQueryBuilder {

  protected final static Logger LOG = Logger.getLogger(AbstractQueryBuilder.class.getName());

  private final EntityManager em;
  private final CriteriaBuilder cb;

  protected AbstractQueryBuilder(final EntityManager em) throws ODataApplicationException, ODataJPAModelException {
    super();
    this.em = em;
    this.cb = em.getCriteriaBuilder();
  }

  public final EntityManager getEntityManager() {
    return em;
  }

  protected final CriteriaBuilder getCriteriaBuilder() {
    return cb;
  }

  protected final javax.persistence.criteria.Expression<Boolean> combineAND(
      javax.persistence.criteria.Expression<Boolean> whereCondition,
      final javax.persistence.criteria.Expression<Boolean> additionalExpression) {

    if (additionalExpression != null) {
      if (whereCondition == null) {
        whereCondition = additionalExpression;
      } else {
        whereCondition = getCriteriaBuilder().and(whereCondition, additionalExpression);
      }
    }
    return whereCondition;
  }

  private Path<?> buildPath(final From<?, ?> from, final JPAStructuredType entity, final UriParameter keyPredicate)
      throws ODataJPAModelException {
    Path<?> path = from;
    final JPASelector selector = entity.getPath(keyPredicate.getName());
    for (final JPAAttribute<?> attribute : selector.getPathElements()) {
      path = path.get(attribute.getInternalName());
    }
    if (path == from) {
      throw new ODataJPAModelException(MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
    }
    return path;
  }

  /**
   *
   * @return A condition for existing key predicates or <code>null</code>.
   */
  protected final javax.persistence.criteria.Expression<Boolean> extendWhereByKey(final From<?, ?> root,
      final JPAStructuredType entity, final List<UriParameter> keyPredicates)
          throws ODataApplicationException {
    // .../Organizations('3')
    // .../BusinessPartnerRoles(BusinessPartnerID='6',RoleCategory='C')
    if (keyPredicates == null || keyPredicates.isEmpty()) {
      return null;
    }
    javax.persistence.criteria.Expression<Boolean> compoundCondition = null;

    for (final UriParameter keyPredicate : keyPredicates) {
      if (keyPredicate.getText() == null) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_KEY_PROPERTY_ERROR,
            HttpStatusCode.BAD_REQUEST, keyPredicate.getName());
      }
      try {
        final Path<?> path = buildPath(root, entity, keyPredicate);
        final javax.persistence.criteria.Expression<Boolean> equalCondition = cb.equal(path, eliminateApostrophe(
            keyPredicate.getText()));
        compoundCondition = combineAND(compoundCondition, equalCondition);
      } catch (final ODataJPAModelException e) {
        throw new ODataJPAQueryException(e, HttpStatusCode.BAD_REQUEST);
      }
    }
    return compoundCondition;
  }

  private String eliminateApostrophe(final String text) {
    return text.replaceAll("'", "");
  }

  /**
   *
   * @param association The association to take {@link JPAAssociationPath#getPathElements() path elements} from it and
   * build {@link #buildJoinPath(From, String...) join path}.
   */
  protected static final From<?, ?> buildJoinPath(final From<?, ?> from, final JPANavigationPath association) {
    final List<JPAAttribute<?>> pathElements = association.getPathElements();
    return buildJoinPath(from, pathElements.toArray(new JPAAttribute[pathElements.size()]));
  }

  /**
   *
   * @param from The starting {@link From}
   * @param joins The relationship/navigation path with multiple path elemens to join
   * @return The {@link Join} or original {@link From} if join path is <code>null</code> or empty.
   */
  protected static final From<?, ?> buildJoinPath(final From<?, ?> from, final JPAAttribute<?>... joins) {
    if (joins == null || joins.length == 0) {
      return from;
    }
    From<?, ?> current = from;
    for (final JPAAttribute<?> join : joins) {
      if (join.isComplex()) {
        current = current.join(join.getInternalName(), JoinType.INNER);
      } else {
        current = current.join(join.getInternalName(), JoinType.LEFT);
      }
    }
    return current;
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

  /**
   *
   * @param aliasPrefix If <code>null</code> the selector alias will be used without prefix
   */
  protected final Path<?> convertToCriteriaPath(final From<?, ?> from, final JPASelector jpaPath,
      final String aliasPrefix) {
    if (JPAAssociationPath.class.isInstance(jpaPath)) {
      throw new IllegalStateException("Handling of joins for associations must be happen outside this method");
    }
    Path<?> p = from;
    Join<?, ?> existingJoin;
    for (final JPAAttribute<?> jpaPathElement : jpaPath.getPathElements()) {
      if (jpaPathElement.isCollection() && From.class.isInstance(p)) {
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
    if (p != null) {
      if (aliasPrefix == null) {
        p.alias(jpaPath.getAlias());
      } else {
        p.alias(aliasPrefix.concat(jpaPath.getAlias()));
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

}
