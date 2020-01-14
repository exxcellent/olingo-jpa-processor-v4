package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

/**
 * Creates a sub query for a navigation.
 * @author Oliver Grande
 *
 */
public class JPANavigationQuery extends JPAAbstractRelationshipQuery {

  private From<?, ?> joinedParentRelatedResultFrom = null;

  public <T extends Object> JPANavigationQuery(final IntermediateServiceDocument sd,
      final UriResourcePartTyped navigationResource, final JPAAssociationPath association,
      final JPAAbstractQuery<?, ?> parent, final EntityManager em)
          throws ODataApplicationException, ODataJPAModelException {

    super(sd, navigationResource, association, parent, em);
  }

  @Override
  protected final void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    // if parent is an Entity query then this is he first sub query and 'scope from' is the correct
    // if parent is an navigation query the 'scope from' must be the target of navigation (== result from)
    joinedParentRelatedResultFrom = buildJoinPath(getParentQuery().getQueryScopeFrom(), getSelector());

    super.initializeQuery();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> From<T, T> getQueryScopeFrom() {
    assertInitialized();
    return (From<T, T>) joinedParentRelatedResultFrom;
  }

  @Override
  protected List<Expression<?>> handleAggregation(final Subquery<?> subQuery, final From<?, ?> subRoot)
      throws ODataApplicationException, ODataJPAModelException {
    // do nothing
    return Collections.emptyList();
  }

  @Override
  protected Expression<Boolean> createSubqueryWhereByAssociation()
      throws ODataApplicationException, ODataJPAModelException {
    // no WHERE for relationship self required, we are joining...
    return null;
  }

}
