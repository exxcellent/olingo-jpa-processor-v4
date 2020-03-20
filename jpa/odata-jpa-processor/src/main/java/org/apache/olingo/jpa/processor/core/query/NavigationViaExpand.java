package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

// TODO In case of second level $expand expandItem.getResourcePath() returns an empty UriInfoResource => Bug or
// Feature?
/**
 * Helper class to wrap an expand item as navigation step with {@link UriInfoResource}.
 *
 */
public class NavigationViaExpand implements NavigationIfc {

  private final ExpandWrapper expandWrapper;
  private final List<UriResource> mergedExpandResourcePath;
  private final NavigationIfc parent;
  private final LinkedList<UriInfoResource> steps;

  public NavigationViaExpand(final NavigationIfc uriResource, final ExpandItem item)
      throws ODataApplicationException {
    super();
    this.expandWrapper = new ExpandWrapper(item);
    mergedExpandResourcePath = new LinkedList<UriResource>();
    mergedExpandResourcePath.addAll(uriResource.getUriResourceParts());
    mergedExpandResourcePath.addAll(expandWrapper.getUriResourceParts());
    this.parent = uriResource;
    this.steps = new LinkedList<UriInfoResource>(parent.getNavigationSteps());
    this.steps.add(expandWrapper);
  }

  /**
   * Called for * $expand without {@link ExpandItem#getResourcePath() resource parts}.
   */
  public NavigationViaExpand(final NavigationIfc uriResource, final ExpandItem item,
      final EdmNavigationProperty property) {
    super();
    this.expandWrapper = new ExpandWrapper(item, property);
    mergedExpandResourcePath = new LinkedList<UriResource>();
    mergedExpandResourcePath.addAll(uriResource.getUriResourceParts());
    mergedExpandResourcePath.addAll(expandWrapper.getUriResourceParts());
    this.parent = uriResource;
    this.steps = new LinkedList<UriInfoResource>(parent.getNavigationSteps());
    this.steps.add(expandWrapper);
  }

  @Override
  public UriInfoResource getFirstStep() {
    return parent.getFirstStep();
  }

  @Override
  public UriInfoResource getLastStep() {
    return steps.getLast();
  }

  @Override
  public Collection<UriInfoResource> getNavigationSteps() {
    return steps;
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return mergedExpandResourcePath;
  }

  @Override
  public FilterOption getFilterOption(final UriResource uriResource) {
    final List<UriResource> parts = expandWrapper.getUriResourceParts();
    if (parts.get(parts.size() - 1) != uriResource) {
      return parent.getFilterOption(uriResource);
    }
    return expandWrapper.getFilterOption();
  }
}