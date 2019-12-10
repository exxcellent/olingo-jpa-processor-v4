package org.apache.olingo.jpa.processor.core.testmodel;

import java.net.URI;

public class DatatypeConversionEntityAccess extends AbstractDatatypeConversionEntityAccess {

  private final URI serviceUrl;

  public DatatypeConversionEntityAccess(final URI serviceUrl) {
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
