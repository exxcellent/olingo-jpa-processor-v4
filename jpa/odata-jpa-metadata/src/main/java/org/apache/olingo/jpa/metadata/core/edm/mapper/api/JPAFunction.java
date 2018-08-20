package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

public interface JPAFunction extends JPAElement {
	/**
	 *
	 * @return Name of the function on the database
	 */
	public String getDBName();

	/**
	 *
	 * @return List of import parameter
	 */
	public List<JPAOperationParameter> getParameter();

	/**
	 *
	 * @return The return or result parameter of the function
	 */
	public JPAOperationResultParameter getResultParameter();

	public boolean isBound();
}
