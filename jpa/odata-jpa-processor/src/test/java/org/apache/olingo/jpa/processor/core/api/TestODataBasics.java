package org.apache.olingo.jpa.processor.core.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.chrono.IsoEra;
import java.util.Map;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.sub.SystemRequirement;
import org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestODataBasics extends TestBase {

  @Test
  public void testMetadata() throws IOException, ODataException {
    persistenceAdapter.registerDTOEntityType(EnvironmentInfo.class);
    persistenceAdapter.registerDTOEntityType(SystemRequirement.class);

    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String metadataString = helper.getRawResult();
    assertNotNull(metadataString);
    assertTrue(metadataString.length() > 1);
    final ObjectNode metadadataJson = helper.getJsonObjectValue();
    // check the presence of a few additional name spaces...
    assertNotNull(metadadataJson.get(EnvironmentInfo.class.getPackage().getName()));
    assertNotNull(metadadataJson.get("odata.dynamic"));//the name space of dynamic (open) data types (Map)
    assertNotNull(metadadataJson.get(TestEnum.class.getPackage().getName()));
    assertNotNull(metadadataJson.get(IsoEra.class.getPackage().getName()));
  }

  @Test
  public void testMetadataXML() throws IOException, ODataException {
    persistenceAdapter.registerDTOEntityType(EnvironmentInfo.class);
    persistenceAdapter.registerDTOEntityType(SystemRequirement.class);

    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType(ContentType.APPLICATION_XML.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String metadataString = helper.getRawResult();
    assertNotNull(metadataString);
    assertTrue(metadataString.contains(
        "ComplexType Name=\"MapType{1}\" Abstract=\"true\" OpenType=\"true\""),
        "EnvironmentInfo declares a Map, that must be present in meta data");
  }

  @Test
  public void testService() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String servicedata = helper.getRawResult();
    assertNotNull(servicedata);
    assertTrue(servicedata.length() > 1);
  }

  @Test
  public void testAll() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendAllSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
  }

  @Test
  public void testCrossjoin() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendCrossjoinSegment("Persons", "PersonImages");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
  }

  @Test
  public void testEntityId() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntityIdSegment("Persons('99')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode person = helper.getJsonObjectValue();
    assertNotNull(person);
  }

}
