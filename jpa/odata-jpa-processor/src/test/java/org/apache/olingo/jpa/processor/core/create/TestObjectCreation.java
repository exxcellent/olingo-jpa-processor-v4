package org.apache.olingo.jpa.processor.core.create;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestObjectCreation extends TestBase {

  @Before
  public void setup() throws ODataJPAModelException {
    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
  }

  @Ignore("Keys are currently not forbidden, because OData<->JPA conversion is alos used for internal loading")
  @Test
  public void testIllegalCreationWithKey() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"ID\": " + Integer.toString((int) System.currentTimeMillis())).append(", ");
    requestBody.append("\"AStringMappedEnum\": \"BCE\"").append(", ");
    requestBody.append("\"AIntBoolean\": true");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.PRECONDITION_FAILED.getStatusCode());
  }

  @Test
  public void testIllegalDTOCreation() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Id\": " + Integer.toString((int) System.currentTimeMillis()));
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("EnvironmentInfos");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder, requestBody,
        HttpMethod.POST);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void testCreationWithGeneratedValueKey() throws IOException, ODataException {
    final String name = "CreatedSourceRelationshipEntity-" + Integer.toString((int) System.currentTimeMillis());
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Name\": \"" + name + "\"");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.CREATED.getStatusCode());

    final ObjectNode object = helper.getValue();
    assertNotNull(object);
    assertNotNull(object.get("ID").asText());
    assertEquals(name, object.get("Name").asText());
  }

}
