package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.lang.reflect.AnnotatedElement;

public interface JPADescribedElement {
  /**
   *
   * @return The direct type of simple attributes (or parameter or return value)
   * or the element type if the attribute is a collection.
   *
   * @see #getCollectionType()
   */
  public Class<?> getType();

  /**
   *
   * @return The member (field/method) or parameter represented by this meta data object or <code>null</code> if
   * annotations are not supported.
   */
  public AnnotatedElement getAnnotatedElement();
}
