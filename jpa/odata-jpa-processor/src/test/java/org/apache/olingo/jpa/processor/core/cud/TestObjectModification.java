package org.apache.olingo.jpa.processor.core.cud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.IdClass;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestObjectModification extends TestBase {

  @Test
  public void testSerialization() throws IOException, ODataException {

    final URIBuilder uriBuilderResource = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").appendKeySegment(
        Integer.valueOf(3));
    final ServerCallSimulator callRead = new ServerCallSimulator(persistenceAdapter, uriBuilderResource);
    callRead.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode dceFirstRead = callRead.getJsonObjectValue();
    assertEquals(2, ((ArrayNode) dceFirstRead.get("EnumCollection")).size());
    assertEquals(DayOfWeek.TUESDAY.name(), dceFirstRead.withArray("EnumCollection").get(0).asText());

    ((ArrayNode) dceFirstRead.get("EnumCollection")).removeAll().add(DayOfWeek.WEDNESDAY.name());
    dceFirstRead.put("AStringMappedEnum", IsoEra.BCE.name());
    dceFirstRead.put("AOrdinalMappedEnum", ChronoUnit.FOREVER.name());
    final StringBuffer requestBody = new StringBuffer(dceFirstRead.toString());
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilderResource,
        requestBody.toString(), HttpMethod.PUT);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode dceResponse = helper.getJsonObjectValue();
    assertNotNull(dceResponse);
    assertEquals(1, dceResponse.withArray("EnumCollection").size());

    // execute again
    callRead.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode dceSecondRead = callRead.getJsonObjectValue();
    assertEquals(IsoEra.BCE.name(), dceSecondRead.get("AStringMappedEnum").asText());
    assertEquals(1, dceSecondRead.withArray("EnumCollection").size());
    assertEquals(DayOfWeek.WEDNESDAY.name(), dceSecondRead.withArray("EnumCollection").get(0).asText());
  }
  
  @Test
  public void testUpdateEntityWithCompoundKeyIdClass() throws IOException, ODataException {
    assertNotNull(AdministrativeDivision.class.getAnnotation(IdClass.class));//verify entity is declared with IdClass for compound key
    
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "BE25");
    mapKeys.put("CodeID", "NUTS2");
    mapKeys.put("CodePublisher", "Eurostat");
    final URIBuilder uriBuilderResource = newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").appendKeySegment(mapKeys);
    final ServerCallSimulator callRead = new ServerCallSimulator(persistenceAdapter, uriBuilderResource);
    callRead.execute(HttpStatusCode.OK.getStatusCode());
    ObjectNode object = callRead.getJsonObjectValue();
    
    object.put("Population", 12345678);
    final StringBuffer requestBody = new StringBuffer(object.toString());
    final ServerCallSimulator callUpdate = new ServerCallSimulator(persistenceAdapter, uriBuilderResource,
        requestBody.toString(), HttpMethod.PUT);
    callUpdate.execute(HttpStatusCode.OK.getStatusCode());
    object = callUpdate.getJsonObjectValue();
    assertEquals(12345678, object.get("Population").asInt());
  }

  @Test
  public void testUpdateEntityWithCompoundKeyEmbeddedId() throws IOException, ODataException {
    
    final Map<String, Object> mapKeys = new HashMap<String, Object>();
    mapKeys.put("DivisionCode", "DEU");
    mapKeys.put("CodeID", "3166-1");
    mapKeys.put("CodePublisher", "ISO");
    mapKeys.put("Language", "de");
    final URIBuilder uriBuilderResource = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").appendKeySegment(mapKeys);
    final ServerCallSimulator callRead = new ServerCallSimulator(persistenceAdapter, uriBuilderResource);
    callRead.execute(HttpStatusCode.OK.getStatusCode());
    ObjectNode object = callRead.getJsonObjectValue();
    assertEquals("Deutschland", object.get("Name").asText());
    
    object.put("Name", "Bundesrepublik Deutschland");
    final StringBuffer requestBody = new StringBuffer(object.toString());
    final ServerCallSimulator callUpdate = new ServerCallSimulator(persistenceAdapter, uriBuilderResource,
        requestBody.toString(), HttpMethod.PUT);
    callUpdate.execute(HttpStatusCode.OK.getStatusCode());
    object = callUpdate.getJsonObjectValue();
    assertEquals("Bundesrepublik Deutschland", object.get("Name").asText());
  }

}
