package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * 
 * @author Ralf Zozmann
 *
 */
class IntermediateActionFactory {

	// use java builtin logging to avoid additional dependencies
	private final static Logger LOGGER = Logger.getLogger(IntermediateActionFactory.class.getName());
	
	Map<? extends String, ? extends IntermediateAction> create(final JPAEdmNameBuilder nameBuilder,
			final EntityType<?> jpaEntityType, final IntermediateMetamodelSchema schema) throws ODataJPAModelException {
		final Map<String, IntermediateAction> actionList = new HashMap<String, IntermediateAction>();

		for (Method method : jpaEntityType.getJavaType().getMethods()) {
			final EdmAction action = method.getAnnotation(EdmAction.class);
			if (action == null) {
				continue;
			}
			if(method.isAnnotationPresent(EdmIgnore.class)) {
				LOGGER.warning("Java method " + buildMethodSignature(method) + " has conflicting annotations @" 
						+ EdmAction.class.getSimpleName()+" and @" 
						+ EdmIgnore.class.getSimpleName() + ". Method will be ignored as edm:Action!");
				continue;
			}
			putAction(nameBuilder, schema, actionList, method);
		}
		return actionList;
	}

	private void putAction(final JPAEdmNameBuilder nameBuilder, final IntermediateMetamodelSchema schema,
			final Map<String, IntermediateAction> actionList, Method actionMethod) throws ODataJPAModelException {
		final IntermediateAction action = new IntermediateAction(nameBuilder, actionMethod, schema);
		if (actionList.containsKey(action.getExternalName())) {
			final IllegalStateException cause = new IllegalStateException("Duplicated action (name): "
					+ action.getExternalName() + " -> " + buildMethodSignature(action.getJavaMethod()));
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INNER_EXCEPTION, cause);
		}
		actionList.put(action.getInternalName(), action);
	}

	private static String buildMethodSignature(Method actionMethod) {
		return actionMethod.getDeclaringClass().getSimpleName()
				+ "::" + actionMethod.getName() + "(...)";
	}
}
