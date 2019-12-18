package org.apache.olingo.jpa.processor.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.Encoder;
import org.apache.olingo.jpa.processor.core.database.JPA_HANADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPASearch extends TestBase {

  @Test
  public void testAllAttributesSimpleCase() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").search("Org");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode ents = helper.getValues();
    // Nameline1 should match for all Organizations
    assertEquals(10, ents.size());
  }

  @Test
  public void testMultipleAttributesSearchWithPhrase() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue(
        "Hibernate has a stupid parameter binding check not accepting '%001%' as pattern for java.net.URL attribute",
        getJPAProvider() != JPAProvider.Hibernate);

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "\"001\"");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode ents = helper.getValues();
    // should have 1 result for matching UUID
    assertEquals(1, ents.size());
    assertTrue(ents.get(0).get("Uuid").asText().endsWith("001"));
  }

  @Test
  public void testMultipleAttributesSearchWithComplexExpression() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "anywhere OR \"888\"");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder);
    // not supported -> TODO
    helper.execute(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());

  }

  @Test
  public void testHANACaseInsensitiveSearch() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue(
        "Hibernate does not accept the search string as value for an BigDecimal attribute having @EdmSearchable",
        getJPAProvider() != JPAProvider.Hibernate);

    final Map<String, Object> properties = new HashMap<String, Object>();
    final DataSource ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.H2);
    properties.put(Constant.ENTITY_MANAGER_DATA_SOURCE, ds);

    // use HANA SQL dialect working against H2 database (will only work for SQL code generation not at runtime)
    persistenceAdapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        properties, new JPA_HANADatabaseProcessor());
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "AnyWhere");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testUmlautInPhrase() throws IOException, ODataException {

    // double encoding required because OLINGO-1239
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").search(
        "\"" + Encoder.encode(Encoder.encode("ö")) + "\"");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getValues();
    assertEquals(1, ents.size());
    assertEquals("Bezirk Löwen", ents.get(0).get("Name").asText());
  }

}
