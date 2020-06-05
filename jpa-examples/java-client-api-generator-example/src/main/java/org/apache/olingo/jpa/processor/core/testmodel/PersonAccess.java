package org.apache.olingo.jpa.processor.core.testmodel;

import java.net.URI;

/**
 * Demo class to show usage of client side api.
 *
 */
public class PersonAccess extends PersonAbstractAccess {

  private final URI serviceUrl;

  public PersonAccess(final URI serviceUrl) {
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
