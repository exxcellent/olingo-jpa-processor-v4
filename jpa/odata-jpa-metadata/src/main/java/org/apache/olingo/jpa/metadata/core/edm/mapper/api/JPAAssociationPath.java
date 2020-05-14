package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Used for JPA relationships.
 *
 */
public interface JPAAssociationPath extends JPANavigationPath {

  /**
   *
   * @return TRUE if the join columns from left side and the join columns from the
   *         rights side are not matching, because an additional join table
   *         (from @JoinTable annotation) is existing.
   */
  public boolean hasJoinTableBetweenSourceAndTarget();

  /**
   *
   * @return The selectors defining the columns to select on right side of an
   * association.
   */
  @Deprecated
  public List<JPASelector> getRightPaths() throws ODataJPAModelException;

  JPAStructuredType getTargetType();

  JPAStructuredType getSourceType();

}