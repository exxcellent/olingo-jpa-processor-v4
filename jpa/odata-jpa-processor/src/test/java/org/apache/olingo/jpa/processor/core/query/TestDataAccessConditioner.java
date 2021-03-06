package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner.GenericBusinessPartnerDataAccessConditioner;
import org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner.GenericBusinessPartnerDataAccessConditioner.SelectionStrategy;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestDataAccessConditioner extends TestBase {

  @Override
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    return persistenceAdapter = new TestGenericJPAPersistenceAdapter("DataAccessConditioner", DataSourceHelper.DatabaseType.H2);
  }

  @Test
  public void testGETCount() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.ALL;

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "GenericBusinessPartners/$count");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    // we have 14 entries in BusinessPartner table
    assertEquals(14, Integer.parseInt(helper.getRawResult()));

    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyPersons;
    // execute again
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(4, Integer.parseInt(helper.getRawResult()));
  }

  @Test
  public void testGETSelect() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.ALL;

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "GenericBusinessPartners('3')");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode orgUnrestricted = helper.getJsonObjectValue();
    assertNotNull(orgUnrestricted);

    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyPersons;
    // execute again -> must now not be found, because Organization is not of type '1'
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testGETNavigation() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.ALL;

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "BusinessPartnerImages('97')/BusinessPartnerPerson");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode personUnrestricted = helper.getJsonObjectValue();
    assertNotNull(personUnrestricted);

    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyOrganizations;
    // execute again -> must now not be found, because Person is not of type '1' so we cannot have a Image
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testGETFilter() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.ALL;

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        "GenericBusinessPartners?$filter=Image/PID eq '97'");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode personImagesUnrestricted = helper.getJsonObjectValues();
    assertEquals(1, personImagesUnrestricted.size());

    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyOrganizations;
    // execute again -> must now not be found, because Person is not of type '1' so we cannot have a Image
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode personImagesRestricted = helper.getJsonObjectValues();
    assertEquals(0, personImagesRestricted.size());
  }

  @Test
  public void testDELETE() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyOrganizations;
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "GenericBusinessPartners('7')", null,
        HttpMethod.DELETE);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

  }

  @Test
  public void testInvalidDELETE() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyPersons;
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "GenericBusinessPartners('8')", null,
        HttpMethod.DELETE);
    helper.execute(HttpStatusCode.NOT_FOUND.getStatusCode());
  }

  /**
   * The access conditioner must affect also $search queries
   */
  @Test
  public void testSearchOption() throws IOException, ODataException {
    GenericBusinessPartnerDataAccessConditioner.SelectStrategy = SelectionStrategy.OnlyOrganizations;

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("GenericBusinessPartners").search("DEU");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode partners = helper.getJsonObjectValues();
    assertEquals(1, partners.size());

  }

}
