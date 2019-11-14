package org.apache.olingo.jpa.client.example;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple unit tests for generated code. For real world integration test see <i>olingo-generic-servlet-example</i>.
 * @author Ralf Zozmann
 *
 */
public class PersonAccessTest {

  private static final String serviceRoot = "http://localhost:1234/jcage";

  private PersonAccess endpoint;

  @Before
  public void setup() throws URISyntaxException {
    endpoint = new PersonAccess(new URI(serviceRoot));
  }

  @Test
  public void test02ActionBound1() throws Exception {
    // URIBuilder uriBuilder = endpoint.newUri().appendOperationCallSegment("ClearPersonsCustomStrings");
    final PersonDto dto = endpoint.retrieve("99");
    Assert.assertNotNull(dto);
  }


}
