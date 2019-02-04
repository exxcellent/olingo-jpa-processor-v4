package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAssociationPath extends JPASelector {

	/**
	 *
	 * @return TRUE if the join columns from left side and the join columns from the
	 *         rights side are not matching, because an additional join table
	 *         (from @JoinTable annotation) is existing.
	 */
	public boolean hasJoinTableBetweenSourceAndTarget();

	/**
	 *
	 * @return The selectors defining the columns to select on left side of an
	 *         association.
	 */
	public List<JPASelector> getLeftPaths() throws ODataJPAModelException;

	/**
	 *
	 * @return The selectors defining the columns to select on right side of an
	 *         association.
	 */
	public List<JPASelector> getRightPaths() throws ODataJPAModelException;

	JPAStructuredType getTargetType();

	JPAStructuredType getSourceType();

}