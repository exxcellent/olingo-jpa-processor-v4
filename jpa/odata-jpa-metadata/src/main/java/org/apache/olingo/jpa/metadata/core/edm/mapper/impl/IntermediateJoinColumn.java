package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.JoinColumn;

/**
 * The mapping is always defined as navigate 'from' ([source] entity column)
 * 'to' ([target] entity or join table column).
 *
 */
class IntermediateJoinColumn {
	private String name;
	private String referencedColumnName;

	public IntermediateJoinColumn(final JoinColumn jpaJoinColumn) {
		this(jpaJoinColumn.name(), jpaJoinColumn.referencedColumnName());
	}

	public IntermediateJoinColumn(final String columnName, final String referencedColumnName) {
		super();
		this.name = columnName;
		this.referencedColumnName = referencedColumnName;
	}

	/**
	 * @see JoinColumn#name()
	 */
	public String getSourceEntityColumnName() {
		return name;
	}

	public void setSourceEntityColumnName(final String name) {
		this.name = name;
	}

	/**
	 * @see JoinColumn#referencedColumnName()
	 */
	public String getTargetColumnName() {
		return referencedColumnName;
	}

	/**
	 *
	 * @param referencedColumnName The name or target entity or join table column.
	 */
	public void setTargetColumnName(final String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}

}
