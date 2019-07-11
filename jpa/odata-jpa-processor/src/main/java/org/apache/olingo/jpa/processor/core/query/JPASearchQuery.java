package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmSearchable;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.exception.ODataJPADBAdaptorException;
import org.apache.olingo.jpa.processor.core.filter.JPAComparisonOperatorImp;
import org.apache.olingo.jpa.processor.core.filter.JPAExpression;
import org.apache.olingo.jpa.processor.core.filter.JPAExpressionElement;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralOperator;
import org.apache.olingo.jpa.processor.core.filter.JPALiteralTypeOperator;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.search.SearchTerm;
import org.apache.olingo.server.core.uri.queryoption.expression.LiteralImpl;

/**
 * Query builder for $search expressions.
 *
 * @author Ralf Zozmann
 *
 */
class JPASearchQuery extends JPAAbstractQuery<Subquery<Integer>, Integer> {

	private static class SearchPathJPAExpression<T extends Expression<?>> implements JPAExpression<T> {
		private final T path;

		public SearchPathJPAExpression(final T path) {
			this.path = path;
		}

		@Override
		public T get() throws ODataApplicationException {
			return path;
		}

	}

	private final OData odata;
	private From<?, ?> queryRoot = null;
	private Subquery<Integer> subQuery = null;
	private final UriInfoResource uriResource;
	private final JPAAbstractCriteriaQuery<?, ?> parent;
	private final JPAODataDatabaseProcessor dbProcessor;

	public JPASearchQuery(final JPAAbstractCriteriaQuery<?, ?> parent) throws ODataApplicationException {
		super(parent.getQueryResultType(), parent.getEntityManager());
		this.odata = parent.getOData();
		this.parent = parent;
		this.uriResource = parent.getUriInfoResource();
		this.dbProcessor = parent.getContext().getDatabaseProcessor();

	}

	@Override
	protected JPAODataContext getContext() {
		return parent.getContext();
	}

	@Override
	public Subquery<Integer> getQuery() {
		if (subQuery == null) {
			throw new IllegalStateException();
		}
		return subQuery;
	}

	@Override
	protected final Locale getLocale() {
		return getContext().getLocale();
	}

	@SuppressWarnings("unchecked")
	@Override
	public From<?, ?> getRoot() {
		if (queryRoot == null) {
			throw new IllegalStateException();
		}
		return queryRoot;
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

			this.subQuery = parent.getQuery().subquery(Integer.class);// we select always '1'
			// Hibernate needs an explicit FROM to work properly
			/* final Root<?> from = */ subQuery.from(jpaEntityType.getTypeClass());

			final From<?, ?> parentFrom = parent.getRoot();
			if (parentFrom instanceof Root) {
				this.queryRoot = subQuery.correlate((Root<?>) parentFrom);
			} else {
				// part of another subquery?!
				this.queryRoot = subQuery.correlate((Join<?, ?>) parentFrom);
			}
			//			this.queryRoot = subQuery.from(getQueryResultType().getTypeClass());

			// EXISTS subselect needs only a marker select for existence
			subQuery.select(getCriteriaBuilder().literal(Integer.valueOf(1)));

			final SearchTerm term = searchOption.getSearchExpression().asSearchTerm();
			javax.persistence.criteria.Expression<Boolean> whereCondition = null;
			javax.persistence.criteria.Expression<Boolean> condition;
			for (final JPASelector searchableAttribute : searchableAttributes) {
				if (containsNavigationToOtherTable(searchableAttribute)) {
					if (attributesWithSearchableAnnotationFound) {
						LOG.log(Level.WARNING, "Collection attribute " + jpaEntityType.getExternalName() + "#"
								+ searchableAttribute.getLeaf().getExternalName() + " cannot be evaluated by $search");
					}
					continue;
				}
				final Path<?> path = convertToCriteriaPath(queryRoot, searchableAttribute);
				path.alias(searchableAttribute.getAlias());
				condition = buildColumnCondition(path, term);
				whereCondition = combineOR(whereCondition, condition);
			}

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

	private Expression<Boolean> buildColumnCondition(final Path<?> pathColumn,
			final SearchTerm term) throws ODataApplicationException {
		final JPAExpression<Path<?>> anonymousMemberOperator = new SearchPathJPAExpression<>(pathColumn);
		final Class<?> javaType = pathColumn.getJavaType();
		if (javaType == String.class || javaType == UUID.class || javaType == URI.class || javaType == URL.class) {
			return buildColumnCondition4String(anonymousMemberOperator, term);
		} else if (javaType == Boolean.class || javaType == boolean.class) {
			return buildColumnCondition4Boolean(anonymousMemberOperator, term);
		} else if (Number.class.isAssignableFrom(javaType)) {
			return buildColumnCondition4Number(anonymousMemberOperator, term);
		}
		LOG.log(Level.WARNING,
				"Unsupported data type " + javaType.getName() + " in '" + pathColumn.getAlias() + "' for @"
						+ EdmSearchable.class.getSimpleName());
		return null;
	}

	@SuppressWarnings("unchecked")
	private Expression<Boolean> buildColumnCondition4Number(JPAExpression<?> memberOperand, final SearchTerm term)
			throws ODataApplicationException {
		// 1. try to cast number to string (optional)
		try {
			final List<JPAExpressionElement<?>> castParameters = new ArrayList<JPAExpressionElement<?>>(2);
			castParameters.add(memberOperand);
			final JPALiteralTypeOperator typeOperand = new JPALiteralTypeOperator(
					EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String));
			castParameters.add(typeOperand);
			final Expression<?> castExpression = dbProcessor.convertBuiltinFunction(MethodKind.CAST, castParameters);
			// wrap imto cast expression
			memberOperand = new SearchPathJPAExpression<>(castExpression);
		} catch (final ODataApplicationException ex) {
			LOG.log(Level.WARNING, "CAST not supported to convert " + ((Path<?>) memberOperand.get()).getJavaType()
					+ " into String for $search... try without cast, but that may fail on some databases!");
		}
		// 2. generate LIKE expression for number
		final JPAExpressionElement<?> stringOperand = new JPALiteralOperator(odata, getCriteriaBuilder(),
				new LiteralImpl("'" + term.getSearchTerm() + "'",
						EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String)));
		final List<JPAExpressionElement<?>> conversionParameters = new ArrayList<JPAExpressionElement<?>>(2);
		conversionParameters.add(memberOperand);
		conversionParameters.add(stringOperand);
		return (javax.persistence.criteria.Expression<Boolean>) dbProcessor.convertBuiltinFunction(MethodKind.CONTAINS,
				conversionParameters);
	}

	@SuppressWarnings("unchecked")
	private Expression<Boolean> buildColumnCondition4String(final JPAExpression<?> memberOperand,
			final SearchTerm term) throws ODataApplicationException {
		final JPAExpressionElement<?> stringOperand = new JPALiteralOperator(odata, getCriteriaBuilder(),
				new LiteralImpl("'" + term.getSearchTerm() + "'", EdmString.getInstance()));
		final List<JPAExpressionElement<?>> conversionParameters = new ArrayList<JPAExpressionElement<?>>(2);
		conversionParameters.add(memberOperand);
		conversionParameters.add(stringOperand);
		return (javax.persistence.criteria.Expression<Boolean>) dbProcessor.convertBuiltinFunction(MethodKind.CONTAINS,
				conversionParameters);
	}

	private Expression<Boolean> buildColumnCondition4Boolean(final JPAExpression<?> memberOperand,
			final SearchTerm term) throws ODataApplicationException {
		String bValue;
		if ("true".equalsIgnoreCase(term.getSearchTerm()) || "1".equalsIgnoreCase(term.getSearchTerm())) {
			bValue = "true";
		} else if("false".equalsIgnoreCase(term.getSearchTerm()) || "0".equalsIgnoreCase(term.getSearchTerm())) {
			bValue = "false";
		} else {
			//do not search for the default boolean behaviour 'false' if not parsable
			return null;
		}
		final JPAExpressionElement<?> booleanOperand = new JPALiteralOperator(odata, getCriteriaBuilder(),
				new LiteralImpl(bValue, EdmBoolean.getInstance()));
		final List<JPAExpressionElement<?>> conversionParameters = new ArrayList<JPAExpressionElement<?>>(2);
		conversionParameters.add(memberOperand);
		conversionParameters.add(booleanOperand);
		final JPAComparisonOperatorImp<Boolean> comparator = new JPAComparisonOperatorImp<>(dbProcessor,
				BinaryOperatorKind.EQ,
				memberOperand, booleanOperand);
		return comparator.get();
	}

	private javax.persistence.criteria.Expression<Boolean> combineOR(
			javax.persistence.criteria.Expression<Boolean> whereCondition,
			final javax.persistence.criteria.Expression<Boolean> additionalExpression) {

		if (additionalExpression != null) {
			if (whereCondition == null) {
				whereCondition = additionalExpression;
			} else {
				whereCondition = getCriteriaBuilder().or(whereCondition, additionalExpression);
			}
		}
		return whereCondition;
	}
}
