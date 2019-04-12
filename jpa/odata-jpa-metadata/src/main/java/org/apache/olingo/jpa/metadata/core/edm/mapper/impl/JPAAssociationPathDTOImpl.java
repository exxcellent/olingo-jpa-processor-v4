package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.Collections;
import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class JPAAssociationPathDTOImpl implements JPAAssociationPath {

	private final JPAStructuredType sourceType;
	private final JPAAssociationAttribute navProperty;
	final private List<JPAAttribute<?>> pathElements;

	public JPAAssociationPathDTOImpl(final JPAStructuredType source, final JPAAssociationAttribute navProperty) {
		this.sourceType = source;
		this.navProperty = navProperty;
		this.pathElements = Collections.singletonList(navProperty);
	}

	@Override
	public String getAlias() {
		return navProperty.getExternalName();
	}

	@Override
	public List<JPAAttribute<?>> getPathElements() {
		return pathElements;
	}

	@Override
	public JPAAttribute<?> getLeaf() {
		return pathElements.get(pathElements.size() - 1);
	}

	@Override
	public int compareTo(final JPASelector o) {
		if (o == null) {
			return -1;
		}
		return getAlias().compareTo(o.getAlias());
	}

	@Override
	public boolean hasJoinTableBetweenSourceAndTarget() {
		return false;
	}

	@Override
	public List<JPASelector> getLeftPaths() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPASelector> getRightPaths() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPAStructuredType getTargetType() {
		return navProperty.getStructuredType();
	}

	@Override
	public JPAStructuredType getSourceType() {
		return sourceType;
	}

}
