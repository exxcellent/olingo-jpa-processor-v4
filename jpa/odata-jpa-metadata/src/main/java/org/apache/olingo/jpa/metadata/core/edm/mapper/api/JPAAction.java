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
	 * This is the list of java method parameters, without the the 'entity'
	 * parameter for bound actions, but with all OData and @Inject parameters.
	 *
	 * @return List of import parameter
	 */
	public List<JPAOperationParameter> getParameters();

	/**
	 *
	 * @return The return/result parameter of the action or <code>null</code> for
	 *         'void'.
	 */
	public JPAOperationResultParameter getResultParameter();

	public boolean isBound();

}
