package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ImageLoader;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAQuerySelectByPath extends TestBase {

  @Test
  public void testNavigationToOwnPrimitiveProperty() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Name1");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("Third Org.", org.get("value").asText());
  }

  @Ignore
  @Test
  public void testNavigationToOwnPrimitiveDescriptionProperty() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("LocationName");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("Vereinigte Staaten von Amerika", org.get("value").asText());
  }

  @Test
  public void testNavigationToComplexProperty() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("Address");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("USA", org.get("Country").asText());
  }

  @Test
  public void testNavigationToNestedComplexProperty() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("AdministrativeInformation").appendNavigationSegment("Created");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    final JsonNode created = org.get("Created");
    assertEquals("98", created.get("By").asText());
  }

  @Ignore
  @Test
  public void testNavigationViaComplexAndNaviPropertyToPrimitive() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("AdministrativeInformation").appendNavigationSegment("Created")
        .appendNavigationSegment("User").appendNavigationSegment("FirstName");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("Max", org.get("value").asText());
  }

  @Test
  public void testNavigationToComplexPropertySelect() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("Address").select("Country", "Region");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals(3, org.size()); // Node "@odata.context" is also counted
    assertEquals("USA", org.get("Country").asText());
    assertEquals("US-UT", org.get("Region").asText());
  }

  @Test
  public void testNavigationToComplexPropertyExpand() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("Address");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("USA", org.get("Country").asText());
  }

  @Test
  public void testNavigationToComplexPrimitiveProperty() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("Address").appendNavigationSegment("Region");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode org = helper.getJsonObjectValue();
    assertEquals("US-UT", org.get("value").asText());
    assertEquals("../../$metadata#Organizations/Address/Region", org.get("@odata.context").asText());
  }

  @Ignore
  @Test
  public void testNavigationToStreamValue() throws IOException, ODataException {
    new ImageLoader().loadPerson(persistenceAdapter.createEntityManager(), "OlingoOrangeTM.png", "99");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99").appendValueSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());

    final byte[] act = helper.getBinaryResult();
    assertEquals(93316, act.length, 0);
  }

  // TODO
  @Ignore("MediaEntityProcessor required")
  @Test
  public void testNavigationToStreamValueVia() throws IOException, ODataException {
    new ImageLoader().loadPerson(persistenceAdapter.createEntityManager(), "OlingoOrangeTM.png", "99");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendKeySegment("99")
        .appendNavigationSegment("Image1").appendValueSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final byte[] act = helper.getBinaryResult();
    assertEquals(93316, act.length, 0);
  }

  @Test
  public void testNavigationToComplexAttributeValue() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue("Hibernate produce invalid SQL", getJPAProvider() != JPAProvider.Hibernate);

    new ImageLoader().loadPerson(persistenceAdapter.createEntityManager(), "OlingoOrangeTM.png", "99");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("AdministrativeInformation").appendNavigationSegment("Created")
        .appendNavigationSegment("By").appendValueSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String act = helper.getRawResult();
    assertEquals("98", act);
  }

  @Test
  public void testNavigationToPrimitiveAttributeValue() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue("Hibernate produce invalid SQL", getJPAProvider() != JPAProvider.Hibernate);

    new ImageLoader().loadPerson(persistenceAdapter.createEntityManager(), "OlingoOrangeTM.png", "99");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("4")
        .appendNavigationSegment("ID").appendValueSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String act = helper.getRawResult();
    assertEquals("4", act);
  }
}
