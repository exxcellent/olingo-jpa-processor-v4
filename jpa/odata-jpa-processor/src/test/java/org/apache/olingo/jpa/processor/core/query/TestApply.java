package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestApply extends TestBase {

  @Test
  public void testAggregationSumMinMaxAvgCombination() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").addQueryOption(
        "apply",
        "aggregate(ADecimal with sum as Sum,ADecimal with min as Min,ADecimal with max as Max,ADecimal with average as Average)",
        false);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode response = helper.getJsonObjectValue();
    final ArrayNode valueArray = response.withArray("value");
    assertEquals(1, valueArray.size());
    final ObjectNode aggNode = (ObjectNode) valueArray.get(0);
    assertEquals("#Decimal", aggNode.get("Sum@odata.type").asText());
    // for Double no metadata is written
    assertEquals(-12.34, aggNode.get("Min").asDouble(), 0.0);
    assertEquals(98989886.00698, aggNode.get("Sum").asDouble(), 0.0);
  }

  @Test
  public void testUnsupportedApplyTransformation() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").addQueryOption(
        "apply", "topsum(2,AIntegerYear)", false);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
  }

  @Test
  public void testAggregationWithFilter() throws IOException, ODataException {

    // query should affect only one row (with value 0.00020)
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").addQueryOption(
        "apply",
        "aggregate(ADecimal with sum as Sum,ADecimal with min as Min,ADecimal with max as Max,ADecimal with average as Average)",
        false).filter("AIntegerYear gt 2000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode response = helper.getJsonObjectValue();
    final ArrayNode valueArray = response.withArray("value");
    assertEquals(1, valueArray.size());
    final ObjectNode aggNode = (ObjectNode) valueArray.get(0);
    assertEquals(0.00020, aggNode.get("Min").asDouble(), 0.0);
    assertEquals(0.00020, aggNode.get("Sum").asDouble(), 0.0);
    assertEquals(0.00020, aggNode.get("Max").asDouble(), 0.0);
    assertEquals(0.00020, aggNode.get("Average").asDouble(), 0.0);
  }

  @Test
  public void testAggregationWithZeroResult() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").addQueryOption(
        "apply", "aggregate(ADecimal with sum as Sum)", false).filter("AIntegerYear gt 9999999");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode response = helper.getJsonObjectValue();
    final ArrayNode valueArray = response.withArray("value");
    assertEquals(1, valueArray.size());
    final ObjectNode aggNode = (ObjectNode) valueArray.get(0);
    assertEquals(0.0, aggNode.get("Sum").asDouble(), 0.0);
  }

  @Test
  public void testAggregationWithFilterNavigation() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").addQueryOption("apply",
        "aggregate(ETag with sum as Sum)", false).filter("Roles/any(d:d/RoleCategory eq 'X')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode response = helper.getJsonObjectValue();
    final ArrayNode valueArray = response.withArray("value");
    assertEquals(1, valueArray.size());
    final ObjectNode aggNode = (ObjectNode) valueArray.get(0);
    assertEquals(11.0, aggNode.get("Sum").asDouble(), 0.0);
  }

}
