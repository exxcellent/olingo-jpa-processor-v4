package org.apache.olingo.jpa.client.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.impl.io.DefaultClassicHttpResponseFactory;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.olingo.client.api.http.ODataHttpClient;
import org.apache.olingo.client.core.ODataClientImpl;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.HttpServletRequestDouble;
import org.apache.olingo.jpa.processor.core.util.HttpServletResponseDouble;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestErrorProcessor;
import org.apache.olingo.server.api.processor.Processor;

public final class LocalTestODataClient extends ODataClientImpl {

  private static class MockDefaultHttpClientFactory extends DefaultHttpClientFactory {

    private final JPAAdapter persistenceAdapter;

    public MockDefaultHttpClientFactory(final JPAAdapter persistenceAdapter) {
      this.persistenceAdapter = persistenceAdapter;
    }

    @Override
    public ODataHttpClient create(final HttpMethod method, final URI uri) {
      CloseableHttpClient client = new CloseableHttpClient() {
        @Override
        protected CloseableHttpResponse doExecute(HttpHost target, ClassicHttpRequest request, HttpContext context)
            throws IOException {
          try {
            String body = null;
            final HttpEntity entity = request.getEntity();
            if(entity != null) {
              final InputStream in = entity.getContent();
              final StringBuilder sb = new StringBuilder();
              final BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
              String read;
              while ((read = br.readLine()) != null) {
                sb.append(read);
              }
              br.close();
              body = sb.toString();
            }
            final HttpServletRequestDouble requestServlet = new HttpServletRequestDouble(uri, body);
            requestServlet.setMethod(method);
            final HttpServletResponseDouble responseServlet = new HttpServletResponseDouble();
            final JPAODataServletHandler handler = new JPAODataServletHandler(persistenceAdapter) {

              @Override
              protected Collection<Processor> collectProcessors(final JPAODataRequestContext requestContext) {
                final Collection<Processor> processors = super.collectProcessors(requestContext);
                processors.add(new TestErrorProcessor());
                return processors;
              }
            };
            Logger.getLogger(IntegrationTestHelper.class.getName()).info("Execute " + uri.toString() + "...");
            handler.process(requestServlet, responseServlet);
            final ClassicHttpResponse responseHttp = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(
                responseServlet.getStatus());
            responseHttp.setEntity(new InputStreamEntity(responseServlet.getInputStream(), responseServlet
                .getBufferSize(), ContentType.APPLICATION_JSON));
            for (final String header : responseServlet.getHeaderNames()) {
              final Collection<String> values = responseServlet.getHeaders(header);
              for (final String value : values) {
                responseHttp.addHeader(header, value);
              }
            }
            return CloseableHttpResponse.adapt(responseHttp);
          } catch (final ODataException e) {
            throw new IOException("Fake call to backend failed", e);
          }
        }
        
        @Override
        public void close() throws IOException {
          close(CloseMode.GRACEFUL);
        }
        
        @Override
        public void close(CloseMode closeMode) {
          //do nothing
        }
      };
      return DefaultHttpClientFactory.wrapHC5Client(client);
    }

  }

  public LocalTestODataClient(final JPAAdapter persistenceAdapter) {
    super();
    getConfiguration().setGzipCompression(false);
    getConfiguration().setHttpClientFactory(new MockDefaultHttpClientFactory(persistenceAdapter));
  }

}
