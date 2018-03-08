package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOnConditionItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;

class JPAOnConditionItemImpl implements JPAOnConditionItem {
	private final JPASelector jpaLeftAttribute;
	private final JPASelector jpaRightAttribute;

	JPAOnConditionItemImpl(final JPASelector jpaLeftAttribute, final JPASelector jpaRightAttribute)
			throws IllegalArgumentException {
		super();
		this.jpaLeftAttribute = jpaLeftAttribute;
		if (jpaLeftAttribute == null) {
			throw new IllegalArgumentException("Left side required");
		}
		this.jpaRightAttribute = jpaRightAttribute;
		if (jpaRightAttribute == null) {
			throw new IllegalArgumentException("Right side required");
		}
	}

	@Override
	public JPASelector getLeftPath() {
		return jpaLeftAttribute;
	}

	@Override
	public JPASelector getRightPath() {
		return jpaRightAttribute;
	}

}
