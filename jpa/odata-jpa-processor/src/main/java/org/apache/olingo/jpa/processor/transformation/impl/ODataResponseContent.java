package org.apache.olingo.jpa.processor.transformation.impl;

import java.io.InputStream;

public class ODataResponseContent {
  public enum ContentState {
    /**
     * Collection without content.
     */
    EMPTY_COLLECTION,
    /**
     * Content represents a single (entity/complex/property) instance or a filled collection.
     */
    PRESENT,
    /**
     * no single entity/complex/property or collection of entity/complex/property is present (may result in
     * {@link org.apache.olingo.commons.api.http.HttpStatusCode.NO_CONTENT
     * NO_CONTENT}).
     */
    NULL;
  }

  private final ContentState contentState;
  private final InputStream content;

  public ODataResponseContent(final ContentState contentState, final InputStream content) {
    this.contentState = contentState;
    this.content = content;
  }

  public ContentState getContentState() {
    return contentState;
  }

  public InputStream getContent() {
    return content;
  }
}
