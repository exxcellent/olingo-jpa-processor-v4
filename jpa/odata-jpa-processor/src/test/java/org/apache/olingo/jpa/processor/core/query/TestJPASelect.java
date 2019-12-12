package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
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

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendKeySegment("99");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode p = helper.getValue();
    assertEquals(99, p.get("ID").asLong());
    assertEquals(2, ((ArrayNode) p.get("PhoneNumbers")).size());
    assertNotNull(((ArrayNode) p.get("PhoneNumbers")).get(0).get("phoneNumber"));
  }

  /**
   * Test working datatype conversion between JPA and OData entity.
   */
  @Test
  public void testDatatypeConversionEntities() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").appendKeySegment(
        Integer.valueOf(1));
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode p = helper.getValue();
    assertEquals(1, p.get("ID").asLong());
  }

  @Test
  public void testSelectEmbeddedId() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").select(
        "CodePublisher", "DivisionCode").filter("CodeID eq 'NUTS3'");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode adds = helper.getValues();
    assertEquals(88, adds.size());
    // Not selected non key attributes must not be set
    assertNull(adds.get(0).get("Name"));

  }

  @Test
  public void testSelectRelationshipTargets() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").appendKeySegment(
        Integer.valueOf(1)).appendNavigationSegment("targets");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(2, targets.size());
  }

  @Test
  public void testSelectRelationshipSource() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities").appendKeySegment(
        Integer.valueOf(3)).appendNavigationSegment("SOURCE");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode source = helper.getValue();
    assertNotNull(source);
    assertEquals(1, source.get("ID").asInt());
  }

  @Test
  public void testSelectRelationshipM2NLeftNavigation() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").appendKeySegment(
        Integer.valueOf(1)).appendNavigationSegment("leftM2Ns");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(1, targets.size());
  }

  @Test
  public void testSelectRelationshipM2NRightNavigation() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities").appendKeySegment(
        Integer.valueOf(5)).appendNavigationSegment("RightM2Ns");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(1, targets.size());
  }

  @Test
  public void testSelectRelationshipOne2Many() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities").appendKeySegment(
        Integer.valueOf(5)).appendNavigationSegment("One2ManyTest");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(1, targets.size());
  }

  @Test
  public void testSelectRelationshipSecondM2NLeftNavigation() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipEntities").appendKeySegment(
        Integer.valueOf(2)).appendNavigationSegment("SecondLeftM2Ns");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(1, targets.size());
  }

  @Test
  public void testSelectRelationshipSecondM2NRightNavigation() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipEntities").appendKeySegment(
        Integer.valueOf(4)).appendNavigationSegment("SecondRightM2Ns");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(3, targets.size());
  }

  @Test
  public void testSelectRelationshipM2NBusinessPartnerRoles() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("BusinessPartners").appendKeySegment("5")
        .appendNavigationSegment("Locations");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode targets = helper.getValues();
    assertEquals(2, targets.size());
  }

}
