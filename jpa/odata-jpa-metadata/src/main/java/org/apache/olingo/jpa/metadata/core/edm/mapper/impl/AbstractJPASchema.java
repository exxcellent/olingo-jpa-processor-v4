package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public abstract class AbstractJPASchema implements JPAElement {

	private final JPAEdmNameBuilder nameBuilder;

	public AbstractJPASchema(final String namespace) {
		nameBuilder = new JPAEdmNameBuilder(namespace);
	}

	protected final JPAEdmNameBuilder getNameBuilder() {
		return nameBuilder;
	}

	@Override
	public FullQualifiedName getExternalFQN() {
		return nameBuilder.buildFQN(getExternalName());
	}

	@Override
	public String getExternalName() {
		return nameBuilder.buildNamespace();
	}

	@Override
	public String getInternalName() {
		return getExternalName();
	}

	abstract IntermediateEnumType getEnumType(final Class<?> targetClass);

	abstract JPAEntityType getEntityType(final Class<?> targetClass);

	abstract JPAEntityType getEntityType(final String externalName);

	abstract JPAFunction getFunction(final String externalName);

	abstract List<JPAFunction> getFunctions();

	abstract JPAAction getAction(final String externalName);

	abstract List<JPAAction> getActions();

	public abstract CsdlSchema getEdmItem() throws ODataJPAModelException;

	/**
	 * {@link IntermediateStructuredType Structured types} including
	 * {@link IntermediateEntityType}'s.
	 */
	abstract IntermediateStructuredType getStructuredType(final Attribute<?, ?> jpaAttribute);

	abstract IntermediateEnumType createEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException;

	abstract List<JPAEntityType> getEntityTypes();

	abstract IntermediateComplexType getComplexType(final Class<?> targetClass);
}
