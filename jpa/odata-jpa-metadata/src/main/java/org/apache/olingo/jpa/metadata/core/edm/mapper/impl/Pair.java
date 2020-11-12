package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.Objects;

public final class Pair<X, Y> {
  final X left;
  final Y right;

  public Pair(final X left, final Y right) {
    this.left = left;
    this.right = right;
  }

  public X getLeft() {
    return left;
  }

  public Y getRight() {
    return right;
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
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
    @SuppressWarnings("rawtypes")
    final Pair other = (Pair) obj;
    return Objects.equals(left, other.left) && Objects.equals(right, other.right);
  }

}