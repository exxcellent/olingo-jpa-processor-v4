package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

public class TestJPAQueryNavigationCount extends TestBase {

  @Test
  public void testEntitySetCount() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").count();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    assertEquals("10", helper.getRawResult());
  }

  @Test
  public void testEntityNavigateCount() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("3")
        .appendNavigationSegment("Roles").count();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    assertEquals("3", helper.getRawResult());
  }

}
