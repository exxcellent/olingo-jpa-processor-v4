package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.data.ResWrap;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.serialization.ClientODataDeserializer;
import org.apache.olingo.client.api.serialization.ODataDeserializerException;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ConfigurationImpl;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.client.core.uri.URIBuilderImpl;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
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

public class ServerCallSimulator {

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
  private final Logger LOG = Logger.getLogger(ServerCallSimulator.class.getName());

  public final HttpServletRequestDouble req;
  public HttpServletResponseDouble resp = null;
  private final JPAAdapter persistenceAdapter;
  static final String SERVLET_PATH = "/Olingo.svc";
  static final String uriPrefix = "http://localhost:8080/Test" + SERVLET_PATH + "/";
  private boolean executed = false;
  private SecurityInceptor securityInceptor = null;

  /**
   *
   * @see #IntegrationTestHelper(JPAAdapter, String,
   *      StringBuffer, HttpMethod)
   * @deprecated Use {@link #IntegrationTestHelper(JPAAdapter, URIBuilder)} instead to improve client side behaviour of
   *             tests.
   */
  @Deprecated
  public ServerCallSimulator(final JPAAdapter persistenceAdapter, final String urlPath)
      throws IOException, ODataException {
    this(persistenceAdapter, urlPath, null, HttpMethod.GET);
  }

  /**
   * @deprecated Use {@link #IntegrationTestHelper(JPAAdapter, URIBuilder, StringBuffer, HttpMethod)} instead to improve
   *             client side behaviour of tests.
   */
  @Deprecated
  public ServerCallSimulator(final JPAAdapter persistenceAdapter,
      final String urlPath, final StringBuffer requestBody, final HttpMethod requestMethod)
          throws IOException,
          ODataException {
    this(persistenceAdapter, wrapUrl(urlPath), requestBody, requestMethod);

  }

  private static URIBuilder wrapUrl(final String urlPath) {
    return new URIBuilderImpl(new ConfigurationImpl(), ServerCallSimulator.uriPrefix + urlPath.replace(" ", "%20"));
  }

  /**
   * @see #IntegrationTestHelper(JPAAdapter, URIBuilder, StringBuffer, HttpMethod)
   */
  public ServerCallSimulator(final JPAAdapter persistenceAdapter, final URIBuilder uriBuilder)
      throws IOException, ODataException {
    this(persistenceAdapter, uriBuilder, null, HttpMethod.GET);
  }

  /**
   * @see TestBase#newUriBuilder()
   */
  public ServerCallSimulator(final JPAAdapter persistenceAdapter,
      final URIBuilder uriBuilder, final StringBuffer requestBody, final HttpMethod requestMethod)
          throws IOException,
          ODataException {
    super();
    this.req = new HttpServletRequestDouble(uriBuilder, requestBody);
    this.req.setMethod(requestMethod);
    this.persistenceAdapter = persistenceAdapter;
    if (persistenceAdapter == null) {
      throw new IllegalArgumentException("JPAAdapter required");
    }
  }

  public void setRequestContentType(final String type) {
    req.setContentType(type);
  }

  /**
   * set the content type in request to define the format of response.
   */
  public void setRequestedResponseContentType(final String type) {
    req.setHeader(HttpHeader.ACCEPT, type);
  }

  public void setSecurityInceptor(final SecurityInceptor securityInceptor) {
    this.securityInceptor = securityInceptor;
  }

  public void setUser(final Principal principal) {
    req.setUserPrincipal(principal);
  }

  public void execute(final int status) throws ODataException, UnsupportedEncodingException {
    this.resp = new HttpServletResponseDouble();
    final JPAODataServletHandler handler = new JPAODataServletHandler(persistenceAdapter) {

      @Override
      protected Collection<Processor> collectProcessors(final HttpServletRequest request,
          final HttpServletResponse response, final EntityManager em) {
        final Collection<Processor> processors = super.collectProcessors(request, response, em);
        processors.add(new TestErrorProcessor());
        return processors;
      }
    };
    if (securityInceptor != null) {
      handler.setSecurityInceptor(securityInceptor);
    }
    LOG.info("Execute " + URLDecoder.decode(req.getRequestTestExecutionURI().toString(), "UTF-8") + "...");
    handler.process(req, resp);
    executed = true;
    assertEquals(parseResponse(), status, getStatus());
  }

  private String parseResponse() {
    try {
      return getRawResult();
    } catch (final IOException e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  public int getStatus() {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }
    return resp.getStatus();
  }

  public String getRawResult() throws IOException {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }
    final InputStream in = resp.getInputStream();
    final StringBuilder sb = new StringBuilder();
    final BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
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
    final BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    String read;

    while ((read = br.readLine()) != null) {
      result.add(read);
    }
    br.close();
    return result;
  }

  /**
   * Helper method to remove NullNode's from mapper result, because some assert's
   * are not aware of a NullNode instead of <code>null</code>.
   */
  @SuppressWarnings("unused")
  private static void stripNulls(final JsonNode node) {
    if (node == null) {
      return;
    }
    final Iterator<JsonNode> it = node.iterator();
    while (it.hasNext()) {
      final JsonNode child = it.next();
      if (child.isNull()) {
        it.remove();
      } else {
        stripNulls(child);
      }
    }
  }

  public ArrayNode getJsonObjectValues() throws JsonProcessingException, IOException, ODataDeserializerException {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode node = mapper.readTree(getRawResult());
    if (!(node.get("value") instanceof ArrayNode)) {
      fail("Wrong result type; ArrayNode expected");
    }
    final ArrayNode values = (ArrayNode) node.get("value");
    return values;
  }

  public ObjectNode getJsonObjectValue() throws JsonProcessingException, IOException, ODataDeserializerException {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode value = mapper.readTree(getRawResult());
    if (!(value instanceof ObjectNode)) {
      fail("Wrong result type; ObjectNode expected");
    }
    return (ObjectNode) value;
  }

  public ClientEntitySet getOlingoEntityCollectionValues() throws ODataDeserializerException {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }

    final ODataClient client = ODataClientFactory.getClient();
    final ClientODataDeserializer deserialzer = client.getDeserializer(ContentType.parse(resp.getContentType()));
    final ResWrap<EntityCollection> resources = deserialzer.toEntitySet(resp.getInputStream());
    return client.getBinder().getODataEntitySet(resources);
  }

  /**
   * Entity object parsed by Olingo.
   */
  public ClientEntity getOlingoEntityValue() throws ODataDeserializerException {
    if (!executed) {
      throw new IllegalStateException("call execute() before");
    }

    final ODataClient client = ODataClientFactory.getClient();
    final ClientODataDeserializer deserialzer = client.getDeserializer(ContentType.parse(resp.getContentType()));
    final ResWrap<org.apache.olingo.commons.api.data.Entity> resource = deserialzer.toEntity(resp.getInputStream());
    return client.getBinder().getODataEntity(resource);
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
