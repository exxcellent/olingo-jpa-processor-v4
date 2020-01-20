package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.JoinColumn;

/**
 * The mapping is always defined as navigate 'from' ([source] entity column)
 * 'to' ([target] entity or join table column).
 *
 */
class IntermediateJoinColumn {
  private String sourceColumnName;
  private String targetColumnName;

  /**
   * Same as {@link #IntermediateJoinColumn(String, String) IntermediateJoinColumn(jpaJoinColumn.name(),
   * jpaJoinColumn.referencedColumnName())}
   */
  public IntermediateJoinColumn(final JoinColumn jpaJoinColumn) {
    this(jpaJoinColumn.name(), jpaJoinColumn.referencedColumnName());
  }

  public IntermediateJoinColumn(final String sourceColumnName, final String targetColumnName) {
    super();
    this.sourceColumnName = sourceColumnName;
    this.targetColumnName = targetColumnName;
  }

  /**
   * @see JoinColumn#name()
   */
  public String getSourceEntityColumnName() {
    return sourceColumnName;
  }

  public void setSourceEntityColumnName(final String name) {
    this.sourceColumnName = name;
  }

  /**
   * @see JoinColumn#referencedColumnName()
   */
  public String getTargetColumnName() {
    return targetColumnName;
  }

  /**
   *
   * @param referencedColumnName The name or target entity or join table column.
   */
  public void setTargetColumnName(final String referencedColumnName) {
    this.targetColumnName = referencedColumnName;
  }

}
