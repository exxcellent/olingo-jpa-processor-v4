package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;
import java.util.Objects;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElementCollectionPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;

public final class JPAElementCollectionPathImpl implements JPAElementCollectionPath {

  private final JPASelector selector;

  public JPAElementCollectionPathImpl(final JPASelector selector) {
    this.selector = selector;
  }

  @Override
  public String getAlias() {
    return selector.getAlias();
  }

  @Override
  public List<JPAAttribute<?>> getPathElements() {
    return selector.getPathElements();
  }

  @Override
  public JPAAttribute<?> getLeaf() {
    return selector.getLeaf();
  }

  @Override
  public int compareTo(final JPASelector o) {
    return selector.compareTo(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(selector);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final JPAElementCollectionPathImpl other = (JPAElementCollectionPathImpl) obj;
    return Objects.equals(selector, other.selector);
  }

}
