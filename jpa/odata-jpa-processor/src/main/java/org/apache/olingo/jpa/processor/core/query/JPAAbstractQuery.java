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

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

public abstract class JPAAbstractQuery<QT extends AbstractQuery<RT>, RT> {

  protected static enum InitializationState {
    NotInitialized, Initialized;
  }

  protected final static Logger LOG = Logger.getLogger(JPAAbstractQuery.class.getName());

  protected static final String SELECT_ITEM_SEPERATOR = ",";
  protected static final String SELECT_ALL = "*";

  private final EntityManager em;
  private final CriteriaBuilder cb;
  private final JPAEntityType jpaEntityType;
  private final EdmType edmType;
  private final IntermediateServiceDocument sd;
  private InitializationState initStateType = InitializationState.NotInitialized;

  protected JPAAbstractQuery(final IntermediateServiceDocument sd, final EdmType edmType,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super();
    this.sd = sd;
    this.em = em;
    this.cb = em.getCriteriaBuilder();
    this.edmType = edmType;
    this.jpaEntityType = sd.getEntityType(edmType);
    assert jpaEntityType != null;
  }

  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    if (initStateType == InitializationState.Initialized) {
      throw new IllegalStateException("Already initialized");
    }
    initStateType = InitializationState.Initialized;
  }

  protected void assertInitialized() {
    if (initStateType != InitializationState.Initialized) {
      throw new IllegalStateException("Not initialized");
    }
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

  protected final IntermediateServiceDocument getServiceDocument() {
    return sd;
  }

  /**
   *
   * @return The {@link #getQueryScopeFrom() starting} query edm type.
   */
  public final EdmType getQueryScopeEdmType() {
    return edmType;
  }

  /**
   *
   * @return The {@link #getQueryScopeFrom() starting} query entity type.
   */
  public final JPAEntityType getQueryScopeType() {
    return jpaEntityType;
  }

  protected final EntityManager getEntityManager() {
    return em;
  }

  protected final CriteriaBuilder getCriteriaBuilder() {
    return cb;
  }

  // TODO replace 'from' by #getQueryFrom()
  private Path<?> buildPath(final From<?, ?> from, final UriParameter keyPredicate) throws ODataJPAModelException {
    Path<?> path = from;
    final JPASelector selector = getQueryScopeType().getPath(keyPredicate.getName());
    for (final JPAAttribute<?> attribute : selector.getPathElements()) {
      path = path.get(attribute.getInternalName());
    }
    if (path == from) {
      throw new ODataJPAModelException(MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
    }
    return path;
  }

  // TODO remove 'whereCondition' as parameter
  protected final javax.persistence.criteria.Expression<Boolean> extendWhereByKey(final From<?, ?> root,
      final javax.persistence.criteria.Expression<Boolean> whereCondition, final List<UriParameter> keyPredicates)
          throws ODataApplicationException {
    assertInitialized();
    // .../Organizations('3')
    // .../BusinessPartnerRoles(BusinessPartnerID='6',RoleCategory='C')
    if (keyPredicates == null || keyPredicates.isEmpty()) {
      return whereCondition;
    }
    javax.persistence.criteria.Expression<Boolean> compundCondition = whereCondition;

    for (final UriParameter keyPredicate : keyPredicates) {
      if (keyPredicate.getText() == null) {
        throw new ODataJPAQueryException(ODataJPAQueryException.MessageKeys.QUERY_RESULT_KEY_PROPERTY_ERROR,
            HttpStatusCode.BAD_REQUEST, keyPredicate.getName());
      }
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

  /**
   *
   * @return The {@link From target} entity table. Maybe the same as {@link #getQueryScopeFrom()} if no navigation
   * (join) is involved.
   */
  public abstract <T> From<T, T> getQueryResultFrom();

  /**
   *
   * @return The initial {@link From starting} entity table before first join.
   */
  public abstract <T> From<T, T> getQueryScopeFrom();

  public abstract QT getQuery();

  protected abstract UriResource getQueryScopeUriInfoResource();

  /**
   *
   * @return The query {@link #getQueryResultFrom() result} entity type (selection from the last joined table).
   */
  protected abstract JPAEntityType getQueryResultType();

  protected abstract Locale getLocale();

  abstract JPAODataContext getContext();

}