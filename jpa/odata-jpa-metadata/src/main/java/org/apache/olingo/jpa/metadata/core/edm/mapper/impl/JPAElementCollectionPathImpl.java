package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElementCollectionPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;

public final class JPAElementCollectionPathImpl implements JPAElementCollectionPath {

	private final JPASelector selector;

	public JPAElementCollectionPathImpl(final JPASelector selector) {
		this.selector = selector;
	}

	@Override
	public String getAlias() {
		return selector.getAlias();
	}

	@Override
	public List<JPAAttribute<?>> getPathElements() {
		return selector.getPathElements();
	}

	@Override
	public JPAAttribute<?> getLeaf() {
		return selector.getLeaf();
	}

	@Override
	public int compareTo(final JPASelector o) {
		return selector.compareTo(o);
	}


}
