package org.apache.olingo.jpa.test.util;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

public abstract class AbstractTest implements Constant {

	public final static class EntityManagerProperty {

		private final String key;
		private final Object value;

		public EntityManagerProperty(final String key, final Object value) {
			super();
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * Helper method to create a proper configured EntityManagerFactory having a non
	 * JTA data source for {@link #PUNIT_NAME}.
	 */
	protected static EntityManagerFactory createEntityManagerFactory(final DataSourceHelper.DatabaseType dbType,
	        final EntityManagerProperty... additionalProperties) {
		final Map<String, Object> properties = buildEntityManagerFactoryProperties(dbType, additionalProperties);
		return Persistence.createEntityManagerFactory(PUNIT_NAME, properties);

	}

	/**
	 * Prepare the properties for entity manager factory with a data source and a
	 * database dialect.
	 */
	public static Map<String, Object> buildEntityManagerFactoryProperties(final DataSourceHelper.DatabaseType dbType,
	        final EntityManagerProperty... additionalProperties) {
		final Map<String, Object> properties = new HashMap<String, Object>();
		DataSource ds = null;
		switch (dbType) {
		case DERBY:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.DERBY);
			properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.DerbyPlatform");
			properties.put("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
			break;
		case HSQLDB:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.HSQLDB);
			properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.HSQLPlatform");
			properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
			break;
		case H2:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.H2);
			properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.H2Platform");
			properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			break;
		case REMOTE:
			ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.REMOTE);
			break;
		default:
			throw new UnsupportedOperationException();
		}
		if (additionalProperties != null) {
			for (final EntityManagerProperty p : additionalProperties) {
				if (ENTITY_MANAGER_DATA_SOURCE.equalsIgnoreCase(p.key)) {
					throw new IllegalArgumentException("Datasource cannot be given again");
				}
				if (p.key == null || p.key.isEmpty()) {
					throw new IllegalArgumentException("Null key not allowed");
				}
				properties.put(p.key, p.value);
			}
		}
		properties.put(ENTITY_MANAGER_DATA_SOURCE, ds);
		return properties;
	}

}
