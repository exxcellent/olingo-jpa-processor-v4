package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;

public interface JPAAggregationOperation extends JPAOperator<Expression<Long>> {

	JPAFilterAggregationType getAggregation();

}