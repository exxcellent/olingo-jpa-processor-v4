package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class ActionParameter implements JPAOperationParameter {

	private final IntermediateAction owner;
	private final Parameter javaParameter;
	private final String name;
	private CsdlParameter csdlParameter = null;
	private final ParameterKind parameterKind;

	public ActionParameter(final IntermediateAction owner, final Parameter parameter, final int parameterIndex)
			throws ODataJPAModelException {
		this.owner = owner;
		this.javaParameter = parameter;

		if (javaParameter.isAnnotationPresent(Inject.class)
				&& javaParameter.isAnnotationPresent(EdmActionParameter.class)) {
			parameterKind = ParameterKind.Invalid;
			name = null;
		} else if (javaParameter.isAnnotationPresent(Inject.class)) {
			parameterKind = ParameterKind.Inject;
			name = "arg".concat(Integer.toString(parameterIndex));
		} else if (javaParameter.isAnnotationPresent(EdmActionParameter.class)) {
			parameterKind = ParameterKind.OData;
			final EdmActionParameter edmParameterAnnotation = javaParameter.getAnnotation(EdmActionParameter.class);
			if (edmParameterAnnotation.name() == null || edmParameterAnnotation.name().isEmpty()) {
				if (!javaParameter.isNamePresent()) {
					throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_MISSING,
							owner.getJavaMethod().getName());
				}
				name = javaParameter.getName();
			} else {
				name = edmParameterAnnotation.name();
			}
		} else {
			parameterKind = ParameterKind.Invalid;
			name = null;
		}

		if (parameterKind == ParameterKind.Invalid) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_PARAMETER,
					owner.getJavaMethod().getName());
		}
	}

	@Override
	public ParameterKind getParameterKind() {
		return parameterKind;
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		// currently not supported
		return null;
	}

	/**
	 *
	 * @return The CSDL parameter item or <code>null</code> if not an OData related
	 *         parameter (like a injected runtime parameter on server side)
	 * @throws ODataJPAModelException
	 */
	public CsdlParameter getEdmItem() throws ODataJPAModelException {
		if (csdlParameter != null) {
			return csdlParameter;
		}
		csdlParameter = buildParameter();
		return csdlParameter;
	}

	private CsdlParameter buildParameter() throws ODataJPAModelException {
		if (parameterKind == ParameterKind.Inject) {
			return null;
		}
		final FullQualifiedName fqn = owner.extractGenericTypeQualifiedName(javaParameter.getParameterizedType());
		final CsdlParameter parameter = new CsdlParameter();
		parameter.setName(name);
		parameter.setNullable(!javaParameter.isAnnotationPresent(NotNull.class));
		parameter.setCollection(Collection.class.isAssignableFrom(javaParameter.getType()));
		parameter.setType(fqn);
		return parameter;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<?> getType() {
		return javaParameter.getType();
	}

	@Override
	public Integer getMaxLength() {
		try {
			if (getEdmItem() == null) {
				throw new IllegalStateException(
						"A call to this method is only supported for OData related action/function parameters");
			}
			return getEdmItem().getMaxLength();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Integer getPrecision() {
		try {
			if (getEdmItem() == null) {
				throw new IllegalStateException(
						"A call to this method is only supported for OData related action/function parameters");
			}
			return getEdmItem().getPrecision();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Integer getScale() {
		try {
			if (getEdmItem() == null) {
				throw new IllegalStateException(
						"A call to this method is only supported for OData related action/function parameters");
			}
			return getEdmItem().getScale();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public FullQualifiedName getTypeFQN() throws ODataJPAModelException {
		try {
			if (getEdmItem() == null) {
				throw new IllegalStateException(
						"A call to this method is only supported for OData related action/function parameters");
			}
			return getEdmItem().getTypeFQN();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}
}