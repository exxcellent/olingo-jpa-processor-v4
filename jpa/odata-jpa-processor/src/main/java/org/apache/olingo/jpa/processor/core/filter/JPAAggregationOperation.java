package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Path;

public interface JPAAggregationOperation extends JPAExpression<Long> {

  JPAFilterAggregationType getAggregation();

  Path<?> getPath();

}