package org.apache.olingo.jpa.processor.core.test;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;

public abstract class AbstractTest implements Constant {
	public static enum TestDatabaseType {
		HSQLDB, DERBY;

	}

	/**
	 * Helper method to create a proper configured EntityManagerFactory having a non
	 * JTA data source for {@link #PUNIT_NAME}.
	 */
	protected static EntityManagerFactory createEntityManagerFactory(final TestDatabaseType dbType) {
		DataSource ds = null;
		switch (dbType) {
		case DERBY:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_DERBY);
			break;
		case HSQLDB:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(ENTITY_MANAGER_DATA_SOURCE, ds);
		return Persistence.createEntityManagerFactory(PUNIT_NAME, properties);

	}

}
