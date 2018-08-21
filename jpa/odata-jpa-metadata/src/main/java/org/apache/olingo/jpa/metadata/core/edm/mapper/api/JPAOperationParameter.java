package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public interface JPAOperationParameter {

	public enum ParameterKind {
		/**
		 * Parameter must be set by server side (inject)
		 */
		Inject,

		/**
		 * Parameter is visible on OData api and must be set by clients
		 */
		OData,

		/**
		 * Parameter is of unknown kind.
		 */
		Invalid;
	}

	public Class<?> getType();

	public String getName();

	public Integer getMaxLength();

	public Integer getPrecision();

	public Integer getScale();

	public FullQualifiedName getTypeFQN() throws ODataJPAModelException;

	public ParameterKind getParameterKind();
}
