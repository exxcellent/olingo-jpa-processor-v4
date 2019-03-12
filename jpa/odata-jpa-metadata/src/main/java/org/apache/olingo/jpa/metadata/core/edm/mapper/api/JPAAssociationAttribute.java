package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAAssociationAttribute extends JPAAttribute<CsdlNavigationProperty> {

	/**
	 * The same as {@link #getStructuredType()}
	 *
	 * @see #getStructuredType()
	 */
	public JPAStructuredType getTargetEntity() throws ODataJPAModelException;

}
