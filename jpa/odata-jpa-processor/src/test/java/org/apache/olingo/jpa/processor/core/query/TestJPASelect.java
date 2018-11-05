package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPASelect extends TestBase {

	@Test
	public void testSimpleGet() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')");
		helper.assertStatus(200);

		final ObjectNode p = helper.getValue();
		assertEquals(99, p.get("ID").asLong());
	}

	/**
	 * Test working datatype conversion between JPA and OData entity.
	 */
	@Test
	public void testDatatypeConversionEntities() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "DatatypeConversionEntities(1)");
		helper.assertStatus(200);

		final ObjectNode p = helper.getValue();
		assertEquals(1, p.get("ID").asLong());
	}

}
