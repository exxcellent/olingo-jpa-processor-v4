package org.apache.olingo.jpa.processor.core.cud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoUnit;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
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
        requestBody, HttpMethod.PUT);
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


}
