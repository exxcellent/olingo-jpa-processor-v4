package org.apache.olingo.jpa.processor.core.query;

import java.util.ArrayList;
import java.util.List;

import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.CustomQueryOption;
import org.apache.olingo.server.api.uri.queryoption.DeltaTokenOption;
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

public class NavigationUriResource implements UriInfoResource {

  private final UriInfoResource parent;
  private final List<UriResource> resourcepath;

  /**
   * Create a new {@link #getUriResourceParts() resource path} based on information from <i>parent</i> concatenating the
   * <i>child</i> as new tailing element in resource path.
   */
  public NavigationUriResource(final UriInfoResource parent, final UriResource child) {
    this.parent = parent;
    resourcepath = new ArrayList<UriResource>(parent.getUriResourceParts().size() + 1);
    resourcepath.addAll(parent.getUriResourceParts());
    resourcepath.add(child);
  }

  @Override
  public List<CustomQueryOption> getCustomQueryOptions() {
    return parent.getCustomQueryOptions();
  }

  @Override
  public ExpandOption getExpandOption() {
    return parent.getExpandOption();
  }

  @Override
  public FilterOption getFilterOption() {
    return parent.getFilterOption();
  }

  @Override
  public FormatOption getFormatOption() {
    return parent.getFormatOption();
  }

  @Override
  public IdOption getIdOption() {
    return parent.getIdOption();
  }

  @Override
  public CountOption getCountOption() {
    return parent.getCountOption();
  }

  @Override
  public DeltaTokenOption getDeltaTokenOption() {
    return parent.getDeltaTokenOption();
  }

  @Override
  public OrderByOption getOrderByOption() {
    return parent.getOrderByOption();
  }

  @Override
  public SearchOption getSearchOption() {
    return parent.getSearchOption();
  }

  @Override
  public SelectOption getSelectOption() {
    return parent.getSelectOption();
  }

  @Override
  public SkipOption getSkipOption() {
    return parent.getSkipOption();
  }

  @Override
  public SkipTokenOption getSkipTokenOption() {
    return parent.getSkipTokenOption();
  }

  @Override
  public TopOption getTopOption() {
    return parent.getTopOption();
  }

  @Override
  public ApplyOption getApplyOption() {
    return parent.getApplyOption();
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return resourcepath;
  }

  @Override
  public String getValueForAlias(final String alias) {
    return parent.getValueForAlias(alias);
  }

}
