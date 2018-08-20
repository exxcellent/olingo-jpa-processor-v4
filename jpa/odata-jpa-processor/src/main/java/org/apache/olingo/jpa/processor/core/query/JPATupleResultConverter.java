package org.apache.olingo.jpa.processor.core.query;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

class JPATupleResultConverter extends JPATupleAbstractConverter {
	private final static Logger LOG = Logger.getLogger(JPATupleResultConverter.class.getName());

	public JPATupleResultConverter(final IntermediateServiceDocument sd, final JPAQueryResult jpaQueryResult,
			final UriHelper uriHelper, final ServiceMetadata serviceMetadata) throws ODataJPAModelException,
	ODataApplicationException {
		super(jpaQueryResult, uriHelper, sd, serviceMetadata);
	}

	/**
	 * Converts the {@link JPAQueryResult#ROOT_RESULT} part of the result map.
	 */
	public EntityCollection convertQueryResult() throws ODataApplicationException {
		final EntityCollection odataEntityCollection = new EntityCollection();

		final JPAEntityType jpaEntityType = getJpaEntityType();
		for (final Tuple row : getJpaQueryResult().getDirectMappingsResult(JPAQueryResult.ROOT_RESULT)) {
			final Entity odataEntity = convertRow2ODataEntity(row, odataEntityCollection);
			try {
				if (jpaEntityType.hasStream()) {
					odataEntity.setMediaContentType(determineContentType(jpaEntityType, row));
				}
			} catch (final ODataJPAModelException e) {
				LOG.log(Level.WARNING, "Couldn't set media stream on entity type " + jpaEntityType.getExternalName(),
						e);
			}
		}
		return odataEntityCollection;
	}

	private String determineContentType(final JPAEntityType jpaEntity, final Tuple row) throws ODataJPAModelException {
		if (jpaEntity.getContentType() != null && !jpaEntity.getContentType().isEmpty()) {
			return jpaEntity.getContentType();
		} else {
			Object rowElement = null;
			for (final JPAElement element : jpaEntity.getContentTypeAttributePath().getPathElements()) {
				rowElement = row.get(element.getExternalName());
			}

			return rowElement.toString();
		}
	}
}
