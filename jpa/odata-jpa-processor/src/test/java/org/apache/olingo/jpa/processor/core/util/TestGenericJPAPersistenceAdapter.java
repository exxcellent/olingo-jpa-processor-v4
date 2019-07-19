package org.apache.olingo.jpa.processor.core.util;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPA_DERBYDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPA_DefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPA_H2DatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPA_HSQLDBDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.ResourceLocalPersistenceAdapter;
import org.apache.olingo.jpa.processor.core.test.AbstractTest;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;

/**
 * Adapter using a non jta data source for testing purposes.
 *
 * @author Ralf Zozmann
 *
 */
public class TestGenericJPAPersistenceAdapter extends ResourceLocalPersistenceAdapter {

	public TestGenericJPAPersistenceAdapter(final String pUnit, final DataSourceHelper.DatabaseType dbType) {
		super(pUnit, AbstractTest.buildEntityManagerFactoryProperties(dbType), determineDatabaseProcessor(dbType));
	}

	public TestGenericJPAPersistenceAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
	        final AbstractJPADatabaseProcessor dbAccessor) {
		super(pUnit, mapEntityManagerProperties, dbAccessor);
	}

	private static AbstractJPADatabaseProcessor determineDatabaseProcessor(final DataSourceHelper.DatabaseType dbType) {
		switch (dbType) {
		case DERBY:
			return new JPA_DERBYDatabaseProcessor();
		case HSQLDB:
			return new JPA_HSQLDBDatabaseProcessor();
		case H2:
			return new JPA_H2DatabaseProcessor();
		default:
			return new JPA_DefaultDatabaseProcessor();
		}
	}

	public EntityManagerFactory getEMF() {
		return getEntityManagerFactory();
	}

}