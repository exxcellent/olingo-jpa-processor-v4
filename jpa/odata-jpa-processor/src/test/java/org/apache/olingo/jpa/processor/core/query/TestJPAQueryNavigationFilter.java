package org.apache.olingo.jpa.processor.core.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
  public void testFilterNavigationNestedLambda() throws IOException, ODataException
  {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").select("ID").filter(
        "MemberOfOrganizations/any(d:d/Roles/any(e:e/RoleCategory eq 'B'))");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode persons = helper.getJsonObjectValues();
    // only the person 97 is organisation member (and having the request role category)
    assertEquals(1, persons.size());
    assertEquals(97, persons.get(0).get("ID").asInt());
  }

  @Test
  public void testFilterLongerNavigationNestedLambda() throws IOException, ODataException
  {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "Creator/MemberOfOrganizations/any(d:d/Roles/any(e:e/RoleCategory eq 'B') and d/Address/Region eq 'US-CA')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(1, orgs.size());
  }

  @Test
  public void testFilterNavigationLongerNestedLambda() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").select("ID").filter(
        "FirstName eq 'Max' and MemberOfOrganizations/any(d:d/Roles/any(e:e/RoleCategory eq 'A'))");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode persons = helper.getJsonObjectValues();
    assertEquals(1, persons.size());
    assertEquals(99, persons.get(0).get("ID").asInt());
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

  @Disabled("Currently no deeper navigation available ending with a collection")
  @Test
  public void testFilterCountNavigationPropertyMultipleHops() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("ID").filter(
        "AdministrativeInformation/Created/User/Roles/$count ge 2");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(8, orgs.size());
  }

  @Test
  public void testNavigationUsingFilter() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartners").appendKeySegment(
        "3").appendNavigationSegment("Roles").filter("RoleCategory ne 'B'");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode roles = helper.getJsonObjectValues();
    assertEquals(2, roles.size());
  }

  @Test
  public void testNavigation2StepsUsingFilter() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment(
        "97").appendNavigationSegment("OwningPerson").appendNavigationSegment("Locations").filter("Name ne 'unknown'")
        .select("Name");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode locations = helper.getJsonObjectValues();
    assertEquals(1, locations.size());
    assertEquals("Basel-Landschaft", locations.get(0).get("Name").asText());
  }

}
