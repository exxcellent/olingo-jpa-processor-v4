package org.apache.olingo.jpa.metadata.core.edm.mapper.extention;

import org.apache.olingo.commons.api.edm.provider.CsdlOnDelete;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;

public interface IntermediateNavigationPropertyAccess extends JPAElement {
	public void setOnDelete(CsdlOnDelete onDelete);
}
