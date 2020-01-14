package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
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
import org.apache.olingo.jpa.processor.core.exception.ODataJPAQueryException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

class JPAAbstractQueryBuilder {

  private final EntityManager em;
  private final CriteriaBuilder cb;
  private final EdmType edmType;
  private final IntermediateServiceDocument sd;

  protected JPAAbstractQueryBuilder(final IntermediateServiceDocument sd, final EdmType edmType,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super();
    this.em = em;
    this.sd = sd;
    this.cb = em.getCriteriaBuilder();
    this.edmType = edmType;
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

  protected final EntityManager getEntityManager() {
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

  private Path<?> buildPath(final From<?, ?> from, final JPAEntityType entity, final UriParameter keyPredicate)
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
      final JPAEntityType entity, final List<UriParameter> keyPredicates)
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

}
