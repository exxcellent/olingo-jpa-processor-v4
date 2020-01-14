package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.Decoder;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;
import org.apache.olingo.server.core.uri.parser.search.SearchTermImpl;

/**
 * Query builder for $search expressions.
 *
 * @author Ralf Zozmann
 *
 */
class JPASearchSubQuery extends JPAAbstractSubQuery {

  private From<?, ?> subqueryResultFrom = null;
  private final UriInfoResource uriResource;
  private final JPAODataDatabaseProcessor dbProcessor;

  public JPASearchSubQuery(final JPAAbstractEntityQuery<?, ?> parent) throws ODataApplicationException,
  ODataJPAModelException {
    super(parent.getServiceDocument(), parent.getQueryResultUriInfoResource().getType(), parent.getEntityManager(),
        parent);
    this.uriResource = parent.getUriInfoResource();
    this.dbProcessor = parent.getContext().getDatabaseProcessor();

    // context should be already prepared for filter queries
    initializeQuery();
  }

  @Override
  protected void initializeQuery() throws ODataJPAModelException, ODataApplicationException {
    this.subqueryResultFrom = createSubqueryResultFrom();
    super.initializeQuery();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> From<T, T> getQueryResultFrom() {
    assertInitialized();
    return (From<T, T>) subqueryResultFrom;
  }

  public final Subquery<Integer> getSubQueryExists()
      throws ODataApplicationException {
    final SearchOption searchOption = uriResource.getSearchOption();
    if (searchOption == null || searchOption.getSearchExpression() == null) {
      return null;
    }
    if (!searchOption.getSearchExpression().isSearchTerm()) {
      throw new UnsupportedOperationException("$search expression type not supported");
    }
    try {
      boolean attributesWithSearchableAnnotationFound = true;
      final JPAEntityType jpaEntityType = getQueryResultType();
      List<JPASelector> searchableAttributes = jpaEntityType.getSearchablePath();
      if (searchableAttributes.isEmpty()) {
        LOG.log(Level.WARNING, "Entity " + jpaEntityType.getExternalName() + " has not attributes marked with @"
            + EdmSearchable.class.getSimpleName() + " annotation. Will use ALL attributes...");
        searchableAttributes = jpaEntityType.getPathList();
        attributesWithSearchableAnnotationFound = false;
      }

      final Subquery<Integer> subQuery = getQuery();

      // EXISTS subselect needs only a marker select for existence
      subQuery.select(getCriteriaBuilder().literal(Integer.valueOf(1)));

      SearchTerm term = searchOption.getSearchExpression().asSearchTerm();

      // use double decoding to workaround OLINGO-1239
      String sTerm = term.getSearchTerm();
      sTerm = Decoder.decode(sTerm);
      term = new SearchTermImpl(sTerm);

      final List<Path<?>> columnList = new ArrayList<Path<?>>(searchableAttributes.size());
      for (final JPASelector searchableAttribute : searchableAttributes) {
        if (containsNavigationToOtherTable(searchableAttribute)) {
          if (attributesWithSearchableAnnotationFound) {
            LOG.log(Level.WARNING, "Collection attribute " + jpaEntityType.getExternalName() + "#"
                + searchableAttribute.getLeaf().getExternalName() + " cannot be evaluated by $search");
          }
          continue;
        }
        final Path<?> path = convertToCriteriaPath(subqueryResultFrom, searchableAttribute);
        path.alias(searchableAttribute.getAlias());
        columnList.add(path);
      }

      final javax.persistence.criteria.Expression<Boolean> whereCondition = dbProcessor.createSearchExpression(term, columnList);
      if (whereCondition == null) {
        throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.NOT_SUPPORTED_SEARCH,
            HttpStatusCode.INTERNAL_SERVER_ERROR);
      }
      subQuery.where(whereCondition);
      return subQuery;
    } catch (final ODataJPAModelException e) {
      throw new ODataJPADBAdaptorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   *
   * @return TRUE if any path element is uses a navigation to another table (like
   *         from BusinessPartner to Phone).
   */
  private boolean containsNavigationToOtherTable(final JPASelector path) {
    for (final JPAAttribute<?> attribute : path.getPathElements()) {
      // a collection is always a relationship (should not happen here) or an
      // @ElementCollection... both are navigating to another table
      if (attribute.isCollection()) {
        return true;
      }
    }
    return false;
  }

}
