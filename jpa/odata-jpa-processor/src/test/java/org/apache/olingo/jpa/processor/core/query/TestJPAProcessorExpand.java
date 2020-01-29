package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
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

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Parent");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode divsion = helper.getValue();
    final ObjectNode parent = (ObjectNode) divsion.get("Parent");
    assertEquals("BE2", parent.get("DivisionCode").asText());

  }

  @Test
  public void testExpandOneEntityCompoundKeyCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode asc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode divsion = helper.getValue();
    final ArrayNode parent = (ArrayNode) divsion.get("Children");
    assertEquals(8, parent.size());
    assertEquals("BE251", parent.get(0).get("DivisionCode").asText());

  }

  @Test
  public void testExpandEntitySetWithOutParentKeySelection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations?$orderby=Name1&$select=Name1&$expand=Roles");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(9);
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(3, roles.size());

  }

  @Ignore // Not supported by Olingo as of now
  @Test
  public void testExpandEntitySetViaNonKeyField_FieldNotSelected() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')/AdministrativeInformation/Created?$select=At&$expand=User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode created = helper.getValue();
    // ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));
  }

  @Ignore // Not supported by Olingo as of now
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi2Hops() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')/AdministrativeInformation/Created?$expand=User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
    final ObjectNode created = (ObjectNode) org.get("Created");
    @SuppressWarnings("unused")
    final ObjectNode user = (ObjectNode) created.get("User");
  }

  @Ignore("AdministrativeDivision not available at Address")
  @Test
  public void testExpandEntityViaComplexProperty() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')/Address?$expand=AdministrativeDivision");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
    final ObjectNode created = (ObjectNode) org.get("AdministrativeDivision");
    assertEquals("USA", created.get("ParentDivisionCode").asText());
  }

  @Ignore // TODO Check if metadata are generated correct
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi0Hops() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')?$expand=AdministrativeInformation/Created/User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
    final ObjectNode admin = (ObjectNode) org.get("AdministrativeInformation");
    final ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));

  }

  @Ignore // Not supported by Olingo now; Not supported ExpandSelectHelper.getExpandedPropertyNames
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi1Hop() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')/AdministrativeInformation?$expand=Created/User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode admin = helper.getValue();
    final ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));
  }

  @Ignore // TODO Check if metadata are generated correct
  @Test
  public void testExpandEntitySetViaNonKeyFieldNavi0HopsCollection() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations?$expand=AdministrativeInformation/Created/User");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    final ObjectNode admin = (ObjectNode) org.get("AdministrativeInformation");
    final ObjectNode created = (ObjectNode) admin.get("Created");
    assertNotNull(created.get("User"));

  }

  @Test
  public void testNestedExpandNestedExpand2LevelsSelf() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent($expand=Children)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Children"));
    final ArrayNode children = (ArrayNode) parent.get("Children");
    assertEquals(8, children.size());
    assertEquals("NUTS3", children.get(0).get("CodeID").asText());
  }

  @Test
  public void testNestedExpandNestedExpand3LevelsSelf() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='33016',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($expand=Parent($expand=Parent))");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations('3')/Address?$select=Country&$expand=AdministrativeDivision($expand=Parent)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ObjectNode admin = (ObjectNode) div.get("AdministrativeDivision");
    assertNotNull(admin);
    final ObjectNode parent = (ObjectNode) admin.get("Parent");
    assertEquals("3166-1", parent.get("CodeID").asText());
  }

  @Ignore // TODO check how the result should look like
  @Test
  public void testExpandWithNavigationToEntity() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='3',CodePublisher='NUTS')?$expand=Parent/Parent");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("Parent").get("CodeID"));
    assertEquals("1", parent.get("Parent").get("CodeID").asText());
  }

  @Ignore // TODO check with Olingo looks like OData does not support this
  @Test
  public void testExpandWithNavigationToProperty() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE253',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent/CodeID");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ObjectNode parent = (ObjectNode) div.get("Parent");
    assertNotNull(parent.get("CodeID"));
    assertEquals("NUTS2", parent.get("CodeID").asText());
    // TODO: Check how to create the response correctly
    // assertEquals(1, parent.size());
  }

  @Test
  public void testExpandWithOrderByDesc() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode desc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByAsc() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($orderby=DivisionCode asc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(5, children.size());
    assertEquals("BE21", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTop() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$orderby=DivisionCode desc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE25", children.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testExpandWithOrderByDescTopSkip() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')?$expand=Children($top=2;$skip=2;$orderby=DivisionCode desc)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode div = helper.getValue();
    final ArrayNode children = (ArrayNode) div.get("Children");
    assertEquals(2, children.size());
    assertEquals("BE23", children.get(0).get("DivisionCode").asText());
  }

  // TODO check how to handle $count -> Olingo auch mit $top=1;
  @Ignore
  @Test
  public void testExpandWithCount() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations?$count=true&$expand=Roles($count=true)&$orderby=Roles/$count desc");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertNotNull(org.get("Roles"));
    @SuppressWarnings("unused")
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    // assertEquals("3", child1.get("count").asText());
  }

  @Ignore("TODO")
  @Test
  public void testExpandWithOrderByDescTopSkipAndExternalOrderBy() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Organizations?$count=true&$expand=Roles($orderby=RoleCategory desc)&$orderby=Roles/$count desc");

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')?$expand=Children($filter=DivisionCode eq 'BE252')");

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode division = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getValues();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode relSources = helper.getValues();
    assertTrue(relSources.size() == 2);
    assertTrue(((ArrayNode) relSources.get(0).get("targets")).size() > 0);
  }

  @Test
  public void testExpandCompleteEntitySet() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations")
        .expand("Roles").orderBy("ID");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getValues();
    final ObjectNode org = (ObjectNode) orgs.get(0);
    assertEquals("1", org.get("ID").asText());
    assertNotNull(org.get("Roles"));
    final ArrayNode roles = (ArrayNode) org.get("Roles");
    assertEquals(1, roles.size());
    final ObjectNode firstRole = (ObjectNode) roles.get(0);
    assertEquals("A", firstRole.get("RoleCategory").asText());
  }

  @Test
  public void testExpandTwoNavigationPath() throws IOException, ODataException {

    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE32");
    mapKeys.put("CodeID", "NUTS2");
    mapKeys.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(
        mapKeys).expand("Parent", "Children");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisions(DivisionCode='38025',CodeID='LAU2',CodePublisher='Eurostat')?$expand=Parent($levels=1)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Parent"));
    final ObjectNode parent = (ObjectNode) org.get("Parent");
    assertNotNull(parent.get("DivisionCode"));
    final ArrayNode children = (ArrayNode) org.get("Children");
    assertEquals(7, children.size());
  }

  @Ignore
  @Test
  public void testExpandLevelMax() throws IOException, ODataException {
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "/AdministrativeDivisions(DivisionCode='BE241',CodeID='NUTS3',CodePublisher='Eurostat')?$expand=Parent($levels=max)");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getValue();
    assertNotNull(org.get("Roles"));
    assertNotNull(org.get("PhoneNumbers"));
    assertNotNull(org.get("Locations"));

  }

  @Test
  public void testExpandCompleteEntitySet2() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").expand("Parent");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testExpandNotMappedPersonFromPersonImage() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99").expand(
        "PersonReferenceWithoutMappedAttribute");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode pi = helper.getValue();
    assertNotNull(pi.get("PersonReferenceWithoutMappedAttribute"));
    assertNotEquals(NullNode.getInstance(), pi.get("PersonReferenceWithoutMappedAttribute"));
  }

  @Test
  public void testExpandAllLoadingNotMappedPersonFromPersonImage() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").expand(
        "PersonReferenceWithoutMappedAttribute").orderBy("PID asc");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode pis = helper.getValues();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode personImage = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getValues();
    assertTrue(entities.size() > 0);
  }

  @Test
  public void testExpandBidirectionalSingleEntityWithGeneratedValueAttribute() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .appendKeySegment(Integer.valueOf(1))
        .select("ID")
        .expand("targets");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode entity = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode entity = helper.getValue();
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
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode entities = helper.getValues();
    assertNotNull(entities);
    assertEquals(2, entities.size());
    // 'source 2' has 2 entries
    assertTrue(entities.get(0).get("ID").asInt() == 4);
    assertTrue(((ArrayNode) entities.get(0).get("unidirectionalTargets")).size() == 1);
    // 'source 1' has 2 entries
    assertTrue(entities.get(1).get("ID").asInt() == 1);
    assertTrue(((ArrayNode) entities.get(1).get("unidirectionalTargets")).size() == 2);
  }

}
