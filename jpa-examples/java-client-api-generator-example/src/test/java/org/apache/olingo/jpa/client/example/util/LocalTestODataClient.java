package org.apache.olingo.jpa.client.example.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
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

@SuppressWarnings("deprecation")
public final class LocalTestODataClient extends ODataClientImpl {

  private static class MockDefaultHttpClientFactory extends DefaultHttpClientFactory {

    private final JPAAdapter persistenceAdapter;

    public MockDefaultHttpClientFactory(final JPAAdapter persistenceAdapter) {
      this.persistenceAdapter = persistenceAdapter;
    }

    @Override
    public DefaultHttpClient create(final HttpMethod method, final URI uri) {
      final DefaultHttpClient client = new DefaultHttpClient() {
        @Override
        protected RequestDirector createClientRequestDirector(final HttpRequestExecutor requestExec,
            final ClientConnectionManager conman, final ConnectionReuseStrategy reustrat,
            final ConnectionKeepAliveStrategy kastrat,
            final HttpRoutePlanner rouplan, final HttpProcessor httpProcessor,
            final HttpRequestRetryHandler retryHandler,
            final RedirectStrategy redirectStrategy, final AuthenticationStrategy targetAuthStrategy,
            final AuthenticationStrategy proxyAuthStrategy, final UserTokenHandler userTokenHandler,
            final HttpParams params) {
          return new DefaultRequestDirector(
              LogFactory.getLog(getClass()),
              requestExec,
              conman,
              reustrat,
              kastrat,
              rouplan,
              httpProcessor,
              retryHandler,
              redirectStrategy,
              targetAuthStrategy,
              proxyAuthStrategy,
              userTokenHandler,
              params) {
            @Override
            public HttpResponse execute(final HttpHost targetHost, final HttpRequest requestHttp,
                final HttpContext context)
                    throws HttpException, IOException {
              try {
                String body = null;
                if (requestHttp instanceof HttpEntityEnclosingRequest) {
                  final HttpEntity entity = ((HttpEntityEnclosingRequest) requestHttp).getEntity();
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
                final HttpResponse responseHttp = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(
                    HttpVersion.HTTP_1_1,
                    responseServlet.getStatus(), context);
                responseHttp.setEntity(new InputStreamEntity(responseServlet.getInputStream(), responseServlet
                    .getBufferSize()));
                for (final String header : responseServlet.getHeaderNames()) {
                  final Collection<String> values = responseServlet.getHeaders(header);
                  for (final String value : values) {
                    responseHttp.addHeader(header, value);
                  }
                }
                return responseHttp;
              } catch (final ODataException e) {
                throw new HttpException("Fake call to backend failed", e);
              }
              //              return super.execute(targetHost, request, context);
            }
          };
        }
      };
      client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, USER_AGENT);
      return client;
    }

  }

  public LocalTestODataClient(final JPAAdapter persistenceAdapter) {
    super();
    getConfiguration().setGzipCompression(false);
    getConfiguration().setHttpClientFactory(new MockDefaultHttpClientFactory(persistenceAdapter));
  }

}
