package org.apache.olingo.jpa.processor.core.util;

public final class DependencyMapping {
  private final Class<?> type;
  private final Object value;

  public DependencyMapping(final Class<?> type, final Object value) {
    this.type = type;
    this.value = value;
  }

  public Class<?> getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }
}
