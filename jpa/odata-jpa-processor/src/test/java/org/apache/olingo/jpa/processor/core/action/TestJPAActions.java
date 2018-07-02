package org.apache.olingo.jpa.processor.core.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAActions extends TestBase {

	@Test
	public void testActionWithEntityParameter() throws IOException, ODataException {
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

}
