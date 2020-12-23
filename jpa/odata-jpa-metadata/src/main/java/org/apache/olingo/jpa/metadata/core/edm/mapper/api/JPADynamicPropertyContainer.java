package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

public interface JPADynamicPropertyContainer {
  /**
   *
   * @return Meta data describing the content (dynamic properties) of the container (like entries in a
   * {@link java.util.Map Map}).
   */
  public JPAParameterizedElement getDynamicPropertyDescription();
}
