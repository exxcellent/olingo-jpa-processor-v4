package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlReturnType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
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
		FullQualifiedName fqn = null;
		boolean isCollection = false;
		final Method javaMethod = owner.getJavaMethod();
		final JPAStructuredType et = owner.getSchema().getEntityType(javaMethod.getReturnType());
		if (et != null) {
			if (et.ignore()) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_WRONG_TYPE);
			}
			fqn = owner.nameBuilder.buildFQN(et.getEdmItem().getName());
		} else if (javaMethod.getReturnType() == void.class || javaMethod.getReturnType() == Void.class) {
			// suppress instance of this class if return type is void
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_PARAM_OUT_MISSING);
		} else if (Collection.class.isAssignableFrom(javaMethod.getReturnType())) {
			isCollection = true;
			fqn = owner.extractGenericTypeQualifiedName(javaMethod.getGenericReturnType());
		} else {
			// may throw an ODataJPAModelException
			final EdmPrimitiveTypeKind simpleType = JPATypeConvertor
					.convertToEdmSimpleType(javaMethod.getReturnType());
			fqn = simpleType.getFullQualifiedName();
		}

		if (fqn == null) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_RETURN_TYPE_ENTITY_NOT_FOUND);
		}

		returnType = new CsdlReturnType();
		returnType.setType(fqn);
		returnType.setCollection(isCollection);
		returnType.setNullable(!owner.getJavaMethod().isAnnotationPresent(NotNull.class));
		// TODO length, precision, scale
	}

}