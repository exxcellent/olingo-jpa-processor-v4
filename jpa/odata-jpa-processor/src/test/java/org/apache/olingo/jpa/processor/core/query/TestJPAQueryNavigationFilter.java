package org.apache.olingo.jpa.processor.core.query;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestJPAQueryNavigationFilter extends TestBase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testFilterAfterNavigationForConcreteEntity() throws IOException, ODataException {

    // $filter after navigation property is currently not supported/allowed
    thrown.expectMessage("The system query option '$filter' is not allowed");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("PersonImages").appendKeySegment("99")
        .appendNavigationSegment("PersonReferenceWithoutMappedAttribute").filter("Roles/any(d:d/RoleCategory eq 'X')");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
  }

}
