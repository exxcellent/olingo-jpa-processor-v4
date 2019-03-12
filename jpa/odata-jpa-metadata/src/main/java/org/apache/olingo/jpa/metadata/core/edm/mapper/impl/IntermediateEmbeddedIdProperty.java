package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.AttributeMapping;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * {@link javax.persistence.EmbeddedId @EmbeddedId's} are handled as special
 * attributes, because all the id fields in the embedded id are threat as
 * properties of the owning entity.
 *
 */
public class IntermediateEmbeddedIdProperty extends IntermediateProperty {

	IntermediateEmbeddedIdProperty(final JPAEdmNameBuilder nameBuilder, final Attribute<?, ?> jpaAttribute,
			final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
		super(nameBuilder, jpaAttribute, serviceDocument);
	}

	@Override
	public boolean isKey() {
		return true;
	}

	@Override
	public boolean isComplex() {
		// a embedded id has always nested attributes
		return super.isComplex();
	}

	@Override
	public AttributeMapping getAttributeMapping() {
		return AttributeMapping.EMBEDDED_ID;
	}
}
