package org.apache.olingo.jpa.processor.core.query;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Selection;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPANavigationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.query.result.NavigationKeyBuilder;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

/**
 * Creates additional FROM clauses and WHERE conditions for navigation (of one association) as part of an existing
 * owning query.
 *
 */
class NavigationBuilder extends AbstractQueryBuilder {

  private final JPAODataContext context;
  private final List<UriParameter> keyPredicates;
  private final UriResourcePartTyped navigationResource;
  private final From<?, ?> joinedParentResultFrom;
  private final JPAStructuredType resultEntityType;
  private final NavigationKeyBuilder navigationKeyBuilder;
  private final FilterOption filterOption;

  public <T extends Object> NavigationBuilder(final JPAODataContext context,
      final UriResourcePartTyped navigationResource, final JPANavigationPath association,
      final From<?, ?> parentFrom, final NavigationKeyBuilder parentNavigationKeyBuilder, final EntityManager em,
      final FilterOption filterOption)
          throws ODataApplicationException, ODataJPAModelException {

    super(em);
    this.context = context;
    if (UriResourceProperty.class.isInstance(navigationResource)) {
      // if the navigation is something of type 'property' then we are working for an @ElementCollection
      // in such a case we have to delegate all calls to the parent 'navigation builder', because all selection paths
      // are starting from the owning entity, so we have to avoid a separate JOIN by our own
      this.keyPredicates = null;
      this.resultEntityType = null;
      this.navigationResource = null;
      this.navigationKeyBuilder = null;
      this.joinedParentResultFrom = null;
      this.filterOption = null;
    } else {
      this.navigationResource = navigationResource;
      this.keyPredicates = determineKeyPredicates(navigationResource);
      // must point to entity
      this.resultEntityType = association.getLeaf().getStructuredType();
      // if parent is an Entity query then this is the first sub query and 'scope from' is the correct
      // if parent is an navigation query the 'scope from' must be the target of navigation (== result from)
      this.joinedParentResultFrom = buildJoinPath(parentFrom, association);
      this.navigationKeyBuilder = parentNavigationKeyBuilder.buildChildNavigation(association, resultEntityType);
      this.filterOption = filterOption;
    }
  }

  /**
   *
   * @return TRUE if navigation builder is prepared to produce Criteria API elements or FALSE if builder should be
   * skipped.
   */
  public boolean isWorking() {
    return resultEntityType != null;
  }

  protected final UriResourcePartTyped getNavigationUriInfoResource() {
    return navigationResource;
  }

  /**
   *
   * @return The entity type of target (result) JOIN of query part.
   */
  protected final JPAStructuredType getQueryResultType() {
    return resultEntityType;
  }

  protected final From<?, ?> getQueryResultFrom() {
    return joinedParentResultFrom;
  }

  final Expression<Boolean> buildJoinWhere() throws ODataApplicationException {
    if (keyPredicates == null) {
      return null;
    }
    // navigation can only have keys
    return extendWhereByKey(joinedParentResultFrom, getQueryResultType(), this.keyPredicates);
  }

  final List<Selection<?>> buildNavigationKeySelection() throws ODataApplicationException, ODataJPAModelException {
    if (navigationKeyBuilder == null) {
      return Collections.emptyList();
    }
    final List<JPASelector> listAssociationJoinKeyPaths = navigationKeyBuilder.getNavigationKeyPaths();
    final List<Selection<?>> joinSelections = new LinkedList<Selection<?>>();
    for (final JPASelector jPath : listAssociationJoinKeyPaths) {
      final Path<?> p = convertToCriteriaAliasPath(joinedParentResultFrom, jPath, navigationKeyBuilder.getNavigationAliasPrefix());
      if (p == null) {
        continue;
      }
      joinSelections.add(p);
    }
    return joinSelections;
  }

  public NavigationKeyBuilder getNavigationKeyBuilder() {
    return navigationKeyBuilder;
  }
}
