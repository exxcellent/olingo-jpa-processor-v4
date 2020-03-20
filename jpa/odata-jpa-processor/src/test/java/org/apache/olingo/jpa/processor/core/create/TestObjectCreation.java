package org.apache.olingo.jpa.processor.core.create;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestObjectCreation extends TestBase {

  @Before
  public void setup() throws ODataJPAModelException {
    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
  }

  @Ignore("Keys are currently not forbidden, because OData<->JPA conversion is also used for internal loading (bound actions for example)")
  @Test
  public void testIllegalCreationWithKey() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"ID\": " + Integer.toString((int) System.currentTimeMillis())).append(", ");
    requestBody.append("\"AStringMappedEnum\": \"BCE\"").append(", ");
    requestBody.append("\"AIntBoolean\": true");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody.toString(), HttpMethod.POST);
    helper.execute(HttpStatusCode.PRECONDITION_FAILED.getStatusCode());
  }

  @Test
  public void testIllegalDTOCreation() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Id\": " + Integer.toString((int) System.currentTimeMillis()));
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("EnvironmentInfos");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, requestBody
        .toString(), HttpMethod.POST);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void testCreationWithGeneratedValueKey() throws IOException, ODataException {
    final String name = "CreatedSourceRelationshipEntity-" + Integer.toString((int) System.currentTimeMillis());
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Name\": \"" + name + "\"");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody.toString(), HttpMethod.POST);
    helper.execute(HttpStatusCode.CREATED.getStatusCode());

    final ObjectNode object = helper.getJsonObjectValue();
    assertNotNull(object);
    assertNotNull(object.get("ID").asText());
    assertEquals(name, object.get("Name").asText());
  }

  @Test
  public void testCreationEntityWithNestedComplexType() throws IOException, ODataException {
    assumeTrue(
        "We cannot provide a Address/AdministrativeDivision instance, because ODataDeserializer does not suppport complex types having relationships... but Hibernate does not accept null value for that relationship",
        getJPAProvider() != JPAProvider.Hibernate);

    final String ADMINSITRATIVEINFORMATION_CREATED_BY = "ME";
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"ID\": \"" + Long.toString(System.currentTimeMillis())).append("\", ");
    requestBody.append("\"Type\": \"1\",");
    requestBody.append("\"AdministrativeInformation\": {");
    requestBody.append("  \"Created\": {");
    requestBody.append("    \"By\": \"" + ADMINSITRATIVEINFORMATION_CREATED_BY + "\"");
    requestBody.append("    }");
    requestBody.append("  },");
    requestBody.append("\"CommunicationData\": {");
    requestBody.append("  \"Fax\": \"+20 1234 5678\"");
    requestBody.append("  }");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("Persons");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.CREATED.getStatusCode());
    final ObjectNode entity = helper.getJsonObjectValue();
    assertNotNull(entity);
    // must have an empty default instance
    assertNotNull(entity.get("Address"));
    assertTrue(NullNode.class.isInstance(entity.get("Address").get("Country")));
    assertTrue(((ArrayNode) entity.get("PhoneNumbers")).size() == 0);
    assertTrue(((ArrayNode) entity.get("PhoneNumbersAsString")).size() == 0);
    assertTrue(((ArrayNode) entity.get("PartnerTelephoneConnections")).size() == 0);
    assertEquals(ADMINSITRATIVEINFORMATION_CREATED_BY, entity.get("AdministrativeInformation").get("Created").get("By")
        .asText());
  }

  @Test
  public void testCreationEntityWithElementCollection() throws IOException, ODataException {
    assumeTrue(
        "We cannot provide a Address/AdministrativeDivision instance, because ODataDeserializer does not suppport complex types having relationships... but Hibernate does not accept null value for that relationship",
        getJPAProvider() != JPAProvider.Hibernate);

    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"ID\": \"" + Long.toString(System.currentTimeMillis())).append("\", ");
    requestBody.append("\"Type\": \"1\",");
    requestBody.append("\"AdministrativeInformation\": {");
    requestBody.append("  \"Created\": {");
    requestBody.append("    \"By\": \"any\"");
    requestBody.append("    }");
    requestBody.append("  },");
    requestBody.append("\"Address\": {");
    requestBody.append("    \"Region\": \"anywhere\"");
    requestBody.append("  },");
    requestBody.append("\"PhoneNumbers\": [");
    requestBody.append("  {\"phoneNumber\": \"+20 1234 5678\"},");
    requestBody.append("  {\"internationalAreaCode\": \"+49\", \"phoneNumber\": \"20/1234 5678\"}");
    requestBody.append("  ]");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("Persons");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.CREATED.getStatusCode());
    final ObjectNode entity = helper.getJsonObjectValue();
    assertNotNull(entity);
    assertTrue(((ArrayNode) entity.get("PhoneNumbers")).size() == 2);
    // the derived (using the same table/columns as Phone numbers) values must now be filled...
    assertTrue(((ArrayNode) entity.get("PhoneNumbersAsString")).size() == 2);
    assertTrue(((ArrayNode) entity.get("PartnerTelephoneConnections")).size() == 2);
    assertFalse(NullNode.class.isInstance(entity.get("Address").get("Region")));
    assertTrue(NullNode.class.isInstance(entity.get("Address").get("PostalCode")));
  }

  @Test
  public void testCreationEntityWithDerivedRelationships() throws IOException, ODataException {
    // we have build the object network starting from 'target' entity referencing the 'source' entity, because the
    // 'target' is owner of all the ...:n relationships
    // the truly wanted relationship between 'source' and 'target' (using 'targets' cannot be declared, because we have
    // currently no ID generated for binding link)
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Name\": \"creation-target\"").append(", ");
    requestBody.append("\"SOURCE\": ");
    requestBody.append("  {");
    requestBody.append("    \"Name\": \"creation-source\"");
    requestBody.append("  }");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.CREATED.getStatusCode());

    final ObjectNode entityCreationTarget = helper.getJsonObjectValue();
    assertNotNull(entityCreationTarget);
    assertFalse(NullNode.class.isInstance(entityCreationTarget.get("Name")));
    assertTrue(IntNode.class.isInstance(entityCreationTarget.get("ID")));
    final ObjectNode entityCreationSource = (ObjectNode) entityCreationTarget.get("SOURCE");
    assertNotNull(entityCreationSource);
    assertFalse(NullNode.class.isInstance(entityCreationSource.get("Name")));

    final ClientEntity entityOlingo = helper.getOlingoEntityValue();
    assertNotNull(entityOlingo);
  }

  @Test
  public void testCreationEntityWithDeeperNestedRelationship() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("\"CodeID\": \"12345\"").append(", ");
    requestBody.append("\"DivisionCode\": \"DEU\"").append(", ");
    requestBody.append("\"CountryCode\": \"DEU\"").append(", ");
    requestBody.append("\"Area\": 1").append(", ");
    requestBody.append("\"Children\": [");
    requestBody.append("  {");
    requestBody.append("    \"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("    \"CodeID\": \"23456\"").append(", ");
    requestBody.append("    \"DivisionCode\": \"USA\"").append(", ");
    requestBody.append("    \"CountryCode\": \"USA\"").append(", ");
    requestBody.append("    \"ParentCodeID\": \"12345\"").append(", ");
    requestBody.append("    \"ParentDivisionCode\": \"DEU\"").append(", ");
    requestBody.append("    \"Area\": 2").append(", ");
    requestBody.append("    \"Children\": [");
    requestBody.append("      {");
    requestBody.append("        \"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("        \"CodeID\": \"34567-1\"").append(", ");
    requestBody.append("        \"DivisionCode\": \"POR\"").append(", ");
    requestBody.append("        \"CountryCode\": \"POR\"").append(", ");
    requestBody.append("        \"ParentCodeID\": \"23456\"").append(", ");
    requestBody.append("        \"ParentDivisionCode\": \"USA\"").append(", ");
    requestBody.append("        \"Area\": 3");
    requestBody.append("      }").append(", ");
    requestBody.append("      {");
    requestBody.append("        \"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("        \"CodeID\": \"234567-2\"").append(", ");
    requestBody.append("        \"DivisionCode\": \"POR\"").append(", ");
    requestBody.append("        \"CountryCode\": \"POR\"").append(", ");
    requestBody.append("        \"ParentCodeID\": \"23456\"").append(", ");
    requestBody.append("        \"ParentDivisionCode\": \"USA\"").append(", ");
    requestBody.append("        \"Area\": 3");
    requestBody.append("      }");
    requestBody.append("      ]");
    requestBody.append("  }");
    requestBody.append("  ]");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.setRequestedResponseContentType("application/json;odata.metadata=full");
    helper.execute(HttpStatusCode.CREATED.getStatusCode());

    final ObjectNode entityRoot = helper.getJsonObjectValue();
    assertNotNull(entityRoot);
    assertEquals("AdministrativeDivisions(DivisionCode='DEU',CodeID='12345',CodePublisher='ISO')", entityRoot.get(
        "@odata.id").asText());
    final ArrayNode rootChildren = (ArrayNode) entityRoot.get("Children");
    assertNotNull(rootChildren);
    assertEquals(1, rootChildren.size());
    assertEquals("AdministrativeDivisions(DivisionCode='USA',CodeID='23456',CodePublisher='ISO')", rootChildren.get(0)
        .get("@odata.id").asText());
    final ArrayNode childrenChildren = (ArrayNode) rootChildren.get(0).get("Children");
    assertNotNull(childrenChildren);
    assertEquals(2, childrenChildren.size());
    assertEquals("POR", childrenChildren.get(0).get("CountryCode").asText());
  }

  @Test
  public void testCreationEntityWithBindingLink() throws IOException, ODataException {
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("\"CodeID\": \"12345-p\"").append(", ");
    requestBody.append("\"DivisionCode\": \"DEU\"").append(", ");
    requestBody.append("\"CountryCode\": \"DEU\"").append(", ");
    requestBody.append("\"Area\": 1").append(", ");
    requestBody.append("\"Children\": [");
    requestBody.append("  {");
    // will have no effect in DB, but must be parsed by server side
    requestBody.append(
        "    \"Parent@odata.bind\": \"AdministrativeDivisions(DivisionCode='DEU',CodeID='12345-p',CodePublisher='ISO')\"")
    .append(", ");
    requestBody.append("    \"CodePublisher\": \"ISO\"").append(", ");
    requestBody.append("    \"CodeID\": \"23456-c\"").append(", ");
    requestBody.append("    \"DivisionCode\": \"USA\"").append(", ");
    requestBody.append("    \"CountryCode\": \"USA\"").append(", ");
    requestBody.append("    \"Area\": 2");
    requestBody.append("  }");
    requestBody.append("  ]");
    requestBody.append("}");

    final URIBuilder uriBuilder = TestBase.newUriBuilder().appendEntitySetSegment("AdministrativeDivisions");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder,
        requestBody, HttpMethod.POST);
    helper.setRequestedResponseContentType("application/json;odata.metadata=none");
    helper.execute(HttpStatusCode.CREATED.getStatusCode());
  }

}
