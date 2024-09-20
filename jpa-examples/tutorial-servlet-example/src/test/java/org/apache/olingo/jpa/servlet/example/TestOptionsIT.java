package org.apache.olingo.jpa.servlet.example;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.http.DefaultHttpClientFactory;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @see https://fetch.spec.whatwg.org/#cors-preflight-request
 *
 */
public class TestOptionsIT {

  private ODataEndpointTestDefinition endpoint;

  @BeforeEach
  public void setup() {
    endpoint = new ODataEndpointTestDefinition();
  }

  @Test
  public void testNonPreflightOptionsForRessource() throws ClientProtocolException, IOException {
    try (final CloseableHttpClient httpClient = new DefaultHttpClientFactory().create(null, null);) {
      final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("10");
      final URI uri = uriBuilder.build();
      final HttpOptions options = new HttpOptions(uri);
      options.addHeader("Origin", uri.toASCIIString());
      final CloseableHttpResponse response = httpClient.execute(options);
      Assertions.assertEquals(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), response.getStatusLine()
          .getStatusCode());
    }
  }

  @Test
  public void testPreflightOptionsForRessource() throws ClientProtocolException, IOException {
    try (final CloseableHttpClient httpClient = new DefaultHttpClientFactory().create(null, null);) {
      final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("10");
      final URI uri = uriBuilder.build();
      final HttpOptions options = new HttpOptions(uri);
      options.addHeader("Origin", uri.toASCIIString());
      options.addHeader("Access-Control-Request-Method", HttpMethod.GET.name());
      final CloseableHttpResponse response = httpClient.execute(options);
      // the example servlet must have overwritten the default and must accept cross origin requests
      Assertions.assertEquals(uri.toASCIIString(), response.getFirstHeader("Access-Control-Allow-Origin").getValue());
    }
  }

}
