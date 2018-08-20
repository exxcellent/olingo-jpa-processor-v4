package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class JPAEntityConverter extends AbstractObjectConverter {

	public JPAEntityConverter(final EntityType<?> persistenceType, final UriHelper uriHelper,
			final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata, final Metamodel metamodel)
					throws ODataApplicationException, ODataJPAModelException {
		super(determineJPAEntityType(sd, persistenceType), uriHelper, sd, serviceMetadata);
	}

	public final static JPAEntityType determineJPAEntityType(final IntermediateServiceDocument sd, final EntityType<?> persistenceType) throws ODataJPAModelException {
		FullQualifiedName fqn;
		JPAEntityType jpaType;
		for(final CsdlSchema schema: sd.getEdmSchemas()) {
			fqn = new FullQualifiedName(schema.getNamespace(), persistenceType.getName());
			jpaType = sd.getEntityType(fqn);
			if(jpaType != null) {
				return jpaType;
			}
		}
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.FUNC_RETURN_TYPE_ENTITY_NOT_FOUND);
	}

	@Override
	protected Collection<? extends Link> createExpand(final Tuple row, final URI uri) throws ODataApplicationException {
		// TODO how to 'expand' aggregated entities in JPA objects?
		return Collections.emptyList();
	}

	/**
	 * Convert a OData entity into a JPA entity.
	 */
	public Object convertOData2JPAEntity(final Entity entity) throws ODataApplicationException {
		final JPAEntityType jpaEntityType = getJpaEntityType();
		if(!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_RESOURCE_TYPE, HttpStatusCode.INTERNAL_SERVER_ERROR);
		}
		try {
			final Object targetJPAInstance = newJPAInstance(jpaEntityType);
			for(final JPAAttribute jpaAttribute: jpaEntityType.getAttributes()) {
				final Property sourceOdataProperty = entity.getProperty(jpaAttribute.getExternalName());
				if(sourceOdataProperty == null) {
					continue;
				}
				convertOData2JPAProperty(targetJPAInstance, jpaEntityType/* , persistenceType */, jpaAttribute,
						sourceOdataProperty);
			}
			return targetJPAInstance;
		} catch (ODataJPAModelException | IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR, HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Convert an object managed by the {@link EntityManager entity manager} into a OData entity representation.
	 */
	public Entity convertJPA2ODataEntity(final Object jpaEntity) throws ODataApplicationException {
		try {
			final Collection<TupleElementFacade<Object>> elements = convertJPAStructuredType(jpaEntity,
					getJpaEntityType(), /* persistenceType, */ "");
			final Tuple tuple = new TupleFacade<Object>(elements);
			return convertRow2ODataEntity(tuple, new EntityCollection());
		} catch (final ODataJPAModelException e) {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
					HttpStatusCode.INTERNAL_SERVER_ERROR, e);
		}
	}


}
