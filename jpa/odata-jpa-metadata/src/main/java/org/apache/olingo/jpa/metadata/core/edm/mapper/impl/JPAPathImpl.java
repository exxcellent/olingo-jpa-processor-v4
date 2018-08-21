package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class JPAPathImpl implements JPAAttributePath {
	final private String alias;
	final private List<JPAAttribute> pathElements;
	final private String dbFieldName;
	final private boolean ignore;

	JPAPathImpl(final String alias, final String dbFieldName, final JPAAttribute element) {
		final List<JPAAttribute> pathElementsBuffer = new ArrayList<JPAAttribute>();

		this.alias = alias;
		pathElementsBuffer.add(element);
		this.pathElements = Collections.unmodifiableList(pathElementsBuffer);
		this.dbFieldName = dbFieldName;
		this.ignore = element.ignore();
	}

	JPAPathImpl(final String selection, final String dbFieldName, final List<JPAAttribute> attribute)
			throws ODataJPAModelException {
		this.alias = selection;
		this.pathElements = Collections.unmodifiableList(attribute);
		this.dbFieldName = dbFieldName;
		this.ignore = ((IntermediateModelElement) pathElements.get(pathElements.size() - 1)).ignore();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final JPAPathImpl other = (JPAPathImpl) obj;
		if (alias == null) {
			if (other.alias != null) {
				return false;
			}
		} else if (!alias.equals(other.alias)) {
			return false;
		}
		if (pathElements == null) {
			if (other.pathElements != null) {
				return false;
			}
		} else if (!pathElements.equals(other.pathElements)) {
			return false;
		}
		return true;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public String getDBFieldName() {
		return dbFieldName;
	}

	@Override
	public JPAAttribute getLeaf() {
		return pathElements.get(pathElements.size() - 1);
	}

	@Override
	public List<JPAAttribute> getPathElements() {
		return pathElements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
		result = prime * result + ((pathElements == null) ? 0 : pathElements.hashCode());
		return result;
	}

	@Override
	public boolean ignore() {
		return ignore;
	}

	@Override
	public int compareTo(final JPASelector o) {
		if (o == null) {
			return -1;
		}
		return this.alias.compareTo(o.getAlias());
	}

	@Override
	public String toString() {
		return "JPAPathImpl [" + (alias != null ? "alias=" + alias + ", " : "")
				+ (pathElements != null ? "pathElements=" + pathElements + ", " : "")
				+ (dbFieldName != null ? "dbFieldName=" + dbFieldName + ", " : "") + "ignore=" + ignore + "]";
	}

}
