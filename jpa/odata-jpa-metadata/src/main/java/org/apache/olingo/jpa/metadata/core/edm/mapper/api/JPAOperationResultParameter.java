package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;

public interface JPAOperationResultParameter extends JPAParameterizedElement {

  public FullQualifiedName getTypeFQN();

  /**
   *
   * @return The value type or <code>null</code> if undefined
   */
  public ValueType getResultValueType();
}
