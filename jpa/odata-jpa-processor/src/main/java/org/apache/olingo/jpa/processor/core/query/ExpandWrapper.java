package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.DeltaTokenOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.FormatOption;
import org.apache.olingo.server.api.uri.queryoption.IdOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SearchOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.SkipTokenOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.core.uri.UriResourceNavigationPropertyImpl;

/**
 * Wrapper to handle $filter, $expand and resource parts in a proper way, because for an {@link ExpandItem} the options
 * are not on the expected level.
 *
 */
class ExpandWrapper implements UriInfoResource {

  private final ExpandItem item;
  private final List<UriResource> resourceParts;

  ExpandWrapper(final ExpandItem item) {
    super();
    this.item = item;
    assert item.getResourcePath() != null;
    resourceParts = new LinkedList<UriResource>(item.getResourcePath().getUriResourceParts());
  }

  /**
   * Called for * $expand without separate {@link ExpandItem#getResourcePath() resource parts}.
   */
  public ExpandWrapper(final ExpandItem item, final EdmNavigationProperty property) {
    super();
    this.item = item;
    assert item.getResourcePath() == null;
    assert property != null;
    final UriResourceNavigationPropertyImpl fake = new UriResourceNavigationPropertyImpl(property);
    resourceParts = new ArrayList<UriResource>(1);
    // simulate a property navigation for $expand without resources
    resourceParts.add(fake);
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return resourceParts;
  }

  @Override
  public ApplyOption getApplyOption() {
    return item.getApplyOption();
  }

  @Override
  public CountOption getCountOption() {
    return item.getCountOption();
  }

  @Override
  public List<CustomQueryOption> getCustomQueryOptions() {
    return null;
  }

  @Override
  public DeltaTokenOption getDeltaTokenOption() {
    return null;
  }

  @Override
  public ExpandOption getExpandOption() {
    return item.getExpandOption();
  }

  @Override
  public FilterOption getFilterOption() {
    return item.getFilterOption();
  }

  @Override
  public FormatOption getFormatOption() {
    return null;
  }

  @Override
  public IdOption getIdOption() {
    return null;
  }

  @Override
  public OrderByOption getOrderByOption() {
    return item.getOrderByOption();
  }

  @Override
  public SearchOption getSearchOption() {
    return item.getSearchOption();
  }

  @Override
  public SelectOption getSelectOption() {
    return item.getSelectOption();
  }

  @Override
  public SkipOption getSkipOption() {
    return item.getSkipOption();
  }

  @Override
  public SkipTokenOption getSkipTokenOption() {
    return null;
  }

  @Override
  public TopOption getTopOption() {
    return item.getTopOption();
  }

  @Override
  public String getValueForAlias(final String alias) {
    return null;
  }
}