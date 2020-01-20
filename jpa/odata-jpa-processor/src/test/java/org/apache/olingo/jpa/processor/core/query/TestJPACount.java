package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPACount extends TestBase {

  @Test
  public void testSimpleCount() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").count();
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(4, Integer.parseInt(helper.getRawResult()));
  }

  @Test
  public void testFilteredCount() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").expandWithOptions("Roles", false,
        true, Collections.emptyMap()).orderBy("ID asc");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);

    //    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
    //        "Persons?$expand=Roles($count=true)&$orderby=Country asc");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode persons = helper.getValues();
    assertEquals(4, persons.size());
    final ObjectNode person = (ObjectNode) persons.get(3);
    assertEquals("DEU", person.get("Country").asText());
    assertEquals(2, person.get("Roles@odata.count").asInt());
    assertTrue(person.get("Roles") == null);
  }

  @Test
  public void testExpandCount() throws IOException, ODataException {

    // skip the ID's '1' and '10'
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").skip(2).top(5)
        .expandWithOptions("Roles", false,
            true, Collections.emptyMap()).orderBy("ID");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    //    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
    //        "Organizations?$skip=2&$top=5&$orderby=ID&$expand=Roles/$count");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getValues();
    assertEquals(5, orgs.size());
    final ObjectNode org = (ObjectNode) orgs.get(1); // after skipping '1' and '10' this should be '3'
    assertEquals("3", org.get("ID").asText());
    assertEquals(3, org.get("Roles@odata.count").asInt());// "Third Org." must have 3 roles
  }

}
