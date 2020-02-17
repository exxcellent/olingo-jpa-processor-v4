package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

public class NavigationViaProperty implements /* NavigationUriInfoResourceIfc, */ NavigationIfc {

  private final NavigationIfc parent;
  private final List<UriResource> resourcepath;

  /**
   * Create a new {@link #getUriResourceParts() resource path} based on information from <i>parent</i> concatenating the
   * <i>child</i> as new tailing element in resource path.
   */
  public NavigationViaProperty(final NavigationIfc parent, final UriResourceProperty child) {
    this.parent = parent;
    resourcepath = new ArrayList<UriResource>(parent.getUriResourceParts().size() + 1);
    resourcepath.addAll(parent.getUriResourceParts());
    resourcepath.add(child);
  }

  @Override
  public UriInfoResource getFirstStep() {
    return parent.getFirstStep();
  }

  @Override
  public UriInfoResource getLastStep() {
    return parent.getLastStep();
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return resourcepath;
  }

  @Override
  public Collection<UriInfoResource> getNavigationSteps() {
    // do not add the property resource child as an step, simply ignore here
    return parent.getNavigationSteps();
  }

  @Override
  public FilterOption getFilterOption(final UriResource uriResource) {
    return parent.getFilterOption(uriResource);
  }

}
