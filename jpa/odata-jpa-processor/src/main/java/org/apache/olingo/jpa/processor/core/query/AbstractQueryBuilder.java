package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.EntityManager;
import javax.persistence.SecondaryTable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
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
    // validate requested join path
    for (final JPAAttribute<?> pathAttribute : joins) {
      if (pathAttribute.isAssociation()) {
        continue;
      }
      if (pathAttribute.isCollection()) {
        continue;
      }
      if (pathAttribute.isComplex()) {
        continue;
      }
      throw new IllegalStateException("Attribute/Path " + pathAttribute.getInternalName()
      + " is neither association nor collection or complex type");
    }

    Path<?> current = from;
    for (final JPAAttribute<?> pathAttribute : joins) {
      current = buildPath(current, pathAttribute);
    }
    // safe to cast, because we have validated before
    return (From<?, ?>) current;
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

  private static Path<?> buildPath(final Path<?> from, final JPAAttribute<?> jpaPathElement) {
    Join<?, ?> existingJoin;
    // only @ElementCollection and true relationships should be handled as JOIN, a single complex type (@Embedded) must
    // taken as simple attribute as JPA specification says
    if (From.class.isInstance(from) && jpaPathElement.isComplex() || jpaPathElement.isCollection()) {
      existingJoin = findAlreadyDefinedJoin((From<?, ?>) from, jpaPathElement);
      if (existingJoin != null) {
        return existingJoin;
      } else {
        if (jpaPathElement.isCollection() && !jpaPathElement.isAssociation()) {
          // @ElementCollection are always optional for the root entity, but loaded in a separate call... so we can use
          // INNER JOIN also to avoid empty results
          return ((From<?, ?>) from).join(jpaPathElement.getInternalName());
        } else {
          // association (real navigations) must be existing (INNER JOIN)
          // @Embedded types cannot be null per JPA specification, so we have to use an INNER JOIN
          return ((From<?, ?>) from).join(jpaPathElement.getInternalName());
        }
      }
    } else {
      return from.get(jpaPathElement.getInternalName());
    }
  }

  /**
   *
   * @param aliasPrefix If <code>null</code> the selector alias will be used without prefix
   */
  protected final Path<?> convertToCriteriaAliasPath(final From<?, ?> from, final JPASelector jpaPath,
      final String aliasPrefix) {
    if (JPAAssociationPath.class.isInstance(jpaPath)) {
      throw new IllegalStateException("Handling of joins for associations must be happen outside this method");
    }
    Path<?> p = from;
    for (final JPAAttribute<?> jpaPathElement : jpaPath.getPathElements()) {
      p = buildPath(p, jpaPathElement);
      // single @Embedded -> must create a JOIN
      validateCorrectAttributeOverride(from, p, jpaPathElement);
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
   * Normally only Hibernate will produce invalid queries covered by this check
   */
  private void validateCorrectAttributeOverride(final From<?, ?> from, final Path<?> path,
      final JPAAttribute<?> jpaPathElement) {
    if (path == null) {
      return;
    }
    if (!JPATypedElement.class.isInstance(jpaPathElement)) {
    }
    if (jpaPathElement.isCollection()) {
      return;
    }
    // only check the entry path element -> the complex one
    if (!jpaPathElement.isComplex()) {
      return;
    }
    if (jpaPathElement.isKey()) {
      return;
    }
    // with a JOIN no problem is present
    if (Join.class.isInstance(path)) {
      return;
    }
    // an (complex) attribute using @AttributeOverride's pointing to another table, must also declare that other table
    // as @SecondaryTable for the entity...
    // but only EclipseLink will produce a proper working JOIN
    final SecondaryTable st = from.getJavaType().getAnnotation(SecondaryTable.class);
    if (st == null) {
      return;
    }
    final JPATypedElement attribute = JPATypedElement.class.cast(jpaPathElement);
    final AttributeOverrides overrides = attribute.getAnnotation(AttributeOverrides.class);
    final AttributeOverride override = attribute.getAnnotation(AttributeOverride.class);
    AttributeOverride[] arrDefs;
    if (overrides != null) {
      arrDefs = overrides.value();
    } else if (override != null) {
      arrDefs = new AttributeOverride[] { override };
    } else {
      return;
    }
    boolean failPresent = false;
    for (final AttributeOverride o : arrDefs) {
      if (o.column() != null && o.column().table().equals(st.name())) {
        failPresent = true;
        break;
      }
    }
    if (!failPresent) {
      return;
    }
    LOG.log(Level.WARNING, "Dubious scenario detected: The attribute " + from.getJavaType().getSimpleName() + "#"
        + jpaPathElement.getInternalName()
        + " seems to be mapped using another table (@" + SecondaryTable.class.getSimpleName()
        + "), but the Criteria API selection path results not in a '" + Join.class.getSimpleName()
        + "'. Maybe the produced query will be invalid!");
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
  private static Join<?, ?> findAlreadyDefinedJoin(final From<?, ?> parentCriteriaPath,
      final JPAAttribute<?> jpaPathElement) {
    for (final Join<?, ?> join : parentCriteriaPath.getJoins()) {
      if (jpaPathElement.getInternalName().equals(join.getAttribute().getName())) {
        return join;
      }
    }
    return null;
  }

}
