package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAProcessorExpand extends TestBase {

  @Test
  public void testExpandEntitySet() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").orderBy("ID").expand("Roles");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    ObjectNode org = (ObjectNode) orgs.get(0);
    ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(1, roles.size());

    org = (ObjectNode) orgs.get(3);
    roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());
  }

  @Test
  public void testExpandOneEntity() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("2").expand(
        "Roles");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(2, roles.size());
    int found = 0;
    for (final JsonNode role : roles) {
      final String id = role.get("BusinessPartnerID").asText();
      final String code = role.get("RoleCategory").asText();
      if (id.equals("2") && (code.equals("A") || code.equals("C"))) {
        found++;
      }
    }
    assertEquals("Not all expected results found", 2, found);
  }

  @Test
  public void testExpandOneEntityCompoundKey() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Parent");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode divsion = helper.getJsonObjectValue();
    final ObjectNode parent = (ObjectNode) divsion.get("Parent");
    assertEquals("BE2", parent.get("DivisionCode").asText());

  }

  @Test
  public void testExpandOneEntityCompoundKeyCollection() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode asc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode divsion = helper.getJsonObjectValue();
    final ArrayNode parent = (ArrayNode) divsion.get("Children");
    assertEquals(8, parent.size());
    assertEquals("BE251", parent.get(0).get("DivisionCode").asText());

  }

  @Test
  public void testExpandEntitySetWithOutParentKeySelection() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "Organizations?$orderby=Name1&$select=Name1&$expand=Roles");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    final ObjectNode org = (ObjectNode) orgs.get(9);
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());

  }

  @Ignore // Not supported by Olingo as of now
  @Test
  public void testExpandEntitySetViaNonKeyField_FieldNotSelected() throws IOException, ODataException {

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "Organizations('3')/AdministrativeInformation/Created?$select=At&$expand=User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode created = helper.getJsonObjectValue();
    // ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));
  }

  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi2Hops() throws IOException, ODataException {

    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE253");
    mapKeys.put("CodeID", "NUTS3");
    mapKeys.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys)
        .appendNavigationSegment("Parent").appendNavigationSegment("Parent").expand("Children");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode ad = helper.getJsonObjectValue();
    assertEquals("NUTS1", ad.get("CodeID").asText());// top level!
    assertEquals("BE2", ad.get("DivisionCode").asText());// top level!
    assertEquals(5, ad.withArray("Children").size());// NUTS1/BE2/BEL has 5 children
  }

  @Test
  public void testExpandEntityViaComplexProperty() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment(
        "3").appendNavigationSegment("Address").expand("AdministrativeDivision");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode address = helper.getJsonObjectValue();
    assertEquals("223", address.get("HouseNumber").asText());
    final ObjectNode ad = (ObjectNode) address.get("AdministrativeDivision");
    assertEquals("USA", ad.get("ParentDivisionCode").asText());
  }

  @Test
  public void testNestedExpandNestedExpand2LevelsSelf() throws IOException, ODataException {
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE253");
    mapKeys.put("CodeID", "NUTS3");
    mapKeys.put("CodePublisher", "Eurostat");
    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.EXPAND, "Children");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expandWithOptions("Parent", expandOptionsTargets);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Children"));
    final ArrayNode children = (ArrayNode) parent.get("Children");
    assertEquals(8, children.size());
    assertEquals("NUTS3", children.get(0).get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand3LevelsSelf() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='33016',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($expand=Parent($expand=Parent))");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Parent"));
    assertNotNull(parent.get("Parent").get("CodeID"));
    assertEquals("NUTS3", parent.get("CodeID").asText());
    final ObjectNode grandParent = (ObjectNode) parent.get("Parent");
    assertNotNull(grandParent);
    assertNotNull(grandParent.get("CodeID"));
    assertEquals("NUTS2", grandParent.get("CodeID").asText());
    final ObjectNode greateGrandParent = (ObjectNode) grandParent.get("Parent");
    assertNotNull(greateGrandParent);
    assertNotNull(greateGrandParent.get("CodeID"));
    assertEquals("NUTS1", greateGrandParent.get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand2LevelsMixed() throws IOException, ODataException {
    // see example:
    // https://services.odata.org/V4/Northwind/Northwind.svc/Customers('ALFKI')/Orders?$select=ShipCity&$expand=Order_Details
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "Organizations('3')/Address?$select=Country&$expand=AdministrativeDivision($expand=Parent)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    assertEquals("USA", div.get("Country").asText());
    assertNull(div.get("CityName"));
    final ObjectNode admin = (ObjectNode) div.get("AdministrativeDivision");
    assertNotNull(admin);
    final ObjectNode parent = (ObjectNode) admin.get("Parent");
    assertEquals("3166-1", parent.get("CodeID").asText());
  }

  @Test
  public void testExpandWithNavigationToProperty() throws IOException, ODataException {
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE253");
    mapKeys.put("CodeID", "NUTS3");
    mapKeys.put("CodePublisher", "Eurostat");
    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.SELECT, "CodeID");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expandWithOptions("Parent", expandOptionsTargets);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    assertNotNull(div.get("Area"));
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("CodeID"));
    assertEquals("NUTS2", parent.get("CodeID").asText());
    // only the keys must be selected (see $select)
    assertNull(parent.get("Area"));
  }

  @Test
  public void testExpandWithOrderByDesc() throws IOException, ODataException {
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE2");
    mapKeys.put("CodeID", "NUTS1");
    mapKeys.put("CodePublisher", "Eurostat");
    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.ORDERBY, "DivisionCode desc");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expandWithOptions("Children", expandOptionsTargets);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByAsc() throws IOException, ODataException {
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE2");
    mapKeys.put("CodeID", "NUTS1");
    mapKeys.put("CodePublisher", "Eurostat");
    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.ORDERBY, "DivisionCode asc");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expandWithOptions("Children", expandOptionsTargets);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE21", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTop() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$orderby=DivisionCode desc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTopSkip() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$skip=2;$orderby=DivisionCode desc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getJsonObjectValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE23", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithCount() throws IOException, ODataException {

    final Map<QueryOption, Object> optionsRolesExpand = new HashMap<>();
    optionsRolesExpand.put(QueryOption.COUNT, Boolean.TRUE);
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").count(true).expandWithOptions(
        "Roles", optionsRolesExpand).orderBy("Roles/$count desc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(10, orgs.size());
    final ObjectNode orgWithMostRoles = (ObjectNode) orgs.get(0);
    assertEquals("3", orgWithMostRoles.get("ID").asText());
    assertEquals(3, orgWithMostRoles.withArray("Roles").size());// 3 roles
    assertEquals(3, orgWithMostRoles.get("Roles@odata.count").asInt());// must be present
  }

  @Ignore("TODO")
  @Test
  public void testExpandWithOrderByDescTopSkipAndExternalOrderBy() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "Organizations?$count=true&$expand=Roles($orderby=RoleCategory desc)&$orderby=Roles/$count desc");

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertEquals("3", org.get("ID").asText());
    assertNotNull(org.get("Roles"));
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());
    final ObjectNode firstRole = (ObjectNode) roles.get(0);
    assertEquals("C", firstRole.get("RoleCategory").asText());
  }

  @Test
  public void testExpandWithFilter() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($filter=DivisionCode eq 'BE252')");

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode division = helper.getJsonObjectValue();
    assertEquals("BE25", division.get("DivisionCode").asText());
    assertNotNull(division.get("Children"));
    final ArrayNode children = (ArrayNode) division.get("Children");
    assertEquals(1, children.size());
    final ObjectNode firstChild = (ObjectNode) children.get(0);
    assertEquals("BE252", firstChild.get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithFilteredNavigation() throws IOException, ODataException {

    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.FILTER, "contains(Name, 'rel')");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .expandWithOptions("targets", expandOptionsTargets).top(3);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getJsonObjectValues();
    assertTrue(relSources.size() == 2);
    assertTrue(((ArrayNode) relSources.get(0).get("targets")).size() > 0);
  }

  @Test
  public void testExpandWithNavigationFilter() throws IOException, ODataException {

    // skip test with EclipseLink
    assumeTrue("EclipseLink will produce an invalid query", getJPAProvider() != JPAProvider.EclipseLink);

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").filter(
        "targets/any(d:contains(d/Name, 'rel'))")
        .expand("targets").top(3);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getJsonObjectValues();
    assertTrue(relSources.size() == 2);
    assertTrue(((ArrayNode) relSources.get(0).get("targets")).size() > 0);
  }

  @Test
  public void testExpandCompleteEntitySet() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations")
        .expand("Roles").orderBy("ID");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    // check json reponse
    final ArrayNode orgs = helper.getJsonObjectValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertEquals("1", org.get("ID").asText());
    assertNotNull(org.get("Roles"));
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(1, roles.size());
    final ObjectNode firstRole = (ObjectNode) roles.get(0);
    assertEquals("A", firstRole.get("RoleCategory").asText());

    // Olingo client response
    final ClientEntitySet set = helper.getOlingoEntityCollectionValues();
    assertNotNull(set);
  }

  @Test
  public void testExpandTwoNavigationPath() throws IOException, ODataException {

    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE32");
    mapKeys.put("CodeID", "NUTS2");
    mapKeys.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expand("Parent", "Children");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Test
  public void testExpandAllNavigationPathOfPath() throws IOException, ODataException {
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE32");
    mapKeys.put("CodeID", "NUTS2");
    mapKeys.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expand("*");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    assertEquals(org.get("ParentDivisionCode"), parent.get("DivisionCode"));
    assertEquals(org.get("CodePublisher"), parent.get("CodePublisher"));
    assertEquals(org.get("ParentCodeID"), parent.get("CodeID"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Ignore
  @Test
  public void testExpandLevel1() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='38025',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($levels=1)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Ignore
  @Test
  public void testExpandLevelMax() throws IOException, ODataException {
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "/AdministrativeDivisions(DivisionCode='BE241',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent($levels=max)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Test
  public void testExpandAllNavigationPathWithComplex() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3").expand(
        "*");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertNotNull(org.get("Roles"));
    assertNotNull(org.get("PhoneNumbers"));
    assertNotNull(org.get("Locations"));

  }

  @Test
  public void testExpandCompleteEntitySet2() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").expand("Parent");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testExpandNotMappedPersonFromPersonImage() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99").expand(
        "PersonReferenceWithoutMappedAttribute");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode pi = helper.getJsonObjectValue();
    assertNotNull(pi.get("PersonReferenceWithoutMappedAttribute"));
    assertNotEquals(NullNode.getInstance(), pi.get("PersonReferenceWithoutMappedAttribute"));
  }

  @Test
  public void testExpandAllLoadingNotMappedPersonFromPersonImage() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").expand(
        "PersonReferenceWithoutMappedAttribute").orderBy("PID asc");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode pis = helper.getJsonObjectValues();
    assertEquals(2, pis.size());
    final ObjectNode pi1 = (ObjectNode) pis.get(0);
    final ObjectNode pi2 = (ObjectNode) pis.get(1);
    assertNotEquals(pi1.get("PID").asText(), pi2.get("PID").asText());
    assertNotEquals(pi1.get("PersonReferenceWithoutMappedAttribute").get("ID").asText(),
        pi2.get("PersonReferenceWithoutMappedAttribute").get("ID").asText());
  }

  @Test
  public void testExpandHavingElementCollections() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99").expand(
        "PersonReferenceWithoutMappedAttribute");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode personImage = helper.getJsonObjectValue();
    assertEquals(99, personImage.get("PID").asLong());
    final ObjectNode person = (ObjectNode) personImage.get("PersonReferenceWithoutMappedAttribute");
    assertNotNull(person);
    assertEquals(98, person.get("ID").asLong());
    assertEquals(2, ((ArrayNode) person.get("PhoneNumbers")).size());
    assertNotNull(((ArrayNode) person.get("PhoneNumbers")).get(0).get("phoneNumber"));
    assertEquals(((ArrayNode) person.get("PhoneNumbersAsString")).size(),
        ((ArrayNode) person.get("PhoneNumbers")).size());
  }

  @Test
  public void testExpandEntitiesWithGeneratedValueAttribute() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .expand("targets");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getJsonObjectValues();
    assertTrue(entities.size() > 0);
  }

  @Test
  public void testExpandBidirectionalSingleEntityWithGeneratedValueAttribute() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .appendKeySegment(Integer.valueOf(1))
        .select("ID")
        .expand("targets");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode entity = helper.getJsonObjectValue();
    assertNotNull(entity);
    final ArrayNode targets = (ArrayNode) entity.get("targets");
    assertTrue(targets.size() > 0);
    assertTrue(targets.get(0).get("ID").asInt() > 0);
  }

  @Test
  public void testExpandUnidirectionalSingleEntityWithGeneratedValueAttribute() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .appendKeySegment(Integer.valueOf(1))
        .expand("unidirectionalTargets");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode entity = helper.getJsonObjectValue();
    assertNotNull(entity);
    final ArrayNode targets = (ArrayNode) entity.get("unidirectionalTargets");
    assertNotNull(targets);
    assertTrue(targets.size() > 0);
    assertTrue(targets.get(0).get("ID").asInt() > 0);
  }

  @Test
  public void testExpandUnidirectionalEntitySet() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").orderBy(
        "ID desc")
        .expand("unidirectionalTargets");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getJsonObjectValues();
    assertNotNull(entities);
    assertEquals(2, entities.size());
    // 'source 2' has 2 entries
    assertTrue(entities.get(0).get("ID").asInt() == 4);
    assertTrue(((ArrayNode) entities.get(0).get("unidirectionalTargets")).size() == 1);
    // 'source 1' has 2 entries
    assertTrue(entities.get(1).get("ID").asInt() == 1);
    assertTrue(((ArrayNode) entities.get(1).get("unidirectionalTargets")).size() == 2);
  }

  /**
   * Test whether the response is conform to Olingo client side parser
   */
  @Test
  public void testExpandEntityForOneToOneUnidirectional() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("9")
        .expand("ImageUnidirectional");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ClientEntity entity = helper.getOlingoEntityValue();
    assertNotNull(entity);
  }

  /**
   * Test whether the response is conform to Olingo client side parser
   */
  @Test
  public void testExpandEntitySetForOneToOneUnidirectional1() throws IOException, ODataException {

    // filter for the only entity having an image
    final Map<QueryOption, Object> expandOptionsTargets = new HashMap<>();
    expandOptionsTargets.put(QueryOption.FILTER, "ThumbnailUrl ne 'http://nowhere.com'");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").filter(
        "Name1 eq 'Ninth Org.'")
        .expandWithOptions(
            "ImageUnidirectional", expandOptionsTargets).expandWithOptions("Address/AdministrativeDivision", Collections
                .emptyMap());
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ClientEntitySet set = helper.getOlingoEntityCollectionValues();
    assertNotNull(set);
    // only Organization('9') should match
    assertTrue(set.getEntities().size() == 1);
    final ClientEntity org = set.getEntities().get(0);
    assertEquals(org.getProperty("ID").getPrimitiveValue().toCastValue(String.class), "9");
    final ClientComplexValue address = set.getEntities().get(0).getProperty("Address").getComplexValue();
    assertEquals(address.get("HouseNumber").getPrimitiveValue().toCastValue(String.class), "93");
    final ClientEntity ad = address.getNavigationLink("AdministrativeDivision").asInlineEntity().getEntity();
    assertNotNull(ad);
    assertEquals("ISO", ad.getProperty("CodePublisher").getPrimitiveValue().toCastValue(String.class));
    assertEquals("3166-2", ad.getProperty("CodeID").getPrimitiveValue().toCastValue(String.class));
    assertEquals("US-MN", ad.getProperty("DivisionCode").getPrimitiveValue().toCastValue(String.class));
  }

  @Test
  public void testExpandEntitySetForOneToOneUnidirectional2() throws IOException, ODataException {

    // skip first 2 orgs with ID '1' and '10'
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").expand("ImageUnidirectional")
        .orderBy("ID asc").skip(2);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ClientEntitySet set = helper.getOlingoEntityCollectionValues();
    assertNotNull(set);
    assertEquals("The number of existing/expected organizations", 8, set.getEntities().size());
    assertEquals("9", set.getEntities().get(7).getProperty("ID").getPrimitiveValue().toCastValue(String.class));
    // Organization('9') must have OrganizationImage
    assertNotNull(set.getEntities().get(7).getNavigationLink("ImageUnidirectional"));
    assertEquals("9", set.getEntities().get(7).getNavigationLink("ImageUnidirectional").asInlineEntity().getEntity()
        .getProperty("ID").getPrimitiveValue().toCastValue(String.class));
    // all other must not have an OrganizationImage
    assertNull(set.getEntities().get(3).getNavigationLink("ImageUnidirectional"));
  }
}
