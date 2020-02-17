package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

public class TestFormat extends TestBase {

  @Test
  public void testFormatOptionXML() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons")
        .expand("Image1").format("xml");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String xml = helper.getRawResult();
    assertTrue(xml.startsWith("<?xml"));
  }

  @Test
  public void testRequestContentTypeXML() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/xml");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String xml = helper.getRawResult();
    assertTrue(xml.startsWith("<?xml"));
  }

  @Test
  public void testRequestContentTypeAtomXML() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.setRequestedResponseContentType("application/atom+xml");
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final String xml = helper.getRawResult();
    assertTrue(xml.startsWith("<?xml"));
  }

}
