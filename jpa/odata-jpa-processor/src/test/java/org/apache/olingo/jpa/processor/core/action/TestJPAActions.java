package org.apache.olingo.jpa.processor.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.Collections;

import javax.persistence.Id;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.test.Constant;
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

		final ObjectNode object = helper.getValue();
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
		final ObjectNode object = helper.getValue();
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
		final ObjectNode object = helper.getValue();
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

		final ObjectNode object = helper.getValue();
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

		// the action must be present in all concrete/abstract entity classes

		final IntegrationTestHelper helper1 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipEntities(1)/" + Constant.PUNIT_NAME + ".actionInAbstractEntity",
		        null, HttpMethod.POST);
		helper1.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		final IntegrationTestHelper helper2 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipSourceEntities(1)/" + Constant.PUNIT_NAME + ".actionInAbstractEntity", null,
		        HttpMethod.POST);
		helper2.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		final IntegrationTestHelper helper3 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipTargetEntities(2)/" + Constant.PUNIT_NAME + ".actionInAbstractEntity", null,
		        HttpMethod.POST);
		helper3.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		// must fail
		final IntegrationTestHelper helper4 = new IntegrationTestHelper(persistenceAdapter,
		        "DatatypeConversionEntities(1)/" + Constant.PUNIT_NAME + ".actionInAbstractEntity", null,
		        HttpMethod.POST);
		helper4.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());

	}

	@Test
	public void testActionInMappedSuperclass() throws IOException, ODataException, NoSuchMethodException {
		assumeTrue("Hibernate cannot handle an abstract entity class as resource",
		        getJPAProvider() != JPAProvider.Hibernate);

		// the action must be present in all concrete/abstract entity classes

		final IntegrationTestHelper helper1 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipEntities(1)/" + Constant.PUNIT_NAME + ".actionInMappedSuperclass",
		        null, HttpMethod.POST);
		helper1.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		final IntegrationTestHelper helper2 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipSourceEntities(1)/" + Constant.PUNIT_NAME + ".actionInMappedSuperclass", null,
		        HttpMethod.POST);
		helper2.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		final IntegrationTestHelper helper3 = new IntegrationTestHelper(persistenceAdapter,
		        "RelationshipTargetEntities(2)/" + Constant.PUNIT_NAME + ".actionInMappedSuperclass", null,
		        HttpMethod.POST);
		helper3.execute(HttpStatusCode.NO_CONTENT.getStatusCode());

		final IntegrationTestHelper helper4 = new IntegrationTestHelper(persistenceAdapter,
		        "DatatypeConversionEntities(1)/" + Constant.PUNIT_NAME + ".actionInMappedSuperclass", null,
		        HttpMethod.POST);
		helper4.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Test
	public void testUboundActionWithCollectionResult() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
		        "unboundActionWithStringCollectionResult", null, HttpMethod.POST);
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ArrayNode objects = helper.getValues();
		assertTrue(objects.size() == 2);
	}

	@Test
	public void testActionSavingToDatabase() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
		        "Organizations('10')/" + Constant.PUNIT_NAME + ".addPhoneToOrganizationAndSave", null, HttpMethod.POST);
		helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Test
	public void testActionWithPrimitiveCollectionResult() throws IOException, ODataException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		persistenceAdapter.registerDTO(SystemRequirement.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
		        "actionWithPrimitiveCollectionResult", null, HttpMethod.POST);
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() == 2);
	}

	@Test
	public void testActionWithDTOResultCollection() throws IOException, ODataException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		persistenceAdapter.registerDTO(SystemRequirement.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
		        "fillDTOWithNestedComplexType", null, HttpMethod.POST);
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() == 2);
		assertTrue(((ObjectNode) helper.getValues().get(0)).get("SystemRequirements").size() == 3);
	}

}
