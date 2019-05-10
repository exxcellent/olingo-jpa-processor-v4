package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

/**
 * Class to convert complete OData or JPA/DTO entities.
 *
 * @author Ralf Zozmann
 *
 */
public class EntityConverter extends JPATupleAbstractConverter {

	public EntityConverter(final JPAEntityType jpaConversionTargetEntity, final UriHelper uriHelper,
	        final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata)
	        throws ODataJPAModelException {
		super(jpaConversionTargetEntity, uriHelper, sd, serviceMetadata);
	}

	/**
	 * @deprecated Not working for DTO's
	 */
	@Deprecated
	public EntityConverter(final EntityType<?> persistenceType, final UriHelper uriHelper,
	        final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata)
	        throws ODataJPAModelException {
		super(determineJPAEntityType(sd, persistenceType), uriHelper, sd, serviceMetadata);
	}

	public final static JPAEntityType determineJPAEntityType(final IntermediateServiceDocument sd,
	        final EntityType<?> persistenceType) throws ODataJPAModelException {
		FullQualifiedName fqn;
		JPAEntityType jpaType;
		for (final CsdlSchema schema : sd.getEdmSchemas()) {
			fqn = new FullQualifiedName(schema.getNamespace(), persistenceType.getName());
			jpaType = sd.getEntityType(fqn);
			if (jpaType != null) {
				return jpaType;
			}
		}
		throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE, persistenceType.getName());
	}

	/**
	 * Convert a OData entity into a JPA entity.
	 */
	public Object convertOData2JPAEntity(final Entity entity) throws ODataJPAModelException, ODataJPAConversionException {
		final JPAEntityType jpaEntityType = getJpaEntityType();
		if (!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
			throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE,
			        jpaEntityType.getExternalFQN().getFullQualifiedNameAsString());
		}
		try {
			final Object targetJPAInstance = newJPAInstance(jpaEntityType);
			for (final JPAAttribute<?> jpaAttribute : jpaEntityType.getAttributes()) {
				transferOData2JPAProperty(targetJPAInstance, jpaEntityType, jpaAttribute, entity.getProperties());
			}
			return targetJPAInstance;
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			throw new ODataJPAConversionException(e, ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM, e.getMessage());
		}
	}

	/**
	 * Convert an object managed by the {@link EntityManager entity manager} into a
	 * OData entity representation.
	 *
	 */
	public Entity convertJPA2ODataEntity(final Object jpaEntity) throws ODataJPAModelException, ODataJPAConversionException {
		return convertJPA2ODataEntity(getJpaEntityType(), jpaEntity);
	}

	private Entity convertJPA2ODataEntity(final JPAEntityType jpaType, final Object jpaEntity)
	        throws ODataJPAModelException, ODataJPAConversionException {

		final Entity odataEntity = new Entity();
		odataEntity.setType(jpaType.getExternalFQN().getFullQualifiedNameAsString());
		final List<Property> properties = odataEntity.getProperties();

		final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
		for (final JPASimpleAttribute jpaAttribute : jpaType.getAttributes()) {
			final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
			if (jpaAttribute.isAssociation()) {
				// couldn't be happen
				throw new IllegalStateException();
			} else if (jpaAttribute.isComplex()) {
				final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value);
				if (complexTypeProperty != null) {
					properties.add(complexTypeProperty);
				}
			} else {
				// simple attribute (or collection)
				final String alias = jpaAttribute.getExternalName();
				convertJPAValue2ODataAttribute(value, alias, "", jpaType, complexValueBuffer, 0, properties);
			}
		}
		odataEntity.getNavigationLinks().addAll(convertAssociations(jpaType, jpaEntity));

		odataEntity.setId(createId(odataEntity, jpaType, getUriHelper()));

		return odataEntity;
	}

	private Collection<Link> convertAssociations(final JPAStructuredType jpaType, final Object jpaObject)
	        throws ODataJPAModelException, ODataJPAConversionException {
		final List<Link> entityExpandLinks = new LinkedList<Link>();
		for (final JPAAssociationAttribute jpaAttribute : jpaType.getAssociations()) {
			final JPAAssociationPath assoziation = jpaType.getDeclaredAssociation(jpaAttribute.getExternalName());
			final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaObject);
			if (!jpaAttribute.isAssociation()) {
				throw new IllegalStateException();
			}
			if (value == null || assoziation.getLeaf().isCollection() && ((Collection<?>) value).isEmpty()) {
				continue;
			}
			final Link link = new Link();
			link.setTitle(assoziation.getLeaf().getExternalName());
			link.setRel(Constants.NS_ASSOCIATION_LINK_REL + link.getTitle());
			link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
			if (assoziation.getLeaf().isCollection()) {
				final EntityCollection expandCollection = new EntityCollection();
				link.setInlineEntitySet(expandCollection);
				for (final Object cEntry : ((Collection<?>) value)) {
					final Entity expandEntity = convertJPA2ODataEntity((JPAEntityType) jpaAttribute.getStructuredType(),
					        cEntry);
					expandCollection.getEntities().add(expandEntity);
				}
				expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
				// TODO link.setHref(parentUri.toASCIIString());
			} else {
				final Entity expandEntity = convertJPA2ODataEntity((JPAEntityType) jpaAttribute.getStructuredType(),
				        value);
				link.setInlineEntity(expandEntity);
				// TODO link.setHref(expandCollection.getId().toASCIIString());
			}
			entityExpandLinks.add(link);
		}
		return entityExpandLinks;
	}

	private Property convertJPAComplexAttribute2OData(final JPASimpleAttribute jpaAttribute, final Object value)
	        throws ODataJPAModelException, ODataJPAConversionException {
		final JPAStructuredType attributeType = jpaAttribute.getStructuredType();
		if (jpaAttribute.isCollection()) {
			final Collection<?> valuesToProcess = (value == null) ? Collections.emptyList() : (Collection<?>) value;
			final Collection<ComplexValue> convertedValues = new LinkedList<>();
			for (final Object cValue : valuesToProcess) {
				final ComplexValue complexValue = new ComplexValue();
				convertedValues.add(complexValue);
				final List<Property> cvProperties = complexValue.getValue();
				convertComplexTypeValue2OData(attributeType, cValue, cvProperties);
				complexValue.getNavigationLinks().addAll(convertAssociations(attributeType, cValue));
			}
			return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
			        jpaAttribute.getExternalName(), ValueType.COLLECTION_COMPLEX, convertedValues);
		} else {
			final ComplexValue complexValue = new ComplexValue();
			final List<Property> cvProperties = complexValue.getValue();
			convertComplexTypeValue2OData(attributeType, value, cvProperties);
			complexValue.getNavigationLinks().addAll(convertAssociations(attributeType, value));
			return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
			        jpaAttribute.getExternalName(), ValueType.COMPLEX, complexValue);
		}
	}

	private void convertComplexTypeValue2OData(final JPAStructuredType attributeType, final Object cValue,
	        final List<Property> cvProperties)
	        throws ODataJPAModelException, ODataJPAConversionException {
		if (cValue == null) {
			return;
		}

		final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
		for (final JPASimpleAttribute jpaAttribute : attributeType.getAttributes()) {
			final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(cValue);
			if (jpaAttribute.isComplex()) {
				final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value);
				if (complexTypeProperty != null) {
					cvProperties.add(complexTypeProperty);
				}
			} else {
				// simple attribute (or collection)
				final String alias = jpaAttribute.getExternalName();
				convertJPAValue2ODataAttribute(value, alias, "", attributeType, complexValueBuffer, 0,
				        cvProperties);
			}
		}

	}
}
