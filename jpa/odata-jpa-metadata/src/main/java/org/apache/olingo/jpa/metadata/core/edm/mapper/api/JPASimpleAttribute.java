package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.edm.provider.CsdlProperty;

public interface JPASimpleAttribute extends JPAAttribute<CsdlProperty>, JPATypedElement {
	/**
	 *
	 * @return The column name in data base.
	 */
	public String getDBFieldName();
}
