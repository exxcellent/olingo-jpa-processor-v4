package org.apache.olingo.jpa.test.util;

public interface Constant {
	/**
	 * The name of the persistence unit is also used as namespace.
	 */
	public static final String PUNIT_NAME = System.getProperty("persistence-unit", "org.apache.olingo.jpa");

	public static final String ENTITY_MANAGER_DATA_SOURCE = "javax.persistence.nonJtaDataSource";
}
