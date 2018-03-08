package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

/**
 * 
 * @author Ralf Zozmann
 *
 */
public interface JPAAction extends JPAElement {

	/**
	 * The parameter name used to define a 'bound resource parameter' on demand for bound actions.
	 */
	public static final String BOUND_ACTION_ENTITY_PARAMETER_NAME = "entity";

	/**
	 * 
	 * @return List of import parameter
	 */
	public List<JPAOperationParameter> getParameters();

	/**
	 * 
	 * @return The return or result parameter of the function
	 */
	public JPAOperationResultParameter getResultParameter();
}
