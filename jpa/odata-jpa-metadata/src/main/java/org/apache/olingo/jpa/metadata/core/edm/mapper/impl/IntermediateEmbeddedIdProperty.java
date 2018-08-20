package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * @deprecated Replace by {@link IntermediateProperty} (+ isKey() == true)
 *             instead of such an special sub class.
 *
 */
@Deprecated
public class IntermediateEmbeddedIdProperty extends IntermediateProperty {

	IntermediateEmbeddedIdProperty(final JPAEdmNameBuilder nameBuilder, final Attribute<?, ?> jpaAttribute,
			final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
		super(nameBuilder, jpaAttribute, serviceDocument);
	}

	@Override
	public boolean isKey() {
		return true;
	}

}
