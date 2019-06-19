package org.apache.olingo.jpa.processor.core.query;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.server.api.uri.UriResourcePartTyped;

public class JPANavigationPropertyInfo {
  private final UriResourcePartTyped navigationTarget;
  private final JPAAssociationPath associationPath;

  public JPANavigationPropertyInfo(final UriResourcePartTyped uriResiource, final JPAAssociationPath associationPath) {
    super();
    this.navigationTarget = uriResiource;
    this.associationPath = associationPath;
  }

  public UriResourcePartTyped getUriResiource() {
    return navigationTarget;
  }

  public JPAAssociationPath getAssociationPath() {
    return associationPath;
  }

}
