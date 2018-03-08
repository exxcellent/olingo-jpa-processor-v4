package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

public interface JPASelector extends Comparable<JPASelector> {
	String PATH_SEPERATOR = "/";

	String getAlias();

	/**
	 * The last path element must be instance of {@link JPAAttribute}.
	 */
	List<JPAAttribute> getPathElements();

	/**
	 *
	 * @return The last {@link #getPathElements() path element}.
	 */
	JPAAttribute getLeaf();

}
