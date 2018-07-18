package org.apache.olingo.jpa.processor.core.create;

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

public class TestObjectCreation extends TestBase {

	@Test
	public void testCreation1() throws IOException, ODataException {
		final StringBuffer requestBody = new StringBuffer("{");
		requestBody.append("\"ID\": " + Integer.toString((int) System.currentTimeMillis())).append(", ");
		requestBody.append("\"AStringMappedEnum\": \"BCE\"").append(", ");
		requestBody.append("\"ABoolean\": true");
		requestBody.append("}");

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "DatatypeConversionEntities",
				requestBody, HttpMethod.POST);
		helper.assertStatus(HttpStatusCode.CREATED.getStatusCode());

		final ObjectNode object = helper.getValue();
		assertNotNull(object);
		assertEquals("BCE", object.get("AStringMappedEnum").asText());
	}

}
