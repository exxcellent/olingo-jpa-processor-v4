package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates additional FROM clauses and WHERE conditions for navigation (of one association) as part of an existing
 * owning query.
 *
 */
class JPAQueryNavigation extends JPAAbstractQueryBuilder {

  private final List<UriParameter> keyPredicates;
  private final JPANavigationPath navigationPath;
  private final UriResourcePartTyped navigationResource;
  private final From<?, ?> joinedParentResultFrom;

  public <T extends Object> JPAQueryNavigation(final IntermediateServiceDocument sd,
      final UriResourcePartTyped navigationResource, final JPAAssociationPath association,
      final From<?, ?> parentFrom, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {

    super(sd, navigationResource.getType(), em);
    this.keyPredicates = determineKeyPredicates(navigationResource);
    this.navigationPath = association;
    this.navigationResource = navigationResource;
    if (navigationPath == null) {
      throw new IllegalArgumentException("selector required");
    }
    // if parent is an Entity query then this is he first sub query and 'scope from' is the correct
    // if parent is an navigation query the 'scope from' must be the target of navigation (== result from)
    joinedParentResultFrom = buildJoinPath(parentFrom, navigationPath);

  }

  protected final UriResourcePartTyped getQueryScopeUriInfoResource() {
    return navigationResource;
  }

  protected JPAEntityType getQueryResultType() {
    return getServiceDocument().getEntityType(navigationPath.getLeaf().getStructuredType().getExternalFQN());
  }

  protected final From<?, ?> getQueryResultFrom() {
    return joinedParentResultFrom;
  }

  public final Expression<Boolean> buildJoinWhere() throws ODataApplicationException {
    // navigation can only have keys
    return extendWhereByKey(joinedParentResultFrom, getQueryResultType(), this.keyPredicates);
  }
}
