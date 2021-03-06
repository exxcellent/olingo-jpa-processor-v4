package org.apache.olingo.jpa.servlet.example;

import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.BPImageIfc;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.testmodel.PersonImage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Ralf Zozmann
 *
 * @see https://templth.wordpress.com/2014/12/03/accessing-odata-v4-service-with-olingo/
 *
 */
public class ReadEntitiesIT {

  private ODataEndpointTestDefinition endpoint;

  @Before
  public void setup() {
    endpoint = new ODataEndpointTestDefinition();
  }

  @Test
  public void testNonExistingResource() {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("NoExistingResource").appendKeySegment("-3");
    try {
      endpoint.retrieveEntity(uriBuilder, "Try to load non existing resource");
    } catch(final ODataClientErrorException ex) {
      Assert.assertTrue(ex.getStatusLine().getStatusCode() == HttpStatusCode.NOT_FOUND.getStatusCode());
    }
  }

  @Test
  public void testMetadataAndCacheControl() {
    final ODataRetrieveResponse<Edm> response = endpoint.retrieveMetadata();
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    response.getHeader(HttpHeader.CACHE_CONTROL);
    // disabled cache control is set as default, so header must be present
    Assert.assertNotNull(response.getHeader(HttpHeader.CACHE_CONTROL));
    response.close();
  }

  @Test
  public void testInterfaceAndTargetEntity() throws Exception {
    // in the JPA model we have to check, that a interface is used, not a real impl class
    Assert.assertTrue(Person.class.getDeclaredField("image1").getType() == BPImageIfc.class);

    final ODataRetrieveResponse<Edm> response = endpoint.retrieveMetadata();
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final String simpleName = response.getBody().getEntityType(new FullQualifiedName("org.apache.olingo.jpa.Person"))
        .getNavigationProperty(
            "Image1").getType().getFullQualifiedName().getName();
    // in the OData metamodel the real impl class must be referenced, not the interface
    Assert.assertEquals(simpleName, PersonImage.class.getSimpleName());
    response.close();
  }

  @Test
  public void testPersonCount() throws Exception {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").count();
    final ODataRetrieveResponse<ClientPrimitiveValue> response = endpoint.retrieveValue(uriBuilder, "Count persons in database");
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final String sCount = response.getBody().toCastValue(String.class);
    Assert.assertTrue(Integer.valueOf(sCount).intValue() > 0);
    response.close();
  }

  @Test
  public void testLoadPerson() {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("99");
    final ODataRetrieveResponse<ClientEntity> response = endpoint.retrieveEntity(uriBuilder, "Load person with ID 99");
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final ClientEntity body = response.getBody();
    // the package name differs from oData namespace, so we can only compare the simple name
    Assert.assertTrue(Person.class.getSimpleName().equals(body.getTypeName().getName()));
    Assert.assertNotNull(body.getProperty("LastName"));
    response.close();
  }

  @Test
  public void testPersonWithExpand() {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("99")
        .addQueryOption(QueryOption.EXPAND, "Roles").addQueryOption(QueryOption.EXPAND, "Locations");
    final ODataRetrieveResponse<ClientEntity> response = endpoint.retrieveEntity(uriBuilder,
        "Load person with ID 99 and expanded navigation");
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final ClientEntity body = response.getBody();
    Assert.assertFalse(body.getNavigationLink("Roles").asInlineEntitySet().getEntitySet().getEntities().isEmpty());
    response.close();
  }

  @Test
  public void testDataConversions() {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("DatatypeConversionEntities");
    final ODataRetrieveResponse<ClientEntitySet> response = endpoint.retrieveEntityCollection(uriBuilder,
        "Load all data conversion entities");
    Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final ClientEntitySet body = response.getBody();
    Assert.assertTrue(body.getEntities().size() > 0);
    // the package name differs from oData namespace, so we can only compare the
    // simple name
    Assert.assertTrue(DatatypeConversionEntity.class.getSimpleName()
        .equals(body.getEntities().get(0).getTypeName().getName()));
    Assert.assertNotNull(body.getEntities().get(0).getProperty("ID"));
    response.close();
  }

}
