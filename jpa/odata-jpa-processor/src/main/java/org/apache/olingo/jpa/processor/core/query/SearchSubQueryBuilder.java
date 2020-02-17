package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.Decoder;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;
import org.apache.olingo.server.core.uri.parser.search.SearchTermImpl;

/**
 * Query builder for $search expressions.
 *
 * @author Ralf Zozmann
 *
 */
class SearchSubQueryBuilder extends AbstractSubQueryBuilder {

  private final SearchOption searchOption;
  private final JPAODataContext context;
  private final From<?, ?> subqueryResultFrom;

  public SearchSubQueryBuilder(final FilterContextQueryBuilderIfc parent, final SearchOption searchOption)
      throws ODataApplicationException,
      ODataJPAModelException {
    super(parent);
    this.searchOption = searchOption;
    this.context = parent.getContext();

    this.subqueryResultFrom = createSubqueryResultFrom();
  }

  public final Subquery<Integer> getSubQueryExists()
      throws ODataApplicationException {

    if (searchOption == null || searchOption.getSearchExpression() == null) {
      return null;
    }
    if (!searchOption.getSearchExpression().isSearchTerm()) {
      throw new UnsupportedOperationException("$search expression type not supported");
    }
    try {
      boolean attributesWithSearchableAnnotationFound = true;
      final JPAStructuredType jpaEntityType = getOwningQueryBuilder().getQueryResultType();
      List<JPASelector> searchableAttributes = jpaEntityType.getSearchablePath();
      if (searchableAttributes.isEmpty()) {
        LOG.log(Level.WARNING, "Entity " + jpaEntityType.getExternalName() + " has not attributes marked with @"
            + EdmSearchable.class.getSimpleName() + " annotation. Will use ALL attributes...");
        searchableAttributes = jpaEntityType.getPathList();
        attributesWithSearchableAnnotationFound = false;
      }

      final Subquery<Integer> subQuery = getSubQuery();

      // TODO remove in a proper working Hibernate version > 5.4.10?!
      // this is an workaround for buggy Hibernate not producing a invalid SQL (the <from> is missing without that
      // explicit thing)
      Expression<Boolean> joinDummyFromCorrelation = null;
      From<?, ?> scopeFrom = subqueryResultFrom;
      if (getEntityManager().getClass().getName().startsWith("org.hibernate")) {
        LOG.log(Level.WARNING, "Buggy Hibernate detected, use workaround for subquery of $search!");
        final From<?, ?> dummyFrom = subQuery.from(jpaEntityType.getTypeClass());
        scopeFrom = dummyFrom;
        joinDummyFromCorrelation = getCriteriaBuilder().equal(subqueryResultFrom, dummyFrom);
      }

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
        final Path<?> path = convertToCriteriaAliasPath(scopeFrom, searchableAttribute, null);
        columnList.add(path);
      }

      // EXISTS subselect needs only a marker select for existence
      subQuery.select(getCriteriaBuilder().literal(Integer.valueOf(1)));

      final Expression<Boolean> searchCondition = context.getDatabaseProcessor().createSearchExpression(term,
          columnList);
      if (searchCondition == null) {
        throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.NOT_SUPPORTED_SEARCH,
            HttpStatusCode.INTERNAL_SERVER_ERROR);
      }

      final Expression<Boolean> whereCondition = combineAND(joinDummyFromCorrelation, searchCondition);
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
