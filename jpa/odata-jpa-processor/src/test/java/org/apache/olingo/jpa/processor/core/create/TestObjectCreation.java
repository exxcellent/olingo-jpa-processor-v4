package org.apache.olingo.jpa.processor.core.create;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestObjectCreation extends TestBase {

	@Test
	public void testCreationWithBooleanValue() throws IOException, ODataException {
		final StringBuffer requestBody = new StringBuffer("{");
		requestBody.append("\"ID\": " + Integer.toString((int) System.currentTimeMillis()));
		requestBody.append(", ");
		requestBody.append("\"ABoolean\": true");
		requestBody.append("}");

		final IntegrationTestHelper helper = new IntegrationTestHelper(emf, "DatatypeConversionEntities",
				requestBody, HttpMethod.POST);
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(1, orgs.size());
	}

}
