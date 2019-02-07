package org.apache.olingo.jpa.processor.core.filter;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;

class JPAAggregationOperationCountImp implements JPAAggregationOperation {

	private final Root<?> root;
	private final JPAODataDatabaseProcessor converter;

	public JPAAggregationOperationCountImp(final Root<?> root, final JPAODataDatabaseProcessor converter) {
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
