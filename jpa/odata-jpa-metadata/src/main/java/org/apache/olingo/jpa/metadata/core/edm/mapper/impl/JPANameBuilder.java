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

	public static String buildStructuredTypeInternalName(final Class<?> clazz) {
		return clazz.getCanonicalName();
	}

	public static String buildAssociationInternalName(final Attribute<?, ?> jpaAttribute) {
		return jpaAttribute.getName();
	}

	public static String buildFunctionInternalName(final EdmFunction jpaFunction) {
		return jpaFunction.name();
	}

	/**
	 * The method declaring class may differ from the real owning entity class,
	 * because overload of methods is possible.
	 */
	public static String buildActionInternalName(final Class<?> jpaEntityClass, final Method actionMethod) {
		return jpaEntityClass.getSimpleName().concat("::").concat(actionMethod.getName());
	}

	public static String buildEntitySetInternalName(final JPAEdmNameBuilder nameBuilder, final JPAElement entityType) {
		return nameBuilder.buildFQN(entityType.getInternalName()).getFullQualifiedNameAsString();
	}
}
