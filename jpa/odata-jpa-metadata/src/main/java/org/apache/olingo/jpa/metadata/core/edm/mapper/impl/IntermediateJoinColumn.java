package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.JoinColumn;

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
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @see JoinColumn#referencedColumnName()
	 */
	public String getReferencedColumnName() {
		return referencedColumnName;
	}

	public void setReferencedColumnName(final String referencedColumnName) {
		this.referencedColumnName = referencedColumnName;
	}

}
