package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;

/**
 * Build the internal name for Intermediate Model Elements
 *
 * @author Oliver Grande
 */
public final class JPANameBuilder {
	public static String buildStructuredTypeName(final Class<?> clazz) {
		return clazz.getCanonicalName();
	}

	public static String buildAttributeName(final Attribute<?, ?> jpaAttribute) {
		return jpaAttribute.getName();
	}

	public static String buildAssociationName(final Attribute<?, ?> jpaAttribute) {
		return jpaAttribute.getName();
	}

	public static String buildFunctionName(final EdmFunction jpaFunction) {
		return jpaFunction.name();
	}

	public static String buildActionName(final Method actionMethod) {
		return actionMethod.getName();
	}

	public static String buildEntitySetName(final JPAEdmNameBuilder nameBuilder, final JPAElement entityType) {
		return nameBuilder.buildFQN(entityType.getInternalName()).getFullQualifiedNameAsString();
	}
}
