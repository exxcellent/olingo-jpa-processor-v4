package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

public interface JPASelector extends Comparable<JPASelector> {
	String PATH_SEPERATOR = "/";

	String getAlias();

	/**
	 * If there are more than 1 path elements, then the selector is navigating
	 * through a complex type.
	 */
	List<JPAAttribute<?>> getPathElements();

	/**
	 *
	 * @return The last {@link #getPathElements() path element}.
	 */
	JPAAttribute<?> getLeaf();

}
