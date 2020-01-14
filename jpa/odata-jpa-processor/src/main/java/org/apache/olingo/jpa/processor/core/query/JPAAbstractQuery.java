package org.apache.olingo.jpa.processor.core.query;

import java.util.Locale;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.server.api.ODataApplicationException;

public abstract class JPAAbstractQuery<QT extends AbstractQuery<RT>, RT> extends JPAAbstractQueryBuilder {

  protected static enum InitializationState {
    NotInitialized, Initialized;
  }

  protected final static Logger LOG = Logger.getLogger(JPAAbstractQuery.class.getName());

  protected static final String SELECT_ITEM_SEPERATOR = ",";
  protected static final String SELECT_ALL = "*";

  private InitializationState initStateType = InitializationState.NotInitialized;

  protected JPAAbstractQuery(final IntermediateServiceDocument sd, final EdmType edmType,
      final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {
    super(sd, edmType, em);
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

  /**
   *
   * @return The query {@link #getQueryResultFrom() result} entity type (selection from the last joined table).
   */
  public abstract JPAEntityType getQueryResultType();

  protected abstract Locale getLocale();

  abstract JPAODataContext getContext();

}