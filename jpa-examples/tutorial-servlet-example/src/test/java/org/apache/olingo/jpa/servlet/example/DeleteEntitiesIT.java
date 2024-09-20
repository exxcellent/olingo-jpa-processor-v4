package org.apache.olingo.jpa.servlet.example;

import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.api.communication.response.ODataDeleteResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Ralf Zozmann
 *
 * @see https://templth.wordpress.com/2014/12/03/accessing-odata-v4-service-with-olingo/
 *
 */
public class DeleteEntitiesIT {

  private ODataEndpointTestDefinition endpoint;

  @BeforeEach
  public void setup() {
    endpoint = new ODataEndpointTestDefinition();
  }

  @Test
  public void testDeleteNonExistingPerson() {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("9555559");
    try {
      endpoint.deleteEntity(uriBuilder,
          "Delete non existing person with ID 9555559");
    } catch (final ODataClientErrorException ex) {
      Assertions.assertTrue(ex.getStatusLine().getStatusCode() == HttpStatusCode.NOT_FOUND.getStatusCode());
    }
  }

  @Test
  public void testDeleteAnyExistingPerson() throws Exception {
    final int countBefore = countPersons();
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("98");
    final ODataDeleteResponse response = endpoint.deleteEntity(uriBuilder, "Delete person with ID 98");
    Assertions.assertTrue(response.getStatusCode() == HttpStatusCode.NO_CONTENT.getStatusCode());
    response.close();
    final int countAfter = countPersons();
    Assertions.assertEquals(countBefore, countAfter + 1);
  }

  @Test
  public void testDeletePersonWithoutPhones() throws Exception {
    final int countBefore = countPersons();
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("96");
    final ODataDeleteResponse response = endpoint.deleteEntity(uriBuilder, "Delete person with ID 96");
    Assertions.assertTrue(response.getStatusCode() == HttpStatusCode.NO_CONTENT.getStatusCode());
    response.close();
    final int countAfter = countPersons();
    Assertions.assertEquals(countBefore, countAfter + 1);
  }

  private int countPersons() throws Exception {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").count();
    final ODataRetrieveResponse<ClientPrimitiveValue> response = endpoint.retrieveValue(uriBuilder, "Count persons in database");
    Assertions.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final String sCount = response.getBody().toCastValue(String.class);
    response.close();
    return Integer.valueOf(sCount).intValue();
  }

}
