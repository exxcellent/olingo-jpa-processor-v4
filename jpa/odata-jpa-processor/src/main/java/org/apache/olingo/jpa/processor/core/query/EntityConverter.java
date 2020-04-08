package org.apache.olingo.jpa.processor.core.query;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
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
public class EntityConverter extends AbstractEntityConverter {

  private static class EntityAsLinkException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final URI link;

    EntityAsLinkException(final URI entityLink) {
      super();
      this.link = entityLink;
    }

    public URI getLink() {
      return link;
    }

  }
  public EntityConverter(final UriHelper uriHelper,
      final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata)
          throws ODataJPAModelException {
    super(uriHelper, sd, serviceMetadata);
  }

  @SuppressWarnings({ "null" })
  private Object convertODataBindingLink2JPAAssociationProperty(final Object targetJPAObject,
      final JPAAssociationAttribute association, final Entity odataOwnerEntity, final Link odataLink,
      final Map<String, Object> mapId2Instance) throws ODataJPAModelException,
  ODataJPAConversionException {
    if(odataLink==null) {
      return null;
    }
    Object result = null;
    Collection<String> links;
    Collection<Object> list = null;
    if(association.isCollection()) {
      links = odataLink.getBindingLinks();
      list = new LinkedList<Object>();
      result = list;
    } else {
      links = Collections.singletonList(odataLink.getBindingLink());
    }
    // assign to owner entity
    for(final String bindingUri: links) {
      // workaround, because base uri is handled as not null by Olingo client and so an id with '/' prefix is produced
      // for otherwise not existing base uri
      final String bindingId = odataOwnerEntity.getBaseURI() == null && bindingUri.startsWith("/") ? bindingUri
          .substring(1) : bindingUri;
          final Object target = mapId2Instance.get(bindingId);
          if(target == null) {
            throw new ODataJPAConversionException(ODataJPAConversionException.MessageKeys.BINDING_LINK_NOT_RESOLVED,
                bindingId, association.getExternalName());
          }
          if (association.isCollection()) {
            list.add(target);
          } else {
            // singleton list, safe assignment
            result = target;
          }
    }
    if (result == null) {
      return null;
    }
    association.getAttributeAccessor().setPropertyValue(targetJPAObject, result);
    return result;
  }

  private Object convertODataNavigationLink2JPAAssociationProperty(final Object targetJPAObject,
      final JPAAssociationAttribute association, final Link odataLink,
      final Map<String, Object> mapId2Instance)
          throws ODataJPAModelException,
          ODataJPAConversionException {
    if(odataLink==null) {
      return null;
    }
    Object result;
    if(association.isCollection()) {
      final List<Entity> odataEntities = odataLink.getInlineEntitySet().getEntities();
      final Collection<Object> list = new ArrayList<Object>();
      for (final Entity childEntity : odataEntities) {
        final JPAStructuredType jpaType = getIntermediateServiceDocument().getEntityType(new FullQualifiedName(
            childEntity
            .getType()));
        final Object jpaInstance = convertOData2JPAEntityInternal(childEntity, jpaType, mapId2Instance);
        list.add(jpaInstance);
      }
      result = list;
    } else {
      final Entity childEntity = odataLink.getInlineEntity();
      if(childEntity == null) {
        return null;
      }
      final JPAStructuredType jpaType = getIntermediateServiceDocument().getEntityType(new FullQualifiedName(childEntity
          .getType()));
      result = convertOData2JPAEntityInternal(childEntity, jpaType, mapId2Instance);
    }
    // assign to owner entity
    association.getAttributeAccessor().setPropertyValue(targetJPAObject, result);
    return result;
  }

  /**
   * Convert a OData entity into a JPA entity.
   */
  public Object convertOData2JPAEntity(final Entity entity, final JPAStructuredType jpaEntityType)
      throws ODataJPAModelException, ODataJPAConversionException {
    return convertOData2JPAEntityInternal(entity, jpaEntityType, new HashMap<String, Object>());
  }

  private Object convertOData2JPAEntityInternal(final Entity entity, final JPAStructuredType jpaEntityType,
      final Map<String, Object> mapId2Instance)
          throws ODataJPAModelException, ODataJPAConversionException {
    if (!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE,
          jpaEntityType.getExternalFQN().getFullQualifiedNameAsString());
    }
    try {
      final Object targetJPAInstance = newJPAInstance(jpaEntityType);
      URI id = entity.getId();
      if (id == null && JPAEntityType.class.isInstance(jpaEntityType)) {
        // try to create id on demand
        try {
          id = createId(entity, JPAEntityType.class.cast(jpaEntityType), true);
        } catch (final ODataRuntimeException e) {
          // ignore
          id = null;
        }
      }
      if (id != null) {
        mapId2Instance.put(id.toASCIIString(), targetJPAInstance);
      }
      for (final JPAAttribute<?> jpaAttribute : jpaEntityType.getAttributes()) {
        transferOData2JPAProperty(targetJPAInstance, jpaAttribute, entity.getProperties());
      }
      for (final JPAAssociationAttribute association : jpaEntityType.getAssociations()) {
        convertODataNavigationLink2JPAAssociationProperty(targetJPAInstance, association, entity
            .getNavigationLink(association
                .getExternalName()), mapId2Instance);
      }
      for (final JPAAssociationAttribute association : jpaEntityType.getAssociations()) {
        convertODataBindingLink2JPAAssociationProperty(targetJPAInstance, association, entity, entity
            .getNavigationBinding(
                association.getExternalName()), mapId2Instance);
      }
      return targetJPAInstance;
    } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
      throw new ODataJPAConversionException(e, ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM, e.getMessage());
    }
  }

  private Property convertJPAAttribute2OData(final JPASimpleAttribute jpaAttribute, final Object value,
      final JPAStructuredType jpaType, final Map<String, Object> complexValueBuffer, final List<Property> properties,
      final Set<URI> processedEntities) throws ODataJPAConversionException, ODataJPAModelException {
    if (jpaAttribute.isAssociation()) {
      // couldn't be happen
      throw new IllegalStateException();
    } else if (jpaAttribute.isComplex()) {
      final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value, processedEntities);
      if (complexTypeProperty != null) {
        properties.add(complexTypeProperty);
      }
      return complexTypeProperty;
    } else {
      // simple attribute (or collection)
      final String alias = jpaAttribute.getExternalName();
      return convertJPAValue2ODataAttribute(value, alias, "", jpaType, complexValueBuffer, 0, properties);
    }
  }

  private Entity convertJPA2ODataEntityInternal(final JPAEntityType jpaType, final Object jpaEntity,
      final Set<URI> processedEntities)
          throws ODataJPAModelException, ODataJPAConversionException, EntityAsLinkException {
    final Entity odataEntity = new Entity();
    odataEntity.setType(jpaType.getExternalFQN().getFullQualifiedNameAsString());
    final List<Property> properties = odataEntity.getProperties();

    final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
    // 1. convert key attributes to create an id for the entity
    for (final JPASimpleAttribute jpaAttribute : jpaType.getAttributes()) {
      if (!jpaAttribute.isKey()) {
        // only key attribute
        continue;
      }
      final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
      convertJPAAttribute2OData(jpaAttribute, value, jpaType, complexValueBuffer, properties, processedEntities);
    }

    // id of entity must be set before relationships are processed
    odataEntity.setId(createId(odataEntity, jpaType, false));

    // break the loop?
    if (processedEntities.contains(odataEntity.getId())) {
      throw new EntityAsLinkException(odataEntity.getId());
    }
    processedEntities.add(odataEntity.getId());

    // 2. convert complex types and relationships
    for (final JPASimpleAttribute jpaAttribute : jpaType.getAttributes()) {
      if (jpaAttribute.isKey()) {
        // already processed
        continue;
      }
      final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
      convertJPAAttribute2OData(jpaAttribute, value, jpaType, complexValueBuffer, properties, processedEntities);
    }

    // Olingo JSON serializer seems to support only simple navigation links... no binding links
    odataEntity.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(jpaType, jpaEntity, processedEntities));

    return odataEntity;
  }

  /**
   * Convert an object managed by the {@link EntityManager entity manager} into a
   * OData entity representation.
   *
   */
  public Entity convertJPA2ODataEntity(final JPAEntityType jpaType, final Object jpaEntity)
      throws ODataJPAModelException, ODataJPAConversionException {
    return convertJPA2ODataEntityInternal(jpaType, jpaEntity, new HashSet<URI>());
  }

  private Collection<Link> convertJPAAssociations2ODataLinks(final JPAStructuredType jpaType, final Object jpaObject,
      final Set<URI> processedEntities)
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
      final Link linkNavigation = new Link();
      boolean isLinkValid = false;
      linkNavigation.setTitle(assoziation.getLeaf().getExternalName());
      linkNavigation.setRel(Constants.NS_NAVIGATION_LINK_REL + assoziation.getLeaf().getExternalName());
      if (assoziation.getLeaf().isCollection()) {
        final EntityCollection expandCollection = new EntityCollection();
        for (final Object cEntry : ((Collection<?>) value)) {
          try {
            final Entity expandEntity = convertJPA2ODataEntityInternal((JPAEntityType) jpaAttribute.getStructuredType(),
                cEntry, processedEntities);
            if (expandEntity == null) {
              continue;
            }
            expandCollection.getEntities().add(expandEntity);
          } catch (final EntityAsLinkException e) {
            linkNavigation.getBindingLinks().add(e.getLink().toASCIIString());
            linkNavigation.setType(Constants.ENTITY_COLLECTION_BINDING_LINK_TYPE);
            isLinkValid = true;
          }
        } // for
        if (expandCollection.getEntities().isEmpty()) {
          continue;
        }
        expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
        linkNavigation.setInlineEntitySet(expandCollection);
        linkNavigation.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
        isLinkValid = true;
      } else {
        // no collection -> 1:1
        try {
          final Entity expandEntity = convertJPA2ODataEntityInternal((JPAEntityType) jpaAttribute.getStructuredType(),
              value, processedEntities);
          if (expandEntity == null) {
            continue;
          }
          linkNavigation.setHref(expandEntity.getId().toASCIIString());
          linkNavigation.setInlineEntity(expandEntity);
          linkNavigation.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
        } catch (final EntityAsLinkException e) {
          linkNavigation.setHref(e.getLink().toASCIIString());
          linkNavigation.setBindingLink(e.getLink().toASCIIString());
          linkNavigation.setType(Constants.ENTITY_BINDING_LINK_TYPE);
        }
        isLinkValid = true;
      }
      if (isLinkValid) {
        entityExpandLinks.add(linkNavigation);
      }
    }
    return entityExpandLinks;
  }

  private Property convertJPAComplexAttribute2OData(final JPASimpleAttribute jpaAttribute, final Object value,
      final Set<URI> processedEntities)
          throws ODataJPAModelException, ODataJPAConversionException {
    final JPAStructuredType attributeType = jpaAttribute.getStructuredType();
    if (jpaAttribute.isCollection()) {
      final Collection<?> valuesToProcess = (value == null) ? Collections.emptyList() : (Collection<?>) value;
      final Collection<ComplexValue> convertedValues = new LinkedList<>();
      for (final Object cValue : valuesToProcess) {
        final ComplexValue complexValue = new ComplexValue();
        convertedValues.add(complexValue);
        final List<Property> cvProperties = complexValue.getValue();
        convertComplexTypeValue2OData(attributeType, cValue, cvProperties, processedEntities);
        complexValue.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(attributeType, cValue,
            processedEntities));
      }
      return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
          jpaAttribute.getExternalName(), ValueType.COLLECTION_COMPLEX, convertedValues);
    } else {
      final ComplexValue complexValue = new ComplexValue();
      final List<Property> cvProperties = complexValue.getValue();
      convertComplexTypeValue2OData(attributeType, value, cvProperties, processedEntities);
      complexValue.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(attributeType, value,
          processedEntities));
      return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
          jpaAttribute.getExternalName(), ValueType.COMPLEX, complexValue);
    }
  }

  private void convertComplexTypeValue2OData(final JPAStructuredType attributeType, final Object cValue,
      final List<Property> cvProperties, final Set<URI> processedEntities)
          throws ODataJPAModelException, ODataJPAConversionException {
    if (cValue == null) {
      return;
    }

    final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
    for (final JPASimpleAttribute jpaAttribute : attributeType.getAttributes()) {
      final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(cValue);
      if (jpaAttribute.isComplex()) {
        final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value, processedEntities);
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
