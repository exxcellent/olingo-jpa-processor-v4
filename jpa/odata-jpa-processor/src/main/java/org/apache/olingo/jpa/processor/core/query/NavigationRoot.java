package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

/**
 * Simply a wrapper class to implement the interface
 *
 * @author Ralf Zozmann
 *
 */
public class NavigationRoot implements NavigationIfc {

  private final UriInfoResource context;

  public NavigationRoot(final UriInfoResource context) {
    this.context = context;
  }

  @Override
  public Collection<UriInfoResource> getNavigationSteps() {
    return Collections.singletonList(context);
  }

  @Override
  public UriInfoResource getFirstStep() {
    return context;
  }

  @Override
  public UriInfoResource getLastStep() {
    return context;
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return context.getUriResourceParts();
  }

  @Override
  public FilterOption getFilterOption(final UriResource uriResource) {
    final List<UriResource> parts = context.getUriResourceParts();
    if (parts.get(parts.size() - 1) != uriResource) {
      return null;
    }
    return context.getFilterOption();
  }
}
