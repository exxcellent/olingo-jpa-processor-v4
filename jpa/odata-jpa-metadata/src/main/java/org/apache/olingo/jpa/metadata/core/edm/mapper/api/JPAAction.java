package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.util.List;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

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
	 * parameter for bound actions.
	 * 
	 * @return List of import parameter
	 */
	public List<JPAOperationParameter> getParameters();

	/**
	 *
	 * @return The return or result parameter of the function
	 */
	public JPAOperationResultParameter getResultParameter();

	/**
	 * Execute the method represented by this action.
	 *
	 * @param parameters
	 *            The parameters if the method has declared ones.
	 * @return The result or <code>null</code>.
	 */
	public Object invoke(Object jpaEntity, Object... parameters) throws ODataJPAModelException;
}
