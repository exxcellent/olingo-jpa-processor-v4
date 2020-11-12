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
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

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
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.Pair;
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

  private static class JPA2ODataProcessingContext {
    /**
     * <code>null</code> for processing of an root/(initial) entry entity otherwise the
     * <i>jpaEntity</i> will be the target of an relationship identified by <i>currentRelationship</i>
     */
    private JPAAssociationAttribute currentRelationship = null;
    private Set<Object> processedEntities = new HashSet<>();
    private final List<JPAAssociationAttribute> processedRelationships = Collections.emptyList();

    JPA2ODataProcessingContext createSubContext(final JPAAssociationAttribute workingRelationShip) {
      final JPA2ODataProcessingContext subContext = new JPA2ODataProcessingContext();
      subContext.currentRelationship = workingRelationShip;
      subContext.processedEntities = new HashSet<>(this.processedEntities);
      return subContext;
    }
  }

  // ------------------------------------------------------------------------------------
  public EntityConverter(final UriHelper uriHelper,
      final IntermediateServiceDocument sd, final ServiceMetadata serviceMetadata)
          throws ODataJPAModelException {
    super(uriHelper, sd, serviceMetadata);
  }

  @SuppressWarnings({ "null" })
  private Object convertODataBindingLink2JPAAssociationProperty(final Object targetJPAObject,
      final JPAAssociationAttribute association, final Entity odataOwnerEntity, final Link odataLink,
      final Map<String, Pair<Entity, Object>> mapId2Instance) throws ODataJPAModelException,
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
          final Pair<?, Object> pair = mapId2Instance.get(bindingId);
          final Object target = pair != null ? pair.getRight() : null;
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

  private Object convertODataNavigationLink2JPAAssociationProperty(final Object owningJPAInstance,
      final JPAAssociationAttribute association, final Link odataLink,
      final Map<String, Pair<Entity, Object>> mapId2Instance)
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
        final Object associationTargetJPAInstance = convertOData2JPAEntityInternal(childEntity, jpaType,
            mapId2Instance);
        manageOData2JPABacklink(owningJPAInstance, association, associationTargetJPAInstance);
        list.add(associationTargetJPAInstance);
      }
      result = list;
    } else {
      // single value
      final Entity childEntity = odataLink.getInlineEntity();
      if(childEntity == null) {
        return null;
      }
      final JPAStructuredType jpaType = getIntermediateServiceDocument().getEntityType(new FullQualifiedName(childEntity
          .getType()));
      result = convertOData2JPAEntityInternal(childEntity, jpaType, mapId2Instance);
      manageOData2JPABacklink(owningJPAInstance, association, result);
    }
    // assign to owner entity
    association.getAttributeAccessor().setPropertyValue(owningJPAInstance, result);
    return result;
  }

  private void manageOData2JPABacklink(final Object owningJPAInstance, final JPAAssociationAttribute association,
      final Object associationTargetJPAInstance) throws ODataJPAModelException {
    final JPAAssociationAttribute associationBidirectionalOpposite = association.getBidirectionalOppositeAssociation();
    if (associationBidirectionalOpposite == null) {
      return;
    }
    if (associationBidirectionalOpposite.isCollection()) {
      @SuppressWarnings("unchecked")
      final Collection<Object> oppositeTargets = (Collection<Object>) associationBidirectionalOpposite
      .getAttributeAccessor()
      .getPropertyValue(associationTargetJPAInstance);
      if (oppositeTargets == null) {
        LOG.log(Level.FINE,
            "Collection for backlink (" + "--[" + associationBidirectionalOpposite.getExternalName() + "]->"
                + associationBidirectionalOpposite
                .getTargetEntity().getExternalName() + ") is NULL, cannot manage backlink for you!");
        return;
      }
      if (!oppositeTargets.contains(owningJPAInstance)) {
        oppositeTargets.add(owningJPAInstance);
      }
    } else {
      // check presence of backlink or set it (if single value target only)
      final Object backlinkValue = associationBidirectionalOpposite.getAttributeAccessor().getPropertyValue(
          associationTargetJPAInstance);
      if (backlinkValue == null) {
        associationBidirectionalOpposite.getAttributeAccessor().setPropertyValue(associationTargetJPAInstance,
            owningJPAInstance);
      }
    }
  }

  /**
   * Convert a collection of OData entities into a JPA entities. This is the <b>preferred method</b> for collection
   * because relationships between that entities can be handled properly.
   */
  public Collection<Object> convertOData2JPAEntity(final EntityCollection odataEntities,
      final JPAStructuredType jpaEntityType)
          throws ODataJPAModelException, ODataJPAConversionException {
    final HashMap<String, Pair<Entity, Object>> cacheMap = new HashMap<>();
    final List<Object> jpaEntities = new ArrayList<>(odataEntities.getEntities().size());
    for (final Entity odataEntity : odataEntities) {
      final Object jpaEntity = convertOData2JPAEntityInternal(odataEntity, jpaEntityType, cacheMap);
      jpaEntities.add(jpaEntity);
    }
    return jpaEntities;
  }

  /**
   * Convert a OData entity into a JPA entity.
   * @see #convertOData2JPAEntity(EntityCollection, JPAStructuredType)
   */
  public Object convertOData2JPAEntity(final Entity entity, final JPAStructuredType jpaEntityType)
      throws ODataJPAModelException, ODataJPAConversionException {
    return convertOData2JPAEntityInternal(entity, jpaEntityType, new HashMap<String, Pair<Entity, Object>>());
  }

  /**
   * Flat comparison to ensure that both entities contains the same values. That means the entities must have:
   * <ul>
   * <li>same id</li>
   * <li>same number of properties and property values</li>
   * <li>same number of links and link values</li>
   * </ul>
   */
  private void checkEntitiesMustBeIdentical(final Entity a, final Entity b, final String errorId)
      throws ODataJPAConversionException {
    if (!Objects.equals(a, b)) {
      throw new ODataJPAConversionException(HttpStatusCode.CONFLICT,
          ODataJPAConversionException.MessageKeys.RUNTIME_PROBLEM, "Found shared instance for '" + errorId
          + "', but is modified and cannot be merged");
    }
  }

  private Object convertOData2JPAEntityInternal(final Entity entity, final JPAStructuredType jpaEntityType,
      final Map<String, Pair<Entity, Object>> mapId2Instance)
          throws ODataJPAModelException, ODataJPAConversionException {
    if (!jpaEntityType.getExternalFQN().getFullQualifiedNameAsString().equals(entity.getType())) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INVALID_ENTITY_TYPE,
          jpaEntityType.getExternalFQN().getFullQualifiedNameAsString());
    }
    try {
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
      final Object targetJPAInstance;
      if (id == null) {
        targetJPAInstance = newJPAInstance(jpaEntityType);
      } else if (mapId2Instance.containsKey(id.toASCIIString())) {
        // shared instance, optimize handling -> skip merging (take first created instance)
        final Pair<Entity, Object> existing = mapId2Instance.get(id.toASCIIString());
        checkEntitiesMustBeIdentical(entity, existing.getLeft(), id.toASCIIString());
        return existing.getRight();
      } else {
        targetJPAInstance = newJPAInstance(jpaEntityType);
        mapId2Instance.put(id.toASCIIString(), new Pair<Entity, Object>(entity, targetJPAInstance));
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

  private Property convertJPAAttribute2OData(final JPAMemberAttribute jpaAttribute, final Object value,
      final JPAStructuredType jpaType, final Map<String, Object> complexValueBuffer, final List<Property> properties,
      final JPA2ODataProcessingContext processingContext) throws ODataJPAConversionException, ODataJPAModelException {
    if (jpaAttribute.isAssociation()) {
      // couldn't be happen
      throw new IllegalStateException();
    } else if (jpaAttribute.isComplex()) {
      if (jpaAttribute.isKey()) {
        throw new IllegalStateException("Do not call this method for @EmbeddedId attributes, like "+jpaAttribute.getInternalName());
      }
      // complex keys (aka @EmbeddedId) must be exploded into simple attribute values
      final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value, processingContext);
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
      final JPA2ODataProcessingContext processingContext)
          throws ODataJPAModelException, ODataJPAConversionException, EntityAsLinkException {
    final Entity odataEntity = new Entity();
    odataEntity.setType(jpaType.getExternalFQN().getFullQualifiedNameAsString());
    final List<Property> properties = odataEntity.getProperties();

    final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
    // 1. convert key attributes to create an id for the entity
    for (final JPAMemberAttribute jpaAttribute : jpaType.getKeyAttributes(false)) {
      if (jpaAttribute.isComplex()) {
        // for @EmbeddedId
        // transfer nested key attribute values to owning oadata entity
        final JPAStructuredType keyType = jpaAttribute.getStructuredType();
        final Object keyObject = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
        for (final JPAMemberAttribute nestedAttribute : keyType.getAttributes()) {
          final Object value = nestedAttribute.getAttributeAccessor().getPropertyValue(keyObject);
          convertJPAAttribute2OData(nestedAttribute, value, keyType, complexValueBuffer, properties, processingContext);
        }
      } else {
        final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
        convertJPAAttribute2OData(jpaAttribute, value, jpaType, complexValueBuffer, properties, processingContext);
      }
    }

    // id of entity must be set before relationships are processed
    odataEntity.setId(createId(odataEntity, jpaType, false));

    if (isBacklinkOfProcessedRelationship(processingContext)) {
      // suppress created odata entity in response, use navigation link as representation instead
      throw new EntityAsLinkException(odataEntity.getId());
    }

    // break the loop?
    if (processingContext.processedEntities.contains(jpaEntity)) {
      throw new EntityAsLinkException(odataEntity.getId());
    }
    processingContext.processedEntities.add(jpaEntity);

    // 2. convert other simple attributes, complex types and relationships
    for (final JPAMemberAttribute jpaAttribute : jpaType.getAttributes()) {
      if (jpaAttribute.isKey()) {
        // already processed
        continue;
      }
      final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(jpaEntity);
      convertJPAAttribute2OData(jpaAttribute, value, jpaType, complexValueBuffer, properties, processingContext);
    }

    // Olingo JSON serializer seems to support only simple navigation links... no binding or association links
    odataEntity.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(jpaType, jpaEntity, processingContext));

    return odataEntity;
  }

  /**
   * Convert an object managed by the {@link EntityManager entity manager} into a
   * OData entity representation.
   *
   */
  public Entity convertJPA2ODataEntity(final JPAEntityType jpaType, final Object jpaEntity)
      throws ODataJPAModelException, ODataJPAConversionException {
    return convertJPA2ODataEntityInternal(jpaType, jpaEntity, new JPA2ODataProcessingContext());
  }

  /**
   *
   * @return TRUE if <i>processingContext.currentRelationship</i> is a relationship with the opposite direction
   * (backlink aka inverse
   * direction) for any relationship in the list of already processed relationships in
   * <i>processingContext.processedRelationships</i>
   */
  private boolean isBacklinkOfProcessedRelationship(final JPA2ODataProcessingContext processingContext) {
    if (processingContext.currentRelationship == null || processingContext.processedRelationships == null) {
      return false;
    }
    for (final JPAAssociationAttribute entry : processingContext.processedRelationships) {
      final JPAAssociationAttribute backlink = entry.getBidirectionalOppositeAssociation();
      if (backlink == null) {
        continue;
      }
      if (backlink == processingContext.currentRelationship) {
        return true;
      }
    }
    return false;
  }

  private Collection<Link> convertJPAAssociations2ODataLinks(final JPAStructuredType jpaType, final Object jpaObject,
      final JPA2ODataProcessingContext processingContext)
          throws ODataJPAModelException, ODataJPAConversionException {
    final List<Link> entityExpandLinks = new LinkedList<Link>();
    for (final JPAAssociationAttribute relationship : jpaType.getAssociations()) {
      final JPA2ODataProcessingContext subContext = processingContext.createSubContext(relationship);
      final JPAAssociationPath assoziation = jpaType.getDeclaredAssociation(relationship.getExternalName());
      final Object value = relationship.getAttributeAccessor().getPropertyValue(jpaObject);
      if (!relationship.isAssociation()) {
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
            final Entity expandEntity = convertJPA2ODataEntityInternal((JPAEntityType) relationship.getStructuredType(),
                cEntry, subContext);
            if (expandEntity == null) {
              continue;
            }
            expandCollection.getEntities().add(expandEntity);
          } catch (final EntityAsLinkException e) {
            // Olingo serializer will ignore these binding links without 'href'
            linkNavigation.getBindingLinks().add(e.getLink().toASCIIString());
            linkNavigation.setType(Constants.ENTITY_COLLECTION_BINDING_LINK_TYPE);
            isLinkValid = true;
          }
        } // for
        if (!expandCollection.getEntities().isEmpty()) {
          expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
          linkNavigation.setInlineEntitySet(expandCollection);
          linkNavigation.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
          isLinkValid = true;
        }
      } else {
        // no collection -> 1:1
        try {
          final Entity expandEntity = convertJPA2ODataEntityInternal((JPAEntityType) relationship.getStructuredType(),
              value, subContext);
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

  private Property convertJPAComplexAttribute2OData(final JPAMemberAttribute jpaAttribute, final Object value,
      final JPA2ODataProcessingContext processingContext)
          throws ODataJPAModelException, ODataJPAConversionException {
    final JPAStructuredType attributeType = jpaAttribute.getStructuredType();
    if (jpaAttribute.isCollection()) {
      final Collection<?> valuesToProcess = (value == null) ? Collections.emptyList() : (Collection<?>) value;
      final Collection<ComplexValue> convertedValues = new LinkedList<>();
      for (final Object cValue : valuesToProcess) {
        final ComplexValue complexValue = new ComplexValue();
        convertedValues.add(complexValue);
        final List<Property> cvProperties = complexValue.getValue();
        convertComplexTypeValue2OData(attributeType, cValue, cvProperties, processingContext);
        complexValue.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(attributeType, cValue,
            processingContext));
      }
      return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
          jpaAttribute.getExternalName(), ValueType.COLLECTION_COMPLEX, convertedValues);
    } else {
      final ComplexValue complexValue = new ComplexValue();
      final List<Property> cvProperties = complexValue.getValue();
      convertComplexTypeValue2OData(attributeType, value, cvProperties, processingContext);
      complexValue.getNavigationLinks().addAll(convertJPAAssociations2ODataLinks(attributeType, value,
          processingContext));
      return new Property(attributeType.getExternalFQN().getFullQualifiedNameAsString(),
          jpaAttribute.getExternalName(), ValueType.COMPLEX, complexValue);
    }
  }

  private void convertComplexTypeValue2OData(final JPAStructuredType attributeType, final Object cValue,
      final List<Property> cvProperties, final JPA2ODataProcessingContext processingContext)
          throws ODataJPAModelException, ODataJPAConversionException {
    if (cValue == null) {
      return;
    }

    final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
    for (final JPAMemberAttribute jpaAttribute : attributeType.getAttributes()) {
      final Object value = jpaAttribute.getAttributeAccessor().getPropertyValue(cValue);
      if (jpaAttribute.isComplex()) {
        final Property complexTypeProperty = convertJPAComplexAttribute2OData(jpaAttribute, value, processingContext);
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
