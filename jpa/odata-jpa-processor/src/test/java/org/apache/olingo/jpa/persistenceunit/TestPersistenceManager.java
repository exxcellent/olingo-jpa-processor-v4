package org.apache.olingo.jpa.persistenceunit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Set;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData;
import org.apache.olingo.jpa.processor.core.util.TestBase;
//import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Test;

public class TestPersistenceManager extends TestBase {

	@Test
	public void testPersistenceUnitSelectionAndMetamodel() throws IOException, ODataException {
		// skip test with Hibernate
		assumeTrue("Hibernate has a bug switching to another PU -> the metamodel is not reloaded",
				getJPAProvider() != JPAProvider.Hibernate);

		final Set<EntityType<?>> entitiesDefault = persistenceAdapter.getMetamodel().getEntities();
		assertTrue(entitiesDefault.size() > 1);
		persistenceAdapter.dispose();

		// check the correct handling of a second persistence unit in persistence.xml
		final TestGenericJPAPersistenceAdapter specialPersistenceAdapter = new TestGenericJPAPersistenceAdapter("DUMMY",
				DataSourceHelper.DatabaseType.HSQLDB);
		final Set<EntityType<?>> entitiesSpecial = specialPersistenceAdapter.getMetamodel().getEntities();
		// Hibernate returns the number of the first loaded PU
		assertEquals(1, entitiesSpecial.size());
	}

	@Test
	public void testCardinality() throws IOException, ODataException {
		// skip test with openJPA
		assumeTrue("openJPA is using a 1:1 relationship instead of m:1", getJPAProvider() != JPAProvider.OpenJPA);

		final EmbeddableType<PostalAddressData> paET = persistenceAdapter.getMetamodel()
				.embeddable(PostalAddressData.class);
		// openJPA 3.0.0 will fail here
		assertEquals(PersistentAttributeType.MANY_TO_ONE,
				paET.getAttribute("administrativeDivision").getPersistentAttributeType());
	}

	/**
	 * Check while test execution that the correct PU is used for the JPA provider
	 * in the actual maven profile
	 */
	@Test
	public void testOpenJPASpecificPersistenceUnit() throws IOException, ODataException {
		// openJPA has own persistence unit declaration, because some settings are
		// incompatible to other JPA providers
		assumeTrue("openJPA is using a 1:1 relationship instead of m:1", getJPAProvider() == JPAProvider.OpenJPA);

		assertEquals("openjpa", Constant.PUNIT_NAME);
		assertEquals(Constant.PUNIT_NAME, persistenceAdapter.getNamespace());
	}
}
