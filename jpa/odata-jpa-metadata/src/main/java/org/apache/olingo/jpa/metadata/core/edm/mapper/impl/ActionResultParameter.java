package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class ActionResultParameter implements JPAOperationResultParameter {

	private final IntermediateAction owner;

	private CsdlReturnType returnType = null;
	public ActionResultParameter(final IntermediateAction owner) {
		this.owner = owner;
	}

	@Override
	public Class<?> getType() {
		return owner.getJavaMethod().getReturnType();
	}

	@Override
	public FullQualifiedName getTypeFQN() {
		try {
			lazyBuildEdmItem();
			return returnType.getTypeFQN();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Integer getPrecision() {
		try {
			lazyBuildEdmItem();
			return returnType.getPrecision();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Integer getMaxLength() {
		try {
			lazyBuildEdmItem();
			return returnType.getMaxLength();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Integer getScale() {
		try {
			lazyBuildEdmItem();
			return returnType.getScale();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean isCollection() {
		try {
			lazyBuildEdmItem();
			return returnType.isCollection();
		} catch (final ODataJPAModelException e) {
			throw new IllegalStateException(e);
		}
	}

	CsdlReturnType getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return returnType;
	}

	private void lazyBuildEdmItem() throws ODataJPAModelException {
		if (returnType != null) {
			return;
		}
		final Method javaMethod = owner.getJavaMethod();
		final FullQualifiedName fqn = owner.extractGenericTypeQualifiedName(javaMethod.getGenericReturnType());

		returnType = new CsdlReturnType();
		returnType.setType(fqn);
		returnType.setCollection(Collection.class.isAssignableFrom(javaMethod.getReturnType()));
		returnType.setNullable(!owner.getJavaMethod().isAnnotationPresent(NotNull.class));
		// TODO length, precision, scale
	}

}