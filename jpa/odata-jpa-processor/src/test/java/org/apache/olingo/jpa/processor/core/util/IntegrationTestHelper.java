package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.jpa.processor.core.api.JPAODataGetHandler;
import org.apache.olingo.jpa.processor.core.database.JPADefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.processor.DefaultProcessor;
import org.apache.olingo.server.api.processor.ErrorProcessor;
import org.apache.olingo.server.api.processor.Processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class IntegrationTestHelper {

	private static class TestErrorProcessor extends DefaultProcessor implements ErrorProcessor {
		Logger LOG = Logger.getLogger(ErrorProcessor.class.getName());

		@Override
		public void processError(final ODataRequest request, final ODataResponse response,
				final ODataServerError serverError, final ContentType responseFormat) {
			LOG.log(Level.SEVERE, serverError.getMessage(), serverError.getException());
			super.processError(request, response, serverError, responseFormat);
		}

	}

	// ----------------------------------------------------------------------------------
	public final HttpServletRequestDouble req;
	public final HttpServletResponseDouble resp;
	private static final String uriPrefix = "http://localhost:8080/Test/Olingo.svc/";

	@Deprecated
	public IntegrationTestHelper(final EntityManagerFactory localEmf, final String urlPath) throws IOException,
	ODataException {
		this(localEmf, null, urlPath, null, HttpMethod.GET);
	}

	/**
	 * Does the same as <i>IntegrationTestHelper(persistenceAdapter.getEMF(),
	 * persistenceAdapter, urlPath, null, HttpMethod.GET)</i>.
	 *
	 * @see #IntegrationTestHelper(TestGenericJPAPersistenceAdapter, String,
	 *      StringBuffer, HttpMethod)
	 */
	public IntegrationTestHelper(final TestGenericJPAPersistenceAdapter persistenceAdapter, final String urlPath)
			throws IOException, ODataException {
		this(persistenceAdapter.getEMF(), persistenceAdapter, urlPath, null, HttpMethod.GET);
	}

	/**
	 * @deprecated Use
	 *             {@link #IntegrationTestHelper(TestGenericJPAPersistenceAdapter, String, StringBuffer, HttpMethod)}
	 */
	@Deprecated
	public IntegrationTestHelper(final EntityManagerFactory localEmf, final String urlPath,
			final StringBuffer requestBody, final HttpMethod requestMethod)
					throws IOException, ODataException {
		this(localEmf, null, urlPath, requestBody, requestMethod);
	}

	public IntegrationTestHelper(final TestGenericJPAPersistenceAdapter persistenceAdapter, final String urlPath,
			final StringBuffer requestBody, final HttpMethod requestMethod) throws IOException, ODataException {
		this(persistenceAdapter.getEMF(), persistenceAdapter, urlPath, requestBody, requestMethod);
	}

	private IntegrationTestHelper(final EntityManagerFactory localEmf, final JPAAdapter persistenceAdapter,
			final String urlPath, final StringBuffer requestBody, final HttpMethod requestMethod)
					throws IOException,
					ODataException {
		super();
		this.req = new HttpServletRequestDouble(uriPrefix + urlPath, requestBody);
		req.setMethod(requestMethod);
		this.resp = new HttpServletResponseDouble();
		JPAAdapter mappingAdapter = persistenceAdapter;
		if (mappingAdapter == null) {
			if (localEmf == null) {
				throw new IllegalArgumentException("EntityManager must not be null if persistence adapter is not given");
			}
			mappingAdapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
					new JPADefaultDatabaseProcessor(), DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
		}
		final JPAODataGetHandler handler = new JPAODataGetHandler(mappingAdapter) {
			@Override
			protected Collection<Processor> collectProcessors(final HttpServletRequest request,
					final HttpServletResponse response, final EntityManager em) {
				final Collection<Processor> processors = super.collectProcessors(request, response, em);
				processors.add(new TestErrorProcessor());
				return processors;
			}
		};
		handler.process(req, resp);
	}

	public HttpServletResponseDouble getResponce() {
		return resp;
	}

	public int getStatus() {
		return resp.getStatus();
	}

	public String getRawResult() throws IOException {
		final InputStream in = resp.getInputStream();
		final StringBuilder sb = new StringBuilder();
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String read;

		while ((read = br.readLine()) != null) {
			sb.append(read);
		}
		br.close();
		return sb.toString();
	}

	public List<String> getRawBatchResult() throws IOException {
		final List<String> result = new ArrayList<String>();

		final InputStream in = resp.getInputStream();
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String read;

		while ((read = br.readLine()) != null) {
			result.add(read);
		}
		br.close();
		return result;
	}

	public ArrayNode getValues() throws JsonProcessingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode node = mapper.readTree(getRawResult());
		if (!(node.get("value") instanceof ArrayNode)) {
			fail("Wrong result type; ArrayNode expected");
		}
		final ArrayNode values = (ArrayNode) node.get("value");
		return values;
	}

	public ObjectNode getValue() throws JsonProcessingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		final JsonNode value = mapper.readTree(getRawResult());
		if (!(value instanceof ObjectNode)) {
			fail("Wrong result type; ObjectNode expected");
		}
		return (ObjectNode) value;
	}

	public void assertStatus(final int exp) throws IOException {
		assertEquals(getRawResult(), exp, getStatus());

	}

	public int getBatchResultStatus(final int i) throws IOException {
		final List<String> result = getRawBatchResult();
		int count = 0;
		for (final String resultLine : result) {
			if (resultLine.contains("HTTP/1.1")) {
				count += 1;
				if (count == i) {
					final String[] statusElements = resultLine.split(" ");
					return Integer.parseInt(statusElements[1]);
				}
			}
		}
		return 0;
	}

	public JsonNode getBatchResult(final int i) throws IOException {
		final List<String> result = getRawBatchResult();
		int count = 0;
		boolean found = false;

		for (final String resultLine : result) {
			if (resultLine.contains("HTTP/1.1")) {
				count += 1;
				if (count == i) {
					found = true;
				}
			}
			if (found && resultLine.startsWith("{")) {
				final ObjectMapper mapper = new ObjectMapper();
				return mapper.readTree(resultLine);
			}
		}
		return null;
	}

	public byte[] getBinaryResult() throws IOException {
		final byte[] result = new byte[resp.getBufferSize()];
		final InputStream in = resp.getInputStream();
		in.read(result);
		return result;
	}
}
