package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

//TODO remove extension
public interface JPAAssociationAttribute extends JPAAttribute {

	/**
	 * The same as {@link #getStructuredType()}
	 *
	 * @see #getStructuredType()
	 */
	public JPAStructuredType getTargetEntity() throws ODataJPAModelException;

}
