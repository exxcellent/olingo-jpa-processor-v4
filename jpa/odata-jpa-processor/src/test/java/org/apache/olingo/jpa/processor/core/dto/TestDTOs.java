package org.apache.olingo.jpa.processor.core.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.junit.Before;
import org.junit.Test;

public class TestDTOs extends TestBase {

  @Before
  public void setup() throws ODataJPAModelException {
    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
  }

  @Test(expected = ODataJPAModelException.class)
  public void testNonDTOThrowsError() throws IOException, ODataException, SQLException {
    // create own instance to avoid pollution of other tests
    final TestGenericJPAPersistenceAdapter myPersistenceAdapter = new TestGenericJPAPersistenceAdapter(
        Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.HSQLDB);
    myPersistenceAdapter.registerDTO(TestDTOs.class);
    // must throw an exception on further processing
    final IntegrationTestHelper helper = new IntegrationTestHelper(myPersistenceAdapter, "$metadata");
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testDTOMetadata() throws IOException, ODataException, SQLException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$metadata");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final String xml = helper.getRawResult();
    assertTrue(!xml.isEmpty());
    assertTrue(xml.contains(EnvironmentInfo.class.getSimpleName()));
    assertTrue(xml.contains(EnvironmentInfo.class.getSimpleName().concat("s")));// + entity set
  }

  @Test
  public void testGetDTO() throws IOException, ODataException, SQLException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "EnvironmentInfos");
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getValues().size() > 0);
    assertEquals(System.getProperty("java.version"), helper.getValues().get(0).get("JavaVersion").asText());
  }

  @Test
  public void testGetSpecificDTO() throws IOException, ODataException, SQLException {
    // our example DTO handler will not support loading of a DTO with a specific ID
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "EnvironmentInfos(1)");
    helper.execute(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  public void testWriteDTO() throws IOException, ODataException, SQLException {
    final String id = Integer.toString((int) System.currentTimeMillis());
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"EnvNames\": [\"envA\"], ");
    requestBody.append("\"JavaVersion\": \"0.0.ex\", ");
    requestBody.append("\"Id\": " + id);
    requestBody.append("}");

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "EnvironmentInfos(" + id + ")",
        requestBody.toString(), HttpMethod.PUT);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(id, helper.getValue().get("Id").asText());
  }

}
