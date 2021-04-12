package org.apache.olingo.jpa.processor.core.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestDTOs extends TestBase {

  @ODataDTO
  public static class EnumDto {
    // use an enum not yet registered... and the DTO of custom schema will trigger enum type creation in another custom
    // schema...
    @SuppressWarnings("unused")
    private StandardProtocolFamily family;
  }

  /**
   * DTO inherit from another
   *
   */
  @ODataDTO
  public static class InheritanceDto extends SystemRequirement {
    // because of default naming strategy this attribute will be named 'Any' in OData part, all attributes from super
    // class are named 'as is' (lower case)
    @SuppressWarnings("unused")
    private String any;

    public InheritanceDto() {
      super("req", "desc");
    }

    @EdmAction
    public static InheritanceDto produceOne() {
      final InheritanceDto instance = new InheritanceDto();
      instance.any = "any";
      return instance;
    }
  }

  @ODataComplexType(attributeNaming = NamingStrategy.AsIs)
  public static class NestedComplexType {
    private String attribute1;
    private String attribute2;
  }

  @ODataDTO(attributeNaming = NamingStrategy.AsIs)
  public static class DtoWithNestedComplexType {
    private final Collection<NestedComplexType> cts = new LinkedList<>();

    @EdmAction
    public static DtoWithNestedComplexType createDtoWithNestedComplexType() {
      final DtoWithNestedComplexType instance = new DtoWithNestedComplexType();
      final NestedComplexType ct = new NestedComplexType();
      ct.attribute1 = "a1";
      ct.attribute2 = "a2";
      instance.cts.add(ct);
      return instance;
    }

    @EdmAction
    public static int countCompleteCTs(@EdmActionParameter(name = "dto") final DtoWithNestedComplexType dto) {
      int c = 0;
      for (final NestedComplexType ct : dto.cts) {
        if (ct.attribute1 == null || ct.attribute2 == null) {
          continue;
        }
        c++;
      }
      return c;
    }
  }

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
    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(myPersistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testDTOMetadata() throws IOException, ODataException, SQLException {

    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final String json = helper.getRawResult();
    assertTrue(!json.isEmpty());
    assertTrue(json.contains(EnvironmentInfo.class.getSimpleName()));
    assertTrue(json.contains(EnvironmentInfo.class.getSimpleName().concat("s")));// + entity set
  }

  @Test
  public void testGetDTO() throws IOException, ODataException, SQLException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("EnvironmentInfos");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValues().size() > 0);
    assertEquals(System.getProperty("java.version"), helper.getJsonObjectValues().get(0).get("JavaVersion").asText());
  }

  @Test
  public void testGetSpecificDTO() throws IOException, ODataException, SQLException {
    // our example DTO handler will not support loading of a DTO with a specific ID
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("EnvironmentInfos").appendKeySegment(Integer
        .valueOf(1));
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());
  }

  @Test
  public void testWriteDTO() throws IOException, ODataException, SQLException {
    final int iId = (int) System.currentTimeMillis();
    final String sId = Integer.toString(iId);
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"EnvNames\": [\"envA\"], ");
    requestBody.append("\"JavaVersion\": \"0.0.ex\", ");
    requestBody.append("\"MapOfNumberCollections\": {\"NullTest\": null, \"numberOfEnv\": [0, 1]},");
    requestBody.append("\"Id\": " + sId);
    requestBody.append("}");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("EnvironmentInfos").appendKeySegment(Integer
        .valueOf(iId));
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody.toString(), HttpMethod.PUT);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(sId, helper.getJsonObjectValue().get("Id").asText());
  }

  @Test
  public void testDTOWithEnumAttribute() throws IOException, ODataException, SQLException {
    persistenceAdapter.registerDTO(EnumDto.class);

    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.GET);
    helper.setRequestedResponseContentType(ContentType.APPLICATION_XML.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testDTOWithInheritance() throws IOException, ODataException, SQLException {
    persistenceAdapter.registerDTO(InheritanceDto.class);

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("produceOne");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode result = helper.getJsonObjectValue();
    assertNotNull(result.get("Any").asText());// starting upper case
    assertNotNull(result.get("requirementName").asText()); // starting lower case
    assertNotNull(result);
  }

  @Test(expected = ODataJPAModelException.class)
  public void testInvalidRegisteredDTOComplexType() throws ODataException, IOException {
    persistenceAdapter.registerDTO(NestedComplexType.class);
    // trigger metamodel building and exception...
    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.GET);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testDTOWithNestedComplexType() throws IOException, ODataException, SQLException {
    persistenceAdapter.registerDTO(DtoWithNestedComplexType.class);

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("createDtoWithNestedComplexType");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode result = helper.getJsonObjectValue();
    assertFalse(result.withArray("cts").isEmpty());
    assertEquals("a1", result.withArray("cts").get(0).get("attribute1").asText());
    assertEquals("a2", result.withArray("cts").get(0).get("attribute2").asText());

    // send same structure as input for action
    final URIBuilder uriBuilderSend = newUriBuilder().appendActionCallSegment("countCompleteCTs");

    final StringBuffer requestBody1 = new StringBuffer("{");
    requestBody1.append("\"dto\": {");
    requestBody1.append("  \"cts\": [{\"attribute1\": \"p1\"}]");
    requestBody1.append("  }");
    requestBody1.append("}");
    final ServerCallSimulator helperSend1 = new ServerCallSimulator(persistenceAdapter, uriBuilderSend, requestBody1
        .toString(), HttpMethod.POST);
    helperSend1.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(0, helperSend1.getJsonObjectValue().get("value").asInt());

    final StringBuffer requestBody2 = new StringBuffer("{");
    requestBody2.append("\"dto\": {");
    requestBody2.append(
        "  \"cts\": [{\"attribute1\": \"p1\", \"attribute2\": \"p2\"}, {\"attribute1\": \"p3\", \"attribute2\": \"p4\"}]");
    requestBody2.append("  }");
    requestBody2.append("}");

    final ServerCallSimulator helperSend2 = new ServerCallSimulator(persistenceAdapter, uriBuilderSend, requestBody2
        .toString(), HttpMethod.POST);
    helperSend2.execute(HttpStatusCode.OK.getStatusCode());
    assertEquals(2, helperSend2.getJsonObjectValue().get("value").asInt());
  }
}
