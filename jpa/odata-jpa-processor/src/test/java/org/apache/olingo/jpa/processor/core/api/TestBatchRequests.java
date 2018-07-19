package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class TestBatchRequests extends TestBase {

	@Test
	public void testOneGetRequestGetResponce() throws IOException, ODataException {
		final StringBuffer requestBody = createBodyOneGet();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$batch", requestBody,
				HttpMethod.POST);
		final List<String> act = helper.getRawBatchResult();
		assertNotNull(act);
	}

	@Test
	public void testOneGetRequestCheckStatus() throws IOException, ODataException {
		final StringBuffer requestBody = createBodyOneGet();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$batch", requestBody,
				HttpMethod.POST);
		assertEquals(200, helper.getBatchResultStatus(1));
	}

	@Test
	public void testOneGetRequestCheckValue() throws IOException, ODataException {
		final StringBuffer requestBody = createBodyOneGet();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$batch", requestBody,
				HttpMethod.POST);
		final JsonNode value = helper.getBatchResult(1);
		assertEquals("3", value.get("ID").asText());
	}

	@Test
	public void testTwoGetRequestSecondFailCheckStatus() throws IOException, ODataException {
		final StringBuffer requestBody = createBodyTwoGetOneFail();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$batch", requestBody,
				HttpMethod.POST);
		assertEquals(404, helper.getBatchResultStatus(2));
	}

	@Test
	public void testTwoGetRequestCheckValue() throws IOException, ODataException {
		final StringBuffer requestBody = createBodyTwoGet();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$batch", requestBody,
				HttpMethod.POST);
		final JsonNode value = helper.getBatchResult(2);
		assertEquals("5", value.get("ID").asText());
	}

	private StringBuffer createBodyTwoGetOneFail() {
		final StringBuffer requestBody = new StringBuffer("--abc123\r\n");
		requestBody.append("Content-Type: application/http\r\n");
		requestBody.append("Content-Transfer-Encoding: binary\r\n");
		requestBody.append("\r\n");
		requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
		requestBody.append("Content-Type: application/json\r\n");
		requestBody.append("\r\n");
		requestBody.append("\r\n");
		requestBody.append("--abc123\r\n");
		requestBody.append("Content-Type: application/http\r\n");
		requestBody.append("Content-Transfer-Encoding: binary\r\n");
		requestBody.append("\r\n");
		requestBody.append("GET AdministrativeDivision HTTP/1.1\r\n");
		requestBody.append("Content-Type: application/json\r\n");
		requestBody.append("\r\n");
		requestBody.append("\r\n");
		requestBody.append("--abc123--");
		return requestBody;
	}

	private StringBuffer createBodyTwoGet() {
		final StringBuffer requestBody = new StringBuffer("--abc123\r\n");
		requestBody.append("Content-Type: application/http\r\n");
		requestBody.append("Content-Transfer-Encoding: binary\r\n");
		requestBody.append("\r\n");
		requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
		requestBody.append("Content-Type: application/json\r\n");
		requestBody.append("\r\n");
		requestBody.append("\r\n");
		requestBody.append("--abc123\r\n");
		requestBody.append("Content-Type: application/http\r\n");
		requestBody.append("Content-Transfer-Encoding: binary\r\n");
		requestBody.append("\r\n");
		requestBody.append("GET Organizations('5') HTTP/1.1\r\n");
		requestBody.append("Content-Type: application/json\r\n");
		requestBody.append("\r\n");
		requestBody.append("\r\n");
		requestBody.append("--abc123--");
		return requestBody;
	}

	private StringBuffer createBodyOneGet() {
		final StringBuffer requestBody = new StringBuffer("--abc123\r\n");
		requestBody.append("Content-Type: application/http\r\n");
		requestBody.append("Content-Transfer-Encoding: binary\r\n");
		requestBody.append("\r\n");
		requestBody.append("GET Organizations('3') HTTP/1.1\r\n");
		requestBody.append("Content-Type: application/json\r\n");
		requestBody.append("\r\n");
		requestBody.append("\r\n");
		requestBody.append("--abc123--");
		return requestBody;
	}
}
