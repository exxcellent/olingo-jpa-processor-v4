package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import org.apache.olingo.jpa.util.Pair;

final class Triple<Left, Middle, Right> extends Pair<Left, Right> {
  final private Middle middle;

  public Triple(final Left left, final Middle middle, final Right right) {
    super(left, right);
    this.middle = middle;
  }

  public Middle getMiddle() {
    return middle;
  }
}
