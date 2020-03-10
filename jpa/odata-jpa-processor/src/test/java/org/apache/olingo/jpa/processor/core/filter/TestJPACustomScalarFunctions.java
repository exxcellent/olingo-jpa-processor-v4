package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPACustomScalarFunctions {

  private static TestGenericJPAPersistenceAdapter persistenceAdapter;

  @BeforeClass
  public static void setupClass() throws ODataJPAModelException {
    persistenceAdapter = new TestGenericJPAPersistenceAdapter(
        Constant.PUNIT_NAME, DataSourceHelper.DatabaseType.HSQLDB);
    CreateDenfityFunction();
  }

  @AfterClass
  public static void tearDownClass() throws ODataJPAModelException {
    DropDenfityFunction();
  }

  @Test
  public void testFunctionCall() throws IOException, ODataException {

    // TODO support function calls with primitive result
    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendOperationCallSegment(
        "PopulationDensity(Area=42,Population=12345)");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
  }

  @Test
  public void testFilterOnFunction() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Area=$it/Area,Population=$it/Population) gt 1");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode values = helper.getJsonObjectValues();

    assertEquals(0, values.size());
  }

  @Test
  public void testFilterOnFunctionAndProperty() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Area=$it/Area,Population=$it/Population)  mul 1000000 gt 1000 and ParentDivisionCode eq 'BE255'&orderBy=DivisionCode)");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(2, orgs.size());
    assertEquals("35002", orgs.get(0).get("DivisionCode").asText());
  }

  @Test
  public void testFilterOnFunctionAndMultiply() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Area=Area,Population=Population)  mul 1000000 gt 100");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(59, orgs.size());
  }

  @Test
  public void testFilterOnFunctionWithFixedValue() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Area=13079087,Population=$it/Population)  mul 1000000 gt 1000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(29, orgs.size());
  }

  @Test
  public void testFilterOnFunctionComuteValue() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Area=Area div 1000000,Population=Population) gt 1000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(7, orgs.size());
  }

  @Test
  public void testFilterOnFunctionMixParamOrder() throws IOException, ODataException {

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions").filter(
        "org.apache.olingo.jpa.PopulationDensity(Population=Population,Area=Area) mul 1000000 gt 1000");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode orgs = helper.getJsonObjectValues();
    assertEquals(7, orgs.size());
  }

  private static void CreateDenfityFunction() {
    final EntityManager em = persistenceAdapter.getEMF().createEntityManager();
    final EntityTransaction t = em.getTransaction();

    final StringBuffer sqlString = new StringBuffer();

    sqlString.append(
        "CREATE FUNCTION  \"OLINGO\".\"org.apache.olingo.jpa::PopulationDensity\" (UnitArea  INT, Population BIGINT ) ");
    sqlString.append("RETURNS DOUBLE ");
    sqlString.append("BEGIN ATOMIC  "); //
    sqlString.append("  DECLARE aDouble DOUBLE; "); //
    sqlString.append("  DECLARE pDouble DOUBLE; ");
    sqlString.append("  SET aDouble = UnitArea; ");
    sqlString.append("  SET pDouble = Population; ");
    sqlString.append("  IF UnitArea <= 0 THEN RETURN 0; ");
    sqlString.append("  ELSE RETURN pDouble  / aDouble; "); // * 1000000
    sqlString.append("  END IF;  "); //
    sqlString.append("END");

    t.begin();
    final Query q = em.createNativeQuery(sqlString.toString());
    q.executeUpdate();
    t.commit();
  }

  private static void DropDenfityFunction() {
    final EntityManager em = persistenceAdapter.getEMF().createEntityManager();
    final EntityTransaction t = em.getTransaction();

    final StringBuffer sqlString = new StringBuffer();

    sqlString.append("DROP FUNCTION  \"OLINGO\".\"org.apache.olingo.jpa::PopulationDensity\"");

    t.begin();
    final Query q = em.createNativeQuery(sqlString.toString());
    q.executeUpdate();
    t.commit();
  }
}
