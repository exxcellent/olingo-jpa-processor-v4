package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

public interface JPASimpleAttribute extends JPAAttribute {
	/**
	 *
	 * @return The column name in data base.
	 */
	public String getDBFieldName();
}
