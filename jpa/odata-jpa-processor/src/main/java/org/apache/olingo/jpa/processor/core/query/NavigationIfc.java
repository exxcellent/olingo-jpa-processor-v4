package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.List;

import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

/**
 * A navigation consist of multiple parts/steps. Steps are separated by an nested/deeper $expand navigation. Every step
 * can have own $filter options and may consist of relationship navigation.
 *
 */
public interface NavigationIfc {

  /**
   *
   * @return Collections with at least 1 element, but more in case of $expand resolution
   */
  public Collection<UriInfoResource> getNavigationSteps();

  public UriInfoResource getFirstStep();

  /**
   *
   * @return Normally this will be the {@link UriInfoResource} identifying the target of current $filter etc. for an
   * query.
   */
  public UriInfoResource getLastStep();

  /**
   *
   * @return Merged list of {@link UriResource path elements} of all {@link UriInfoResource steps}.
   */
  public List<UriResource> getUriResourceParts();

  /**
   * For an {@link org.apache.olingo.server.api.uri.queryoption.ExpandItem ExpandItem} the $filter is NOT available at
   * {@link UriResource} level, but on wrapping expand item self. In all other cases the filter is available for the
   * last element in {@link UriInfoResource#getUriResourceParts()}.
   *
   * @param uriResource The uri resource to find the aassigned filter option.
   * @return The filter option or <code>null</code>
   */
  public FilterOption getFilterOption(UriResource uriResource);
}

