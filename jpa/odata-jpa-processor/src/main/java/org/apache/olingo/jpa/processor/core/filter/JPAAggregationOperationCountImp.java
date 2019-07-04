package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;

class JPAAggregationOperationCountImp implements JPAAggregationOperation {

	private final From<?, ?> root;
	private final JPAODataDatabaseProcessor converter;

	public JPAAggregationOperationCountImp(final From<?, ?> root, final JPAODataDatabaseProcessor converter) {
		this.root = root;
		this.converter = converter;
	}

	@Override
	public Expression<Long> get() throws ODataApplicationException {
		return converter.convert(this);
	}

	@Override
	public JPAFilterAggregationType getAggregation() {
		return JPAFilterAggregationType.COUNT;
	}

	@Override
	public Path<?> getPath() {
		return root; // keyPathList;
	}

}
