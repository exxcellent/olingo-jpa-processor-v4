package org.apache.olingo.jpa.persistenceunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.database.JPA_HSQLDB_DatabaseProcessor;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.junit.Test;

public class TestPersistenceManager extends TestBase {

	@Test
	public void testCorrectUnit() throws IOException, ODataException {
		final Set<EntityType<?>> entitiesDefault = persistenceAdapter.getMetamodel().getEntities();
		assertTrue(entitiesDefault.size() > 1);

		// check the correct handling of a second persistence unit in persistence.xml
		final TestGenericJPAPersistenceAdapter specialPersistenceAdapter = new TestGenericJPAPersistenceAdapter("DUMMY",
				new JPA_HSQLDB_DatabaseProcessor(), DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
		final Set<EntityType<?>> entitiesSpecial = specialPersistenceAdapter.getMetamodel().getEntities();
		assertEquals(1, entitiesSpecial.size());
	}

}
