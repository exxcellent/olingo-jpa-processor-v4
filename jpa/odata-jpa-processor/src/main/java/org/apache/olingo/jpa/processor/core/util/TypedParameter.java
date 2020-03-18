package org.apache.olingo.jpa.processor.core.util;

public final class TypedParameter {
  private final Class<?> type;
  private final Object value;

  public <T> TypedParameter(final Class<T> type, final T value) {
    this.type = type;
    this.value = value;
  }

  public Class<?> getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "TypedParameter [" + (type != null ? "type=" + type + ", " : "") + (value != null ? "value=" + value : "")
        + "]";
  }

}
