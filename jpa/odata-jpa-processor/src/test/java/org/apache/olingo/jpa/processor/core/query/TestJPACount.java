package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPACount extends TestBase {

	@Test
	public void testSimpleCount() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons/$count");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertEquals(4, Integer.parseInt(helper.getRawResult()));
	}

	// TODO
	@Ignore("$count for expands doesn't work properly")
	@Test
	public void testFilteredCount() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons?$expand=Roles($count=true)&$orderby=Country asc");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode persons = helper.getValues();
		assertEquals(4, persons.size());
		final ObjectNode person = (ObjectNode) persons.get(2);
		// all persons have DEU or CHE, so CHE should be the last entry (as defined in
		// $orderby)
		assertEquals("DEU", person.get("Country").asText());
		assertEquals(2, person.get("Roles@odata.count").asInt());
		assertTrue(((ArrayNode) person.get("Roles")).size() == 0);// must be empty of null?
	}

	@Test
	public void testExpandCount() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				// skip the ID's '1' and '10'
				"Organizations?$skip=2&$top=5&$orderby=ID&$expand=Roles/$count");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size());
		final ObjectNode org = (ObjectNode) orgs.get(1); // after skipping '1' and '10' this should be '3'
		assertEquals("3", org.get("ID").asText());
		assertEquals(3, org.get("Roles@odata.count").asInt());// "Third Org." must have 3 roles
	}

}
