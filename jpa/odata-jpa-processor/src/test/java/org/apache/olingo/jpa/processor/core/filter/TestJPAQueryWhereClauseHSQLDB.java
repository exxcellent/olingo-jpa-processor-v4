package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * A few tests have problems with specific databases. Here are tests not running with default H2, but with HSQLDB
 */
public class TestJPAQueryWhereClauseHSQLDB extends TestBase {

  @Override
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    // H2 has problems with java.time.LocalDate
    return new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.HSQLDB);
  }

  @Test
  public void testFilterDayOfTime2LocalTime() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "ATime1 eq 22:21:20");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterDate2LocalDate() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "ADate2 eq 1600-12-01");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(1, entities.size());
  }

  @Test
  public void testFilterTimestamp2SqlTimestampWithDateConversion() throws IOException, ODataException {
    //    // FIXME
    //    // skip test...
    //    assumeTrue("This test fails on Travis", false);

    // '2010-01-01' will be expanded to '2010-01-01 00:00:00.0' (a complete
    // timestamp)
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").filter(
        "date(ATimestamp1SqlTimestamp) ge 2010-01-01");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode entities = helper.getJsonObjectValues();
    assertEquals(2, entities.size());
  }

}
