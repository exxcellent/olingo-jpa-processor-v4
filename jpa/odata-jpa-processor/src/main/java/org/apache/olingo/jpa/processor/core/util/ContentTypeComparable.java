package org.apache.olingo.jpa.processor.core.util;

import java.util.Objects;

import org.apache.olingo.commons.api.format.ContentType;

/**
 * Helper class to make {@link ContentType} comparable.
 * @author Ralf Zozmann
 *
 */
public final class ContentTypeComparable implements Comparable<ContentTypeComparable> {

  private final ContentType contentType;

  public ContentTypeComparable(final ContentType object) {
    this.contentType = object;
    if (object == null) {
      throw new IllegalArgumentException("content type required");
    }
  }

  @Override
  public int compareTo(final ContentTypeComparable o) {
    if (contentType.isCompatible(o.contentType)) {
      return 0;
    }
    return contentType.toContentTypeString().compareTo(o.contentType.toContentTypeString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentType);
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
    final ContentTypeComparable other = (ContentTypeComparable) obj;
    return Objects.equals(contentType, other.contentType);
  }

  public ContentType getContentType() {
    return contentType;
  }

  @Override
  public String toString() {
    return "ContentTypeComparable [" + (contentType != null ? "contentType=" + contentType : "") + "]";
  }

}
