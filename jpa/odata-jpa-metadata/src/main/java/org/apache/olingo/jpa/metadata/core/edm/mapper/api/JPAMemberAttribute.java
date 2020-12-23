package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

public interface JPAMemberAttribute extends JPAAttribute<CsdlProperty>, JPAParameterizedElement {

  /**
   *
   * @return The column name in data base.
   */
  public String getDBFieldName();

  /**
   *
   * @return TRUE if property represents the version of data.
   */
  public boolean isEtag();
}
