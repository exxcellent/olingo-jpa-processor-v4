package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPACount extends TestBase {

	@Test
	public void testSimpleCount() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons/$count");
		helper.assertStatus(200);
		assertEquals(3, Integer.parseInt(helper.getRawResult()));
	}

	@Test
	public void testExpandCount() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				// skip the ID's '1' and '10'
				"Organizations?$skip=2&$top=5&$orderby=ID&$expand=Roles/$count");
		helper.assertStatus(200);
		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size());
		final ObjectNode org = (ObjectNode) orgs.get(1); // after skipping '1' and '10' this should be '3'
		assertEquals("3", org.get("ID").asText());
		assertEquals(3, org.get("Roles@odata.count").asInt());// "Third Org." must have 3 roles
	}

}
