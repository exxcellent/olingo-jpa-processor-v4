package org.apache.olingo.jpa.processor.core.util;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.ResourceLocalPersistenceAdapter;

/**
 * Adapter using a non jta data source for testing purposes.
 *
 * @author Ralf Zozmann
 *
 */
public class TestGenericJPAPersistenceAdapter extends ResourceLocalPersistenceAdapter {

	public TestGenericJPAPersistenceAdapter(final String pUnit, final JPAODataDatabaseProcessor dbAccessor,
			final DataSource ds) {
		super(pUnit, prepareEMFProperties(ds), dbAccessor);
	}

	private static Map<String, Object> prepareEMFProperties(final DataSource ds) {
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(org.apache.olingo.jpa.processor.core.test.Constant.ENTITY_MANAGER_DATA_SOURCE, ds);
		return properties;
	}

	public EntityManagerFactory getEMF() {
		return getEntityManagerFactory();
	}

}