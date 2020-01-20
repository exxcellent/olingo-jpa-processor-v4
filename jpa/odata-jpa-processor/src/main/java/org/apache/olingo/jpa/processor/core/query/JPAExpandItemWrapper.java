package org.apache.olingo.jpa.processor.core.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.ODataApplicationException;
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

// TODO In case of second level $expand expandItem.getResourcePath() returns an empty UriInfoResource => Bug or
// Feature?
/**
 * Helper class to fake an expand item as {@link UriInfoResource}.
 *
 */
public class JPAExpandItemWrapper implements UriInfoResource {
  private final ExpandItem item;
  private final List<UriResource> starExpandResourcePathFake;

  public JPAExpandItemWrapper(final List<UriResource> startResourceList, final ExpandItem item)
      throws ODataApplicationException {
    super();
    this.item = item;
    assert item.getResourcePath() != null;
    starExpandResourcePathFake = new LinkedList<UriResource>();
    starExpandResourcePathFake.addAll(startResourceList);
    starExpandResourcePathFake.addAll(item.getResourcePath().getUriResourceParts());
  }

  /**
   * Called for * $expand without resource parts.
   */
  public JPAExpandItemWrapper(final List<UriResource> startResourceList, final ExpandItem item,
      final EdmNavigationProperty property) {
    super();
    assert property != null;
    this.item = item;
    assert item.getResourcePath() == null;
    // simulate a resource navigation for $expand
    final UriResourceNavigationPropertyImpl fake = new UriResourceNavigationPropertyImpl(property);
    starExpandResourcePathFake = new LinkedList<UriResource>();
    starExpandResourcePathFake.addAll(startResourceList);
    starExpandResourcePathFake.add(fake);
  }

  @Override
  public List<CustomQueryOption> getCustomQueryOptions() {
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
  public CountOption getCountOption() {
    return item.getCountOption();
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
  public List<UriResource> getUriResourceParts() {
    return starExpandResourcePathFake;
  }

  @Override
  public String getValueForAlias(final String alias) {
    return null;
  }

  @Override
  public ApplyOption getApplyOption() {
    return item.getApplyOption();
  }

  @Override
  public DeltaTokenOption getDeltaTokenOption() {
    return null;
  }
}