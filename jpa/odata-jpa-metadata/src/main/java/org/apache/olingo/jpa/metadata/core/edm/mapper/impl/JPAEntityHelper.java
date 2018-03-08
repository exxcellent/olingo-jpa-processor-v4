package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Parameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * 
 * @author Ralf Zozmann
 *
 */
public class JPAEntityHelper {

	private final EntityManager em;
	
	public JPAEntityHelper(EntityManager em) {
		this.em = em;
	}
	
	/**
	 * Invoke the corresponding JAVA method.
	 * 
	 * @return The result or <code>null</code> if no result parameter is defined or value self is <code>null</code>
	 * @see #loadJPAEntity(JPAEntityType, Entity)
	 */
	@SuppressWarnings("unchecked")
	public final <R> R invokeActionMethod(JPAEntityType jpaType, Entity oDataEntity, JPAAction jpaAction, Map<String, Parameter> parameters) throws ODataJPAModelException {
		Method javaMethod = determineJavaMethod(jpaAction);
		if(javaMethod.getDeclaringClass() != jpaType.getTypeClass()) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL);
		}
		Object jpaEntity = loadJPAEntity(jpaType, oDataEntity);
		if(jpaEntity == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.GENERAL);
		}
		try {
			Object[] args = buildArguments(jpaAction, parameters);
			Object result = javaMethod.invoke(jpaEntity, args);
			if(result == null || ((IntermediateAction)jpaAction).getEdmItem().getReturnType() == null) {
				return null;
			}
			return (R) result;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ODataJPAModelException(e);
		}
	}
	
	private Object[] buildArguments(JPAAction jpaAction, Map<String, Parameter> parameterValues) throws ODataJPAModelException {
		Object[] args = new Object[jpaAction.getParameters().size()];
		for(int i=0;i<jpaAction.getParameters().size();i++) {
			args[i] = null;
			JPAOperationParameter jpaParameter = jpaAction.getParameters().get(i);
			Parameter p = parameterValues.get(jpaParameter.getName());
			if(p != null) {
				args[i] = p.getValue();
			}
		}
		return args;
	}
	
	private Method determineJavaMethod(JPAAction jpaAction) {
		return ((IntermediateAction)jpaAction).getJavaMethod();
	}
	
	/**
	 * Load/search a JPA entity based on the identifier taken from the given OData entity. The JPA entity will assigned to the
	 * current {@link #getEntityManager() entity manager}.
	 * 
	 * @param oDataEntity The OData entity used to identify the corresponding JPA entity. 
	 * @return A instance of one of the {@link Metamodel#getEntities() managed types}, loaded based on the given OData entity. 
	 * @throws ODataJPAModelException
	 */
	@SuppressWarnings("unchecked")
	public final <O> O loadJPAEntity(JPAEntityType jpaType, Entity oDataEntity) throws ODataJPAModelException {
			List<Object> listPrimaryKeyValues = new LinkedList<>();
			// TODO risk: the order of (primary) key/id attributes must be the same as in descriptor; that is not ensured
			for(JPAAttribute jpaAttribute: jpaType.getKeyAttributes()) {
				if(jpaAttribute.isComplex() || jpaAttribute.isAssociation()) {
					throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_COMPLEX_TYPE);
				}
				Object value = oDataEntity.getProperty(jpaAttribute.getExternalName()).getValue();
				listPrimaryKeyValues.add(value);
			}
			if(listPrimaryKeyValues.isEmpty()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_KEY);
			}
			return (O) em.find((Class<O>)jpaType.getTypeClass(), listPrimaryKeyValues, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
	}
	
}
