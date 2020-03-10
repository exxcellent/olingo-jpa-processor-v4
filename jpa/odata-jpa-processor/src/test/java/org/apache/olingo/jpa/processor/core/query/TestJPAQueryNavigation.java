package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAQueryNavigation extends TestBase {

  @Test
  public void testNavigationOneHop() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Roles");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(3, orgs.size());
  }

  @Test
  public void testNavigationOneHopNormal() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendKeySegment("99")
        .appendNavigationSegment("Image1");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode img = helper.getJsonObjectValue();
    assertNotNull(img);
    assertEquals(99, img.get("PID").asLong());
  }

  @Test
  public void testNavigationOneToOneWithoutMappedAttribute() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99")
        .appendNavigationSegment("PersonReferenceWithoutMappedAttribute");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode person = helper.getJsonObjectValue();
    assertNotNull(person);
    assertEquals(2, ((ArrayNode) person.get("PhoneNumbers")).size());
  }

  @Test
  public void testNavigationOneHopWithoutReferencedColumn() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendKeySegment("99")
        .appendNavigationSegment("Image2");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode img = helper.getJsonObjectValue();
    assertNotNull(img);
    assertEquals(97, img.get("PID").asLong());
  }

  @Test
  public void testNavigationTwoHopUsingDefaultIdMapping() throws IOException, ODataException {
    assumeTrue("Hibernate does not build a proper columns selection without quoting of column name",
        getJPAProvider() != JPAProvider.Hibernate);

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendKeySegment("98")
        .appendNavigationSegment("Image2").appendNavigationSegment("PersonWithDefaultIdMapping");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode img = helper.getJsonObjectValue();
    assertEquals(97, img.get("ID").asLong());
  }

  @Test
  public void testNoNavigationOneEntity() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("Third Org.", org.get("Name1").asText());
  }

  @Test
  public void testNavigationOneHopAndOrderBy() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Roles").orderBy("RoleCategory desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(3, orgs.size());
    assertEquals("C", orgs.get(0).get("RoleCategory").asText());
    assertEquals("A", orgs.get(2).get("RoleCategory").asText());
  }

  @Test
  public void testNavigationOneHopReverse() throws IOException, ODataException {
    final Map<String, Object> keys = new HashMap<String, Object>();
    keys.put("BusinessPartnerID", "2");
    keys.put("RoleCategory", "A");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartnerRoles").appendKeySegment(
        keys).appendNavigationSegment("BusinessPartner");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("2", org.get("ID").asText());
  }

  @Test
  public void testNavigationViaComplexType() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("AdministrativeInformation").appendNavigationSegment(
            "Created").appendNavigationSegment("By");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    //    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
    //        "Organizations('3')/AdministrativeInformation/Created/By");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("99", org.get("value").asText());
  }

  @Ignore("Requested navigation path currenlty not exisitng in model")
  @Test
  public void testNavigationViaComplexTypeTwoHops() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "Organizations('3')/AdministrativeInformation/Created/User/Address/AdministrativeDivision");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("3166-1", org.get("ParentCodeID").asText());
  }

  @Test
  public void testNavigationSelfToOneOneHops() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE352',CodeID='NUTS3',CodePublisher='Eurostat')/Parent");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("NUTS2", org.get("CodeID").asText());
    assertEquals("BE35", org.get("DivisionCode").asText());
  }

  @Test
  public void testNavigationSelfToOneTwoHops() throws IOException, ODataException {
    final Map<String, Object> keysOrganization = new HashMap<String, Object>();
    keysOrganization.put("DivisionCode", "BE352");
    keysOrganization.put("CodeID", "NUTS3");
    keysOrganization.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        keysOrganization).appendNavigationSegment("Parent").appendNavigationSegment("Parent");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("NUTS1", org.get("CodeID").asText());
    assertEquals("BE3", org.get("DivisionCode").asText());
  }

  @Test
  public void testNavigationSelfToManyOneHops() throws IOException, ODataException {
    final Map<String, Object> keysOrganization = new HashMap<String, Object>();
    keysOrganization.put("DivisionCode", "BE2");
    keysOrganization.put("CodeID", "NUTS1");
    keysOrganization.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        keysOrganization).appendNavigationSegment("Children").orderBy("DivisionCode desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(5, orgs.size());
    assertEquals("NUTS2", orgs.get(0).get("CodeID").asText());
    assertEquals("BE25", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testNavigationSelfToManyTwoHopsOrdered() throws IOException, ODataException {
    final Map<String, Object> keysOrganization = new HashMap<String, Object>();
    keysOrganization.put("DivisionCode", "BE2");
    keysOrganization.put("CodeID", "NUTS1");
    keysOrganization.put("CodePublisher", "Eurostat");
    final Map<String, Object> keysAD = new HashMap<String, Object>();
    keysAD.put("DivisionCode", "BE25");
    keysAD.put("CodeID", "NUTS2");
    keysAD.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        keysOrganization).appendNavigationSegment("Children").appendKeySegment(keysAD).appendNavigationSegment(
            "Children").orderBy("DivisionCode desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(8, orgs.size());
    assertEquals("NUTS3", orgs.get(0).get("CodeID").asText());
    assertEquals("BE258", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testNavigationSelfToOneTwoHopsUsingKeys() throws IOException, ODataException {
    final Map<String, Object> keysOrganization = new HashMap<String, Object>();
    keysOrganization.put("DivisionCode", "BE2");
    keysOrganization.put("CodeID", "NUTS1");
    keysOrganization.put("CodePublisher", "Eurostat");

    final Map<String, Object> keysAD1 = new HashMap<String, Object>();
    keysAD1.put("DivisionCode", "BE25");
    keysAD1.put("CodeID", "NUTS2");
    keysAD1.put("CodePublisher", "Eurostat");

    final Map<String, Object> keysAD2 = new HashMap<String, Object>();
    keysAD2.put("DivisionCode", "BE258");
    keysAD2.put("CodeID", "NUTS3");
    keysAD2.put("CodePublisher", "Eurostat");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        keysOrganization).appendNavigationSegment("Children").appendKeySegment(keysAD1).appendNavigationSegment(
            "Children").appendKeySegment(keysAD2);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode ad = helper.getJsonObjectValue();
    assertNotNull(ad);
    assertEquals("NUTS3", ad.get("CodeID").asText());
    assertEquals("BE258", ad.get("DivisionCode").asText());
  }

  @Test
  public void testNavigationSelfToOneThreeHopsNoResult() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Address").appendNavigationSegment("AdministrativeDivision").appendNavigationSegment(
            "Parent").appendNavigationSegment("Parent");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testNavigationSelfToManyOneHopsWithResult() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Address").appendNavigationSegment("AdministrativeDivision").appendNavigationSegment(
            "Parent");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode ad = helper.getJsonObjectValue();
    assertNotNull(ad);
    assertEquals("3166-1", ad.get("CodeID").asText());
  }

  @Test
  public void testNavigationSelfToManyOneHopsNoResult() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Address").appendNavigationSegment("AdministrativeDivision").appendNavigationSegment(
            "Children");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ads = helper.getJsonObjectValues();
    assertNotNull(ads);
    assertEquals(0, ads.size());
  }

  @Test
  public void testNavigationSelfToManyTwoHops() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Address").appendNavigationSegment("AdministrativeDivision");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode ad = helper.getJsonObjectValue();
    assertNotNull(ad);
    assertEquals("3166-2", ad.get("CodeID").asText());
  }

  @Test
  public void testNavigationSelfToEmbedded() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Address");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode ad = helper.getJsonObjectValue();
    assertNotNull(ad);
    assertEquals("223", ad.get("HouseNumber").asText());
  }

  @Test
  public void testNavigationThroughJoinTable() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("2")
        .appendNavigationSegment("Locations");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ads = helper.getJsonObjectValues();
    assertNotNull(ads);
    assertEquals(2, ads.size());
  }
}
