package org.apache.olingo.jpa.metadata.core.edm;

public enum NamingStrategy {
  /**
   * The default strategy implementing the historical behavior by upper case the first character of attribute names.
   */
  UpperCamelCase,

  /**
   * Use the name as defined in the JPA model. Attribute names are not changed...
   */
  AsIs;
}
