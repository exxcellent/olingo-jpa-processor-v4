package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPAEnumFilter extends TestBase {

  @Test
  public void testEnumStringBasedEq() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "AStringMappedEnum eq java.time.chrono.IsoEra'CE'");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode dces = helper.getValues();
    assertEquals(1, dces.size());
    assertEquals("CE", dces.get(0).get("AStringMappedEnum").asText());
  }

  @Test
  public void testEnumStringBasedNe() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "AStringMappedEnum ne java.time.chrono.IsoEra'BCE'");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode dces = helper.getValues();
    assertTrue(dces.size() > 0);
  }

  @Test
  public void testEnumOrdinalBased() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "AOrdinalMappedEnum eq java.time.temporal.ChronoUnit'NANOS'");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode dces = helper.getValues();
    assertEquals(1, dces.size());
    assertEquals("NANOS", dces.get(0).get("AOrdinalMappedEnum").asText());
  }

}
