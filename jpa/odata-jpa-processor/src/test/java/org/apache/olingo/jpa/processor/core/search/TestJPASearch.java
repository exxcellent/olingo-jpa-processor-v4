package org.apache.olingo.jpa.processor.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.Encoder;
import org.apache.olingo.jpa.processor.core.database.JPA_HANADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPASearch extends TestBase {

  @Test
  public void testAllAttributesSimpleCase() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").search("Org");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode ents = helper.getJsonObjectValues();
    // Nameline1 should match for all Organizations
    assertEquals(10, ents.size());
  }

  /**
   * The attributes used for $search must be derived also from a (@Embedded) complex type
   */
  @Test
  public void testAllAttributesDerivedFromEmbeddable() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate produces an SQL selecting the search columns from the wrong table");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("EntityWithSecondaryTableAndEmbeddedSet")
        .search("\"96\"");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(1, ents.size());
    assertTrue(ents.get(0).get("ID").asText().endsWith("1"));
    assertTrue(ents.get(0).get("data").asText().endsWith("other DATA"));
  }

  @Test
  public void testMultipleAttributesSearchWithPhrase() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate has a stupid parameter binding check not accepting '%001%' as pattern for java.net.URL attribute");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "\"001\"");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode ents = helper.getJsonObjectValues();
    // should have 1 result for matching UUID
    assertEquals(1, ents.size());
    assertTrue(ents.get(0).get("Uuid").asText().endsWith("001"));
  }

  @Test
  public void testMultipleAttributesSearchWithComplexExpressionOR() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate has a stupid parameter binding check not accepting '%12345%' as pattern for java.math.BigDecimal");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "anywhere OR 12345");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(3, ents.size());
  }

  @Test
  public void testMultipleAttributesSearchWithSpacePhrase() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").search(
        "\"Test Road\" AND NOT \"Seventh Org\"");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    // the 'Tenth Org.' as a NULL value for "Adress.Region", so the NOT-Condition will exclude it from "...AND NOT(...)"
    // condition
    // so the result is -1 as expected
    assertEquals(8, ents.size());
  }

  @Test
  public void testMultipleAttributesSearchWithComplexExpressionANDNOT() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate has a stupid parameter binding check not accepting '%2000%' as pattern for java.math.BigDecimal");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "anywhere AND NOT 2000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(1, ents.size());
  }

  @Test
  public void testMultipleAttributesSearchWithComplexExpressionAND() throws IOException, ODataException {
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate has a stupid parameter binding check not accepting '%2000%' as pattern for java.math.BigDecimal");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "anywhere AND 2000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(1, ents.size());
  }

  @Test
  public void testHANACaseInsensitiveSearch() throws IOException, ODataException {
    // skip test with Hibernate
    assumeTrue(
        getJPAProvider() != JPAProvider.Hibernate,
        "Hibernate does not accept the search string as value for an BigDecimal attribute having @EdmSearchable");

    final Map<String, Object> properties = new HashMap<String, Object>();
    final DataSource ds = DataSourceHelper.createDataSource(DataSourceHelper.DatabaseType.H2);
    properties.put(Constant.ENTITY_MANAGER_DATA_SOURCE, ds);

    // use HANA SQL dialect working against H2 database (will only work for SQL code generation not at runtime)
    persistenceAdapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        properties, new JPA_HANADatabaseProcessor());
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").search(
        "AnyWhere");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testUmlautInPhrase() throws IOException, ODataException {

    // double encoding required because OLINGO-1239
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("AdministrativeDivisionDescriptions").search(
        "\"" + Encoder.encode(Encoder.encode("ö")) + "\"");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(1, ents.size());
    assertEquals("Bezirk Löwen", ents.get(0).get("Name").asText());
  }

  @ParameterizedTest
  @CsvSource(value = {"\"John\" \"Doe\":1", "John Doe:1", "John AND Doe:1", "Nothing OR Dorf:2", "\"Test\" AND \"Stra%C3%9Fe\":2", "(John AND Doe) OR (\"Max\" AND Mustermann):2"}, delimiter = ':')
  public void testParametrizedQuoteEffects(String searchTerm, int expected) throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").search(searchTerm);
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
        uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode ents = helper.getJsonObjectValues();
    assertEquals(expected, ents.size());
  }

}
