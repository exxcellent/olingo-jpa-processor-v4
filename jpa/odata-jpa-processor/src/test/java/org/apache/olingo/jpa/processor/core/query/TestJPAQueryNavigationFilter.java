package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPAQueryNavigationFilter extends TestBase {

  @Test
  public void testFilterAfterNavigationForConcreteEntity() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99")
        .appendNavigationSegment("PersonReferenceWithoutMappedAttribute").filter("Roles/any(d:d/RoleCategory eq 'X')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
    assertTrue(helper.getRawResult().contains("not allowed"));
  }

  @Test
  public void testFilterNavigationPropertyToManyValueAll() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "Roles/all(d:d/RoleCategory eq 'A')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterCountNavigationProperty() throws IOException, ODataException {
    // https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301
    // Example 43: return all Categories with less than 10 products
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "Roles/$count eq 1").orderBy("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
    // only the Organizations with ID 1 and 7 have exactly one Role
    assertEquals(1, orgs.get(0).get("ID").asInt());
    assertEquals(7, orgs.get(1).get("ID").asInt());
  }

  @Test
  public void testFilterCountNavigationPropertyWithTargetHavingMultipleIds() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID", "Name1").filter(
        "Locations/$count gt 0").orderBy("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(9, orgs.size());
  }

  @Ignore("Currently no deeper navigation available ending with a collection")
  @Test
  public void testFilterCountNavigationPropertyMultipleHops() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "AdministrativeInformation/Created/User/Roles/$count ge 2");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(8, orgs.size());
  }

}
