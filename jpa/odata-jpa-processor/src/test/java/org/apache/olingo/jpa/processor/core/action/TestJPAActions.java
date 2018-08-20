package org.apache.olingo.jpa.processor.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.persistence.Id;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressData;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfoHandler;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

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
			final PostalAddressData address = new PostalAddressData();
			address.setCityName("Berlin");
			address.setPOBox("1234567");
			org.setAddress(address);
			return org;
		}

	}

	@Test
	public void testBoundPrimitiveActionWithEntityParameter() throws IOException, ODataException {
		final StringBuffer requestBody = new StringBuffer("{");
		requestBody.append("\"dummy\": " + Integer.toString(3)).append(", ");
		requestBody.append("\"country\": {");
		requestBody.append("\"Code\": \"DEU\"").append(", ");
		requestBody.append("\"Language\": \"de\"").append(", ");
		requestBody.append("\"Name\": \"Deutschland\"");
		requestBody.append("}");
		requestBody.append("}");

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons('99')/" + PUNIT_NAME + ".extractCountryCode", requestBody, HttpMethod.POST);
		helper.assertStatus(HttpStatusCode.OK.getStatusCode());

		final ObjectNode object = helper.getValue();
		assertNotNull(object);
		assertEquals("DEU", object.get("value").asText());
	}

	@Test
	public void testUnboundVoidAction() throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"unboundVoidAction", null, HttpMethod.POST);
		helper.assertStatus(HttpStatusCode.NO_CONTENT.getStatusCode());

	}

	@Test
	public void testUnboundEntityAction() throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(ActionDTO.class);

		final StringBuffer requestBody = new StringBuffer("{");
		final String testId = "3";
		requestBody.append("\"demoId\": \"" + testId + "\"");
		requestBody.append("}");
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"createOrganization", requestBody, HttpMethod.POST);
		helper.assertStatus(HttpStatusCode.OK.getStatusCode());
		final ObjectNode object = helper.getValue();
		assertNotNull(object);
		assertEquals(testId, object.get("ID").asText());

	}

}
