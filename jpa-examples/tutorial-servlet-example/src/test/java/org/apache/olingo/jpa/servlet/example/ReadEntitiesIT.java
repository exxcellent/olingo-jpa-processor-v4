package org.apache.olingo.jpa.servlet.example;

import org.apache.olingo.client.api.communication.ODataClientErrorException;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
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
	public void test1() {
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("NoExistingResource").appendKeySegment("-3");
		try {
			endpoint.retrieveEntity(uriBuilder, "Try to load non existing resource");
		} catch(final ODataClientErrorException ex) {
			Assert.assertTrue(ex.getStatusLine().getStatusCode() == HttpStatusCode.NOT_FOUND.getStatusCode());
		}
	}

	@Test
	public void test2() {
		final ODataRetrieveResponse<Edm> response = endpoint.retrieveMetadata();
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		response.close();
	}

	@Test
	public void test3() throws Exception {
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").count();
		final ODataRetrieveResponse<ClientPrimitiveValue> response = endpoint.retrieveValue(uriBuilder, "Count persons in database");
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		final String sCount = response.getBody().toCastValue(String.class);
		Assert.assertTrue(Integer.valueOf(sCount).intValue() > 0);
		response.close();
	}

	@Test
	public void test4() {
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
	public void test4WithExpand() {
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
	public void test5() {
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
