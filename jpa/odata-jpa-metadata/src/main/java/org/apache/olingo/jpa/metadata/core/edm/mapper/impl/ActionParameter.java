package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Parameter;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlParameter;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmActionParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class ActionParameter implements JPAOperationParameter {

	private final IntermediateAction owner;
	private final Parameter parameter;

	public ActionParameter(final IntermediateAction owner, final Parameter parameter) {
		this.owner = owner;
		this.parameter = parameter;
	}

	private CsdlParameter buildParameter(final Parameter javaParameter) throws ODataJPAModelException {
		final EdmActionParameter edmParameterAnnotation = javaParameter.getAnnotation(EdmActionParameter.class);
		String name = javaParameter.getName();
		if (edmParameterAnnotation == null || edmParameterAnnotation.name() == null
				|| edmParameterAnnotation.name().isEmpty()) {
			if (!javaParameter.isNamePresent()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_MISSING);
			}
		} else {
			name = edmParameterAnnotation.name();
		}
		final FullQualifiedName fqn = intermediateAction.extractGenericTypeQualifiedName(javaParameter.getParameterizedType());
		final CsdlParameter parameter = new CsdlParameter();
		parameter.setName(name);
		parameter.setNullable(!javaParameter.isAnnotationPresent(NotNull.class));
		parameter.setCollection(Collection.class.isAssignableFrom(javaParameter.getType()));
		parameter.setType(fqn);
		return parameter;
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