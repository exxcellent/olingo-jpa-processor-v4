package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

public class DTOConverter extends AbstractObjectConverter {

	public DTOConverter(final JPAEntityType jpaConversionTargetEntity, final UriHelper uriHelper,
			final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata) throws ODataApplicationException {
		super(jpaConversionTargetEntity, uriHelper, sd, serviceMetadata);
	}

	@Override
	protected Collection<? extends Link> createExpand(final Tuple row, final URI uri) throws ODataApplicationException {
		return Collections.emptyList();
	}

	public Object convertODataEntity2DTO(final Entity entity) throws ODataApplicationException {
		final JPAEntityType jpaEntityType = getJpaEntityType();
		if (!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE,
					HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		try {
			final Object targetDTOInstance = newJPAInstance(jpaEntityType);
			for (final JPAAttribute jpaAttribute : jpaEntityType.getAttributes()) {
				final Property sourceOdataProperty = entity.getProperty(jpaAttribute.getExternalName());
				if (sourceOdataProperty == null) {
					continue;
				}
				convertOData2JPAProperty(targetDTOInstance, jpaEntityType, jpaAttribute,
						sourceOdataProperty);
			}
			return targetDTOInstance;
		} catch (ODataJPAModelException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	public Entity convertDTO2ODataEntity(final Object dto) throws ODataApplicationException {
		try {
			final Collection<TupleElementFacade<Object>> elements = convertJPAStructuredType(dto, getJpaEntityType(),
					"");
			final Tuple tuple = new TupleFacade<Object>(elements);
			return convertRow2ODataEntity(tuple, new EntityCollection());
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}

	}

}
