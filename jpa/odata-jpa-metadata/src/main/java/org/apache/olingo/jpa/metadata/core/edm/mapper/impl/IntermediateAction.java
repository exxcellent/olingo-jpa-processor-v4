package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * Mapper, that is able to convert different metadata resources into a edm
 * action metadata.
 *
 * @author Ralf Zozmann
 *
 */
class IntermediateAction extends IntermediateModelElement implements JPAAction {

	private static class ActionParameter implements JPAOperationParameter {

		private final IntermediateAction owner;
		private final int parameterIndex;
		public ActionParameter(final IntermediateAction owner, final int parameterIndex) {
			this.owner = owner;
			this.parameterIndex = parameterIndex;
		}

		@Override
		public String getName() {
			try {
				if (owner.isBound) {
					return owner.getEdmItem().getParameters().get(parameterIndex + 1).getName();
				} else {
					return owner.getEdmItem().getParameters().get(parameterIndex).getName();
				}
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Class<?> getType() {
			return owner.javaMethod.getParameters()[parameterIndex].getType();
		}

		@Override
		public Integer getMaxLength() {
			try {
				return owner.getEdmItem().getParameters().get(parameterIndex).getMaxLength();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Integer getPrecision() {
			try {
				return owner.getEdmItem().getParameters().get(parameterIndex).getPrecision();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Integer getScale() {
			try {
				return owner.getEdmItem().getParameters().get(parameterIndex).getScale();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public FullQualifiedName getTypeFQN() throws ODataJPAModelException {
			try {
				return owner.getEdmItem().getParameters().get(parameterIndex).getTypeFQN();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static class ActionResultParameter implements JPAOperationResultParameter {
		private final IntermediateAction owner;

		public ActionResultParameter(final IntermediateAction owner) {
			this.owner = owner;
		}

		@Override
		public Class<?> getType() {
			return owner.javaMethod.getReturnType();
		}

		@Override
		public FullQualifiedName getTypeFQN() {
			try {
				return owner.getEdmItem().getReturnType().getTypeFQN();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Integer getPrecision() {
			try {
				return owner.getEdmItem().getReturnType().getPrecision();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Integer getMaxLength() {
			try {
				return owner.getEdmItem().getReturnType().getMaxLength();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public Integer getScale() {
			try {
				return owner.getEdmItem().getReturnType().getScale();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}

		@Override
		public boolean isCollection() {
			try {
				return owner.getEdmItem().getReturnType().isCollection();
			} catch (final ODataJPAModelException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private CsdlAction edmAction = null;
	private final AbstractJPASchema schema;
	private final Method javaMethod;
	private final List<JPAOperationParameter> parameterList;
	private final JPAOperationResultParameter resultParameter;
	private final boolean isBound = true; // currently all actions are bound

	IntermediateAction(final JPAEdmNameBuilder nameBuilder, final Method actionMethod, final AbstractJPASchema schema)
			throws ODataJPAModelException, IllegalArgumentException {
		super(nameBuilder, JPANameBuilder.buildActionName(actionMethod));
		this.javaMethod = actionMethod;
		final EdmAction jpaAction = actionMethod.getAnnotation(EdmAction.class);
		if (jpaAction == null) {
			throw new IllegalArgumentException("Given JAVA method must be annotated with @"
					+ EdmAction.class.getSimpleName() + " to be handled as edm:Action");
		}
		this.setExternalName(jpaAction.name());
		this.schema = schema;

		final int noOfParameters = javaMethod.getParameters().length;
		parameterList = new ArrayList<JPAOperationParameter>(noOfParameters);
		for(int i=0;i<noOfParameters;i++) {
			parameterList.add(new ActionParameter(this, i));
		}

		resultParameter = new ActionResultParameter(this);
	}

	Method getJavaMethod() {
		return javaMethod;
	}

	@Override
	public List<JPAOperationParameter> getParameters() {
		return parameterList;
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
	private FullQualifiedName extractGenericTypeQualifiedName(final Type type) throws ODataJPAModelException {
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
			final IntermediateStructuredType et = schema.getEntityType(clazzType);
			if (et != null) {
				if (et.ignore()) {
					throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_WRONG_TYPE);
				}
				return nameBuilder.buildFQN(et.getEdmItem().getName());
			} else {
				// may throw an ODataJPAModelException
				final EdmPrimitiveTypeKind simpleType = JPATypeConvertor.convertToEdmSimpleType(clazzType);
				return simpleType.getFullQualifiedName();
			}
		}
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED, type.getTypeName());
	}

	private CsdlReturnType determineResultType() throws ODataJPAModelException {
		FullQualifiedName fqn = null;
		boolean isCollection = false;
		final IntermediateStructuredType et = schema.getEntityType(javaMethod.getReturnType());
		if (et != null) {
			if (et.ignore()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_WRONG_TYPE);
			}
			fqn = nameBuilder.buildFQN(et.getEdmItem().getName());
		} else if (javaMethod.getReturnType() == void.class || javaMethod.getReturnType() == Void.class) {
			return null;
		} else if (Collection.class.isAssignableFrom(javaMethod.getReturnType())) {
			isCollection = true;
			fqn = extractGenericTypeQualifiedName(javaMethod.getGenericReturnType());
		} else {
			// may throw an ODataJPAModelException
			final EdmPrimitiveTypeKind simpleType = JPATypeConvertor.convertToEdmSimpleType(javaMethod.getReturnType());
			fqn = simpleType.getFullQualifiedName();
		}

		if (fqn == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_RETURN_TYPE_ENTITY_NOT_FOUND);
		}

		final CsdlReturnType edmResultType = new CsdlReturnType();
		edmResultType.setType(fqn);
		edmResultType.setCollection(isCollection);
		edmResultType.setNullable(!javaMethod.isAnnotationPresent(NotNull.class));
		// TODO length, precision, scale
		return edmResultType;
	}

	/**
	 *
	 * @return The list of parameters or <code>null</code> if empty.
	 */
	private List<CsdlParameter> determineParameterTypes() throws ODataJPAModelException {
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
		// real method parameters...
		for (final Parameter p : javaMethod.getParameters()) {
			final CsdlParameter csdlParam = buildParameter(p);
			parameters.add(csdlParam);
		}
		if (parameters.isEmpty()) {
			return null;
		}
		return parameters;
	}

	private CsdlParameter buildParameter(final Parameter javaParameter) throws ODataJPAModelException {
		final EdmActionParameter edmParameterAnnotation = javaParameter.getAnnotation(EdmActionParameter.class);
		String name = javaParameter.getName();
		if(edmParameterAnnotation == null || edmParameterAnnotation.name() == null || edmParameterAnnotation.name().isEmpty()) {
			if(!javaParameter.isNamePresent()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_MISSING);
			}
		} else {
			name = edmParameterAnnotation.name();
		}
		final FullQualifiedName fqn = extractGenericTypeQualifiedName(javaParameter.getParameterizedType());
		final CsdlParameter parameter = new CsdlParameter();
		parameter.setName(name);
		parameter.setNullable(!javaParameter.isAnnotationPresent(NotNull.class));
		parameter.setCollection(Collection.class.isAssignableFrom(javaParameter.getType()));
		parameter.setType(fqn);
		return parameter;
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmAction != null) {
			return;
		}
		edmAction = new CsdlAction();
		edmAction.setName(getExternalName());
		edmAction.setParameters(determineParameterTypes());
		edmAction.setReturnType(determineResultType());
		edmAction.setBound(isBound);

	}

	@Override
	public Object invoke(final Object jpaEntity, final Object... args) throws ODataJPAModelException {
		try {
			final Object result = javaMethod.invoke(jpaEntity, args);
			if (result == null || getEdmItem().getReturnType() == null) {
				return null;
			}
			return result;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new ODataJPAModelException(e);
		}
	}
}
