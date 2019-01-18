package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPASelect extends TestBase {

	@Test
	public void testSimpleGet() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ObjectNode p = helper.getValue();
		assertEquals(99, p.get("ID").asLong());
	}

	/**
	 * Test working datatype conversion between JPA and OData entity.
	 */
	@Test
	public void testDatatypeConversionEntities() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "DatatypeConversionEntities(1)");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ObjectNode p = helper.getValue();
		assertEquals(1, p.get("ID").asLong());
	}

	@Test
	public void testSelectEmbeddedId() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$select=CodePublisher,DivisionCode&$filter=CodeID eq 'NUTS3'");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode orgs = helper.getValues();
		assertEquals(88, orgs.size());
		// Not selected non key attributes must not be set
		assertNull(orgs.get(0).get("Name"));

	}

}
