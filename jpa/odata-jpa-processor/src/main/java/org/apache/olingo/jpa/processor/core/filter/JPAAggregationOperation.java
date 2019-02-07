package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

public interface JPAAggregationOperation extends JPAExpression<Expression<Long>> {

	JPAFilterAggregationType getAggregation();

	Path<?> getPath();

}