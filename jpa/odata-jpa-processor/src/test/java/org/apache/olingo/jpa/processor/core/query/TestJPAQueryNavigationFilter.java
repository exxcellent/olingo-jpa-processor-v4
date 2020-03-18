package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

public class TestJPAQueryNavigationFilter extends TestBase {

  @Test
  public void testFilterAfterNavigationForConcreteEntity() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99")
        .appendNavigationSegment("PersonReferenceWithoutMappedAttribute").filter("Roles/any(d:d/RoleCategory eq 'X')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
    assertTrue(helper.getRawResult().contains("not allowed"));
  }

}
