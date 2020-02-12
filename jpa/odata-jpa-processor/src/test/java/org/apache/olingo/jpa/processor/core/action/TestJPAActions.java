package org.apache.olingo.jpa.processor.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Collections;

import javax.persistence.Id;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.Phone;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfoHandler;
import org.apache.olingo.jpa.processor.core.testmodel.dto.SystemRequirement;
import org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAActions extends TestBase {

  @ODataDTO(handler = EnvironmentInfoHandler.class)
  public static class ActionDTO {

    @Id
    private final long id = System.currentTimeMillis();

    /**
     * Unbound oData action.
     */
    @EdmAction
    public static Organization createOrganization(@EdmActionParameter(name = "demoId") final String id,
        @EdmActionParameter(name = "withElementCollection") final boolean withElementCollection,
        @EdmActionParameter(name = "withAssociation") final boolean withAssociation,
        @Inject final JPAAdapter adapter) {
      if (adapter == null) {
        throw new IllegalStateException("JPAAdapter not onjected");
      }
      if (id == null || id.isEmpty()) {
        throw new IllegalStateException("Id not given");
      }
      final Organization org = new Organization();
      org.setID(id);
      org.setName1("name 1");
      org.setCountry("DEU");
      org.setCustomString1("custom 1");
      org.setType("1");
      final PostalAddressData address = org.getAddress();
      address.setCityName("Berlin");
      address.setPOBox("1234567");
      org.setAddress(address);
      // leave 'communicationData' untouched to transfer empty complex type (but not
      // null)
      assert org.getCommunicationData() != null;
      if (withElementCollection) {
        final Phone phone = new Phone();
        phone.setInternationalAreaCode("+42");
        phone.setPhoneNumber("987654321");
        org.addPhone(phone);
        org.getPhoneNumbersAsString().add("67676767676");
        org.getPhoneNumbersAsString().add("123-567-999");
      }
      if (withAssociation) {
        final BusinessPartnerRole role = new BusinessPartnerRole();
        role.setBusinessPartnerID(org.getID());
        role.setRoleCategory("TEST");
        org.setRoles(Collections.singletonList(role));
      }
      return org;
    }

  }

  @Test
  public void testBoundPrimitiveActionWithEntityParameter() throws IOException, ODataException {
    assumeTrue("Hibernate does not build a proper columns selection without quoting of column name",
        getJPAProvider() != JPAProvider.Hibernate);

    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"dummy\": " + Integer.toString(3)).append(", ");
    requestBody.append("\"country\": {");
    requestBody.append("\"Code\": \"DEU\"").append(", ");
    requestBody.append("\"Language\": \"de\"").append(", ");
    requestBody.append("\"Name\": \"Deutschland\"");
    requestBody.append("}");
    requestBody.append("}");

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Persons('99')/" + Constant.PUNIT_NAME + ".extractCountryCode", requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode object = helper.getJsonObjectValue();
    assertNotNull(object);
    assertEquals("DEU", object.get("value").asText());
  }

  @Test
  public void testUnboundVoidAction() throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "unboundVoidAction", null, HttpMethod.POST);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

  }

  @Test
  public void testUnboundEntityActionWithoutComplexTypesAndAssociations()
      throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(ActionDTO.class);

    final StringBuffer requestBody = new StringBuffer("{");
    final String testId = "3";
    requestBody.append("\"demoId\": \"" + testId + "\",");
    requestBody.append("\"withElementCollection\": false,");
    requestBody.append("\"withAssociation\": false");
    requestBody.append("}");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "createOrganization", requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode object = helper.getJsonObjectValue();
    assertNotNull(object);
    assertEquals(testId, object.get("ID").asText());
    assertTrue(object.get("CommunicationData").get("MobilePhoneNumber") instanceof NullNode);
    assertNotNull(object.get("PhoneNumbersAsString"));
    assertNotNull(object.get("PhoneNumbers"));
    assertTrue(((ArrayNode) object.get("PhoneNumbers")).size() == 0);
    assertTrue(((ArrayNode) object.get("PhoneNumbersAsString")).size() == 0);
  }

  @Test
  public void testUnboundEntityActionWithComplexTypesAndAssociations()
      throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(ActionDTO.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);

    final StringBuffer requestBody = new StringBuffer("{");
    final String testId = "5";
    requestBody.append("\"demoId\": \"" + testId + "\",");
    requestBody.append("\"withElementCollection\": true,");
    requestBody.append("\"withAssociation\": true");
    requestBody.append("}");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "createOrganization", requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ObjectNode object = helper.getJsonObjectValue();
    assertNotNull(object);
    assertEquals(testId, object.get("ID").asText());
    assertTrue(object.get("CommunicationData").get("MobilePhoneNumber") instanceof NullNode);
    assertNotNull(object.get("PhoneNumbersAsString"));
    assertNotNull(object.get("PhoneNumbers"));
    assertTrue(((ArrayNode) object.get("PhoneNumbers")).size() == 1);
    assertTrue(((ArrayNode) object.get("PhoneNumbersAsString")).size() == 2);
  }

  @Test
  public void testBoundPrimitiveActionWithEnumParameter() throws IOException, ODataException {
    assumeTrue("Hibernate does not build a proper columns selection without quoting of column name",
        getJPAProvider() != JPAProvider.Hibernate);

    final StringBuffer requestBody = new StringBuffer("{");
    final String testValue = TestEnum.Three.name();
    requestBody.append("\"value\": \"" + testValue + "\"");
    requestBody.append("}");

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "Persons('99')/" + Constant.PUNIT_NAME + ".sendBackEnumParameter", requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ObjectNode object = helper.getJsonObjectValue();
    assertNotNull(object);
    assertEquals(testValue, object.get("value").asText());
  }

  @Test
  public void testBoundActionForEntityWithEmbeddedId() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "AdministrativeDivisionDescriptions(CodePublisher='Eurostat',CodeID='NUTS3',DivisionCode='BE212',Language='de')/"
            + Constant.PUNIT_NAME + ".boundActionCheckLoadingOfEmbeddedId",
            null, HttpMethod.POST);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testUnboundActionForEntityWithEmbeddedId() throws IOException, ODataException {

    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"parameter\": {");
    requestBody.append("\"Name\": \"test\",");
    requestBody.append("\"CodePublisher\": \"Eurostat\",");
    requestBody.append("\"CodeID\": \"NUTS3\",");
    requestBody.append("\"DivisionCode\": \"BE212\",");
    requestBody.append("\"Language\": \"de\"");
    requestBody.append("}");
    requestBody.append("}");

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "unboundActionCheckLoadingOfEmbeddedId", requestBody, HttpMethod.POST);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testActionThrowingCustomHttpStatusErrorCode()
      throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "throwODataApplicationException", null, HttpMethod.POST);
    helper.execute(911);
  }

  @Test
  public void testActionInAbstractEntity() throws IOException, ODataException, NoSuchMethodException {
    assumeTrue("Hibernate cannot handle an abstract entity class as resource",
        getJPAProvider() != JPAProvider.Hibernate);

    URIBuilder uriBuilder;

    // the action must be present in all concrete/abstract entity classes
    uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipEntities").appendKeySegment(Integer.valueOf(1))
        .appendActionCallSegment(Constant.PUNIT_NAME + ".actionInAbstractEntity");
    final IntegrationTestHelper helper1 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper1.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities").appendKeySegment(Integer.valueOf(
        1)).appendActionCallSegment(Constant.PUNIT_NAME + ".actionInAbstractEntity");
    final IntegrationTestHelper helper2 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper2.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    uriBuilder = newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities").appendKeySegment(Integer.valueOf(
        2)).appendActionCallSegment(Constant.PUNIT_NAME + ".actionInAbstractEntity");
    final IntegrationTestHelper helper3 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper3.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    // must fail
    uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").appendKeySegment(Integer.valueOf(
        1)).appendActionCallSegment(Constant.PUNIT_NAME + ".actionInAbstractEntity");
    final IntegrationTestHelper helper4 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper4.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());

  }

  @Test
  public void testBoundActionInMappedSuperclass() throws IOException, ODataException, NoSuchMethodException {
    assumeTrue("Hibernate cannot handle an abstract entity class as resource",
        getJPAProvider() != JPAProvider.Hibernate);

    // the action must be present in all concrete/abstract entity classes

    final URIBuilder uriBuilder1 = newUriBuilder().appendEntitySetSegment("RelationshipEntities").appendKeySegment(
        Integer.valueOf(1)).appendActionCallSegment(Constant.PUNIT_NAME + ".actionInMappedSuperclass");
    final IntegrationTestHelper helper1 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder1,
        null, HttpMethod.POST);
    helper1.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    final URIBuilder uriBuilder2 = newUriBuilder().appendEntitySetSegment("RelationshipSourceEntities")
        .appendKeySegment(Integer.valueOf(1)).appendActionCallSegment(Constant.PUNIT_NAME
            + ".actionInMappedSuperclass");
    final IntegrationTestHelper helper2 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder2, null,
        HttpMethod.POST);
    helper2.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    final URIBuilder uriBuilder3 = newUriBuilder().appendEntitySetSegment("RelationshipTargetEntities")
        .appendKeySegment(Integer.valueOf(2)).appendActionCallSegment(Constant.PUNIT_NAME
            + ".actionInMappedSuperclass");
    final IntegrationTestHelper helper3 = new IntegrationTestHelper(persistenceAdapter, uriBuilder3, null,
        HttpMethod.POST);
    helper3.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

    final URIBuilder uriBuilder4 = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities")
        .appendKeySegment(Integer.valueOf(1)).appendActionCallSegment(Constant.PUNIT_NAME
            + ".actionInMappedSuperclass");
    final IntegrationTestHelper helper4 = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder4, null,
        HttpMethod.POST);
    helper4.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testUnboundActionWithCollectionResult() throws IOException, ODataException {

    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        "unboundActionWithStringCollectionResult", null, HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final ArrayNode objects = helper.getJsonObjectValues();
    assertTrue(objects.size() == 2);
  }

  @Test
  public void testBoundActionSavingToDatabase() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").appendKeySegment("10")
        .appendActionCallSegment(Constant.PUNIT_NAME + ".addPhoneToOrganizationAndSave");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

  @Test
  public void testBoundActionForNonExistingResource() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities").appendKeySegment(
        Integer.valueOf(9999)).appendActionCallSegment(Constant.PUNIT_NAME + ".actionInMappedSuperclass");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, null, HttpMethod.POST);
    helper.execute(HttpStatusCode.NOT_FOUND.getStatusCode());
  }

  @Test
  public void testUnboundActionWithPrimitiveCollectionResult() throws IOException, ODataException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithPrimitiveCollectionResult");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder, null,
        HttpMethod.POST);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValues().size() == 2);
  }

  @Test
  public void testActionWithDTOResultCollection() throws IOException, ODataException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("fillDTOWithNestedComplexType");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder, null, HttpMethod.POST);

    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValues().size() == 2);
    assertTrue(((ObjectNode) helper.getJsonObjectValues().get(0)).get("SystemRequirements").size() == 3);
  }

  @Test
  public void testMultipartFormContentUpload() throws Exception {

    // build complete multipart body in pure java
    // https://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
    final String fileName = "file-" + Long.toString(System.currentTimeMillis()) + ".log";
    final String boundary = "boundary";
    final String binaryData = "sdfjkgsdfjgsdkjfg";
    final String NEW_LINE = "\r\n";

    final StringBuffer requestBody = new StringBuffer("");
    // part 1: the filename as separate argument
    requestBody.append("--").append(boundary).append(NEW_LINE);
    requestBody.append("Content-Disposition: form-data; name=\"filename\"").append(NEW_LINE);
    requestBody.append(NEW_LINE);
    requestBody.append(fileName).append(NEW_LINE);
    // part 3: ignored data
    requestBody.append("--").append(boundary).append(NEW_LINE);
    requestBody.append("Content-Disposition: form-data; name=\"document-data\"").append(NEW_LINE);
    requestBody.append(NEW_LINE);
    requestBody.append("useless content").append(NEW_LINE);
    // part 3: the file content
    requestBody.append("--").append(boundary).append(NEW_LINE);
    requestBody.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"").append(NEW_LINE);
    requestBody.append("Content-Type: application/octet-stream").append(NEW_LINE);// content type of multi part entry
    requestBody.append(NEW_LINE);
    // we fake the content (no real binary content), because we a IT testing the call again
    requestBody.append(binaryData).append(NEW_LINE);
    // finalize multipart
    requestBody.append("--").append(boundary).append("--").append(NEW_LINE);

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("uploadFile");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder, requestBody, HttpMethod.POST);
    helper.setRequestContentType("multipart/form-data; boundary=" + boundary);// global content type

    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(((ArrayNode) helper.getJsonObjectValue().get("value")).get(0).asText().equals(fileName));
    assertTrue(((ArrayNode) helper.getJsonObjectValue().get("value")).get(1).asInt() == binaryData.getBytes().length);
  }

  @Test
  public void testUnboundActionWithMissedEmbedded() throws IOException, ODataException {

    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"pi\": {");
    requestBody.append("\"PID\": \"dummyId\", ");
    requestBody.append("\"AdministrativeInformation\": null");// empty to check handling
    requestBody.append("}");
    requestBody.append("}");

    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("checkPersonImageWithoutEmbeddedArgument");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
        uriBuilder, requestBody, HttpMethod.POST);

    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

}
