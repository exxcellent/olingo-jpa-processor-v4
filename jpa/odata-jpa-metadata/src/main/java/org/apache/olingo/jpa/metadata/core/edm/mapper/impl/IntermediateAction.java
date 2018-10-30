package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Mapper, that is able to convert different metadata resources into a edm
 * action metadata.
 *
 * @author Ralf Zozmann
 *
 */
public class IntermediateAction extends IntermediateModelElement implements JPAAction {

	private CsdlAction edmAction = null;
	private final IntermediateServiceDocument isd;
	final Method javaMethod;
	private final List<ActionParameter> parameterList;
	private final ActionResultParameter resultParameter;
	boolean isBound = true;

	IntermediateAction(final JPAEdmNameBuilder nameBuilder, final Method actionMethod,
			final IntermediateServiceDocument isd)
					throws ODataJPAModelException, IllegalArgumentException {
		super(nameBuilder, JPANameBuilder.buildActionName(actionMethod));
		this.javaMethod = actionMethod;
		if (Modifier.isStatic(actionMethod.getModifiers())) {
			if (!Modifier.isPublic(actionMethod.getModifiers())) {
				throw new IllegalArgumentException(
						"Given JAVA method must be 'public static' to be handled as unbound edm:Action");
			}
			isBound = false;
		}
		final EdmAction jpaAction = actionMethod.getAnnotation(EdmAction.class);
		if (jpaAction == null) {
			throw new IllegalArgumentException("Given JAVA method must be annotated with @"
					+ EdmAction.class.getSimpleName() + " to be handled as edm:Action");
		}
		String name = jpaAction.name();
		if (name == null || name.isEmpty()) {
			name = actionMethod.getName();
		}
		this.setExternalName(name);
		this.isd = isd;

		parameterList = new ArrayList<ActionParameter>(javaMethod.getParameters().length);
		int index = 0;
		for (final Parameter p : javaMethod.getParameters()) {
			final ActionParameter ap = new ActionParameter(this, p, index);
			parameterList.add(ap);
			index++;
		}

		if (javaMethod.getReturnType() == void.class || javaMethod.getReturnType() == Void.class) {
			resultParameter = null;
		} else {
			resultParameter = new ActionResultParameter(this);
		}
	}

	public Method getJavaMethod() {
		return javaMethod;
	}

	@Override
	public List<JPAOperationParameter> getParameters() {
		return Collections.unmodifiableList(parameterList);
	}

	@Override
	public JPAOperationResultParameter getResultParameter() {
		return resultParameter;
	}

	@Override
	CsdlAction getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmAction;
	}

	/**
	 * Helper method to extract 'parameter type' from a parameterized (generic) type like a {@link Collection}.
	 */
	FullQualifiedName extractGenericTypeQualifiedName(final Type type) throws ODataJPAModelException {
		Class<?> clazzType = null;
		if(Class.class.isInstance(type)) {
			// simply use the argument self without further inspection
			clazzType = (Class<?>) type;
		}
		else if(ParameterizedType.class.isInstance(type)) {
			final ParameterizedType pType = (ParameterizedType) type;
			if(pType.getActualTypeArguments().length == 1) {
				final Type genericType = pType.getActualTypeArguments()[0];
				if(Class.class.isInstance(genericType)) {
					clazzType = (Class<?>) genericType;
				}
			}
		}
		// now adapt to oData type to determine FQN
		if(clazzType != null) {
			final JPAStructuredType et = isd.getEntityType(clazzType);
			if (et != null) {
				if (et.ignore()) {
					throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_WRONG_TYPE);
				}
				return et.getExternalFQN();
			} else if (clazzType.isEnum()) {
				final IntermediateEnumType enT = isd.getEnumType(clazzType);
				if (enT != null) {
					return enT.getExternalFQN();
				}
			} else {
				// may throw an ODataJPAModelException
				final EdmPrimitiveTypeKind simpleType = JPATypeConvertor.convertToEdmSimpleType(clazzType);
				return simpleType.getFullQualifiedName();
			}
		}
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED, type.getTypeName());
	}

	/**
	 *
	 * @return The list of parameters or <code>null</code> if empty.
	 */
	private List<CsdlParameter> buildParameterList() throws ODataJPAModelException {
		final List<CsdlParameter> parameters = new LinkedList<>();
		if (isBound) {
			// if an action is 'bound' then the first parameter in list must be the entity
			// type where the action is bound to; we generate that on demand
			final FullQualifiedName fqn = extractGenericTypeQualifiedName(javaMethod.getDeclaringClass());
			final CsdlParameter parameter = new CsdlParameter();
			parameter.setName(BOUND_ACTION_ENTITY_PARAMETER_NAME);
			parameter.setNullable(false);// TODO mark as 'nullable' to work with Deserializer missing the 'bound resource parameter'?
			parameter.setCollection(false);
			parameter.setType(fqn);
			parameters.add(parameter);
		}
		// other relevant method parameters...
		for (final ActionParameter ap : parameterList) {
			final CsdlParameter csdlParam = ap.getEdmItem();
			if (csdlParam == null) {
				continue;
			}
			parameters.add(csdlParam);
		}
		if (parameters.isEmpty()) {
			return null;
		}
		return parameters;
	}


	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmAction != null) {
			return;
		}
		edmAction = new CsdlAction();
		edmAction.setName(getExternalName());
		edmAction.setParameters(buildParameterList());
		if (resultParameter != null) {
			edmAction.setReturnType(resultParameter.getEdmItem());
		}
		edmAction.setBound(isBound);
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

}
