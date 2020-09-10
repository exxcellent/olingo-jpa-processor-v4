package org.apache.olingo.jpa.metadata.core.edm.mapper.extention;

import org.apache.olingo.commons.api.edm.provider.CsdlOnDelete;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;

public interface IntermediateNavigationPropertyAccess extends JPAElement {
  /**
   * @see http://docs.oasis-open.org/odata/odata/v4.0/errata03/os/complete/part3-csdl/odata-v4.0-errata03-os-part3-csdl-complete.html#_Toc453752546
   */
  public void setOnDelete(CsdlOnDelete onDelete);
}
