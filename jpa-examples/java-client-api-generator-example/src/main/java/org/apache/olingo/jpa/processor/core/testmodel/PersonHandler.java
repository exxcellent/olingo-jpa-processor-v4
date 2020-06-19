package org.apache.olingo.jpa.processor.core.testmodel;

import java.net.URI;

/**
 * Demo class to show usage of client side api.
 *
 */
public class PersonHandler extends PersonAbstractHandler {

  private final URI serviceUrl;

  public PersonHandler(final URI serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  @Override
  protected String determineAuthorizationHeaderValue() {
    return null;
  }

  @Override
  protected URI getServiceRootUrl() {
    return serviceUrl;
  }

}
