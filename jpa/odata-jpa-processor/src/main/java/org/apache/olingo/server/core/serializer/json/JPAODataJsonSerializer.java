package org.apache.olingo.server.core.serializer.json;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.DecoderException;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.IConstants;
import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Operation;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.DynamicEdmProperty;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.server.ODataFactory;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.core.serializer.utils.ContentTypeHelper;
import org.apache.olingo.server.core.serializer.utils.ExpandSelectHelper;
import org.apache.olingo.server.core.uri.UriHelperImpl;

import com.fasterxml.jackson.core.JsonGenerator;

public class JPAODataJsonSerializer extends ODataJsonSerializer {

  private final boolean isODataMetadataNone;
  private final boolean isODataMetadataFull;
  private IConstants constants;
  private ODataJsonInstanceAnnotationSerializer instanceAnnotSerializer;

  public JPAODataJsonSerializer(final ContentType contentType, final IConstants constants) {
    super(contentType, constants);
    isODataMetadataNone = ContentTypeHelper.isODataMetadataNone(contentType);
    isODataMetadataFull = ContentTypeHelper.isODataMetadataFull(contentType);
    this.constants = constants;
    instanceAnnotSerializer = new ODataJsonInstanceAnnotationSerializer(contentType, constants);
  }

  // called for single complex value
  @Override
  protected void writeProperties(final ServiceMetadata metadata, final EdmStructuredType type,
      final List<Property> properties,
      final SelectOption select, final JsonGenerator json, final Linked linked, final ExpandOption expand)
      throws IOException, SerializerException, DecoderException {
    superWriteProperties(metadata, type, properties, select, json, linked, expand);
    final Set<List<String>> expandedPaths = ExpandSelectHelper.getExpandedItemsPath(expand);
    writeOpenTypeProperties(metadata, type, properties, null, json, expandedPaths, linked, expand);
  }

  protected void superWriteProperties(final ServiceMetadata metadata, final EdmStructuredType type,
      final List<Property> properties,
      final SelectOption select, final JsonGenerator json, Linked linked, ExpandOption expand)
      throws IOException, SerializerException, DecoderException {
    final boolean all = ExpandSelectHelper.isAll(select);
    final Set<String> selected = all ? new HashSet<>() :
        ExpandSelectHelper.getSelectedPropertyNames(select.getSelectItems());
    //    addKeyPropertiesToSelected(selected, type);
    Set<List<String>> expandedPaths = ExpandSelectHelper.getExpandedItemsPath(expand);
    for (final String propertyName : type.getPropertyNames()) {
      if (all || selected.contains(propertyName)) {
        final EdmProperty edmProperty = type.getStructuralProperty(propertyName);
        final Property property = findProperty(propertyName, properties);
        final Set<List<String>> selectedPaths = all || edmProperty.isPrimitive() ? null :
            ExpandSelectHelper.getSelectedPaths(select.getSelectItems(), propertyName);
        writeProperty(metadata, edmProperty, property, selectedPaths, json, expandedPaths, linked, expand);
      }
    }
  }

  private Property findProperty(final String propertyName, final List<Property> properties) {
    for (final Property property : properties) {
      if (propertyName.equals(property.getName())) {
        return property;
      }
    }
    return null;
  }
  
  // called for complex value collection
  @Override
  protected void writeComplexValue(final ServiceMetadata metadata, final EdmComplexType type,
      final List<Property> properties,
      final Set<List<String>> selectedPaths, final JsonGenerator json, final Set<List<String>> expandedPaths,
      final Linked linked,
      final ExpandOption expand, final String complexPropName) throws IOException, SerializerException,
      DecoderException {
    super.writeComplexValue(metadata, type, properties, selectedPaths, json, expandedPaths, linked, expand,
        complexPropName);
    writeOpenTypeProperties(metadata, type, properties, selectedPaths, json, expandedPaths, linked, expand);
  }

  private void writeOpenTypeProperties(final ServiceMetadata metadata, final EdmStructuredType type,
      final List<Property> properties, final Set<List<String>> selectedPaths, final JsonGenerator json,
      final Set<List<String>> expandedPaths, final Linked linked, final ExpandOption expand) throws IOException,
      SerializerException, DecoderException {
    if (!type.isOpenType()) {
      return;
    }
    // write dynamic properties of open type
    for (final Property property : properties) {
      // properties defined in type are not dynamic
      if (type.getProperty(property.getName()) != null) {
        continue;
      }
      if (selectedPaths == null || ExpandSelectHelper.isSelected(selectedPaths, property.getName())) {
//        final EdmProperty edmProperty = ODataFactory.createDynamicEdmProperty(type, property.getName());
        final EdmProperty edmProperty = createDynamicEdmProperty(metadata, type, property);
        writeProperty(metadata, edmProperty, property,
            selectedPaths == null ? null : ExpandSelectHelper.getReducedSelectedPaths(selectedPaths, property
                .getName()), json, expandedPaths, linked, expand);
      }
    }
  }

  private static EdmProperty createDynamicEdmProperty(ServiceMetadata metadata, EdmStructuredType type, Property property) throws SerializerException {
    final String sType = property.getType();
    if(property.getValueType() == null || sType == null || sType.isEmpty()) {
      //fallback to old behavior also compatible with de-serializer
      return ODataFactory.createDynamicEdmProperty(type, property.getName());
    }
    final boolean isCollection;
    switch(property.getValueType()) {
      case COLLECTION_COMPLEX:
      case COLLECTION_ENTITY:
      case COLLECTION_ENUM:
      case COLLECTION_GEOSPATIAL:
      case COLLECTION_PRIMITIVE:
        isCollection = true;
        break;
      default:
        isCollection = false;
    }
    
    FullQualifiedName fqnType = new FullQualifiedName(sType);
    final EdmType pType;
    switch(property.getValueType()) {
      case COLLECTION_COMPLEX:
      case COMPLEX:
        pType = metadata.getEdm().getComplexType(fqnType);
        break;
      case COLLECTION_ENTITY:
      case ENTITY:
        throw new SerializerException("Entities cannot be used as dynamic property type, use complex type instead", SerializerException.MessageKeys.UNSUPPORTED_PROPERTY_TYPE, sType);
      case COLLECTION_ENUM:
      case ENUM:
        pType = metadata.getEdm().getEnumType(fqnType);
        break;
      case COLLECTION_GEOSPATIAL:
      case GEOSPATIAL:
      case COLLECTION_PRIMITIVE:
      case PRIMITIVE:
        final EdmPrimitiveTypeKind kind = EdmPrimitiveTypeKind.valueOfFQN(sType);
        pType = EdmPrimitiveTypeFactory.getInstance(kind);
        break;
      default:
        throw new SerializerException("Cannot determine type for dynamic property value", SerializerException.MessageKeys.UNKNOWN_TYPE, sType);
    }
    //use OUR dynamic property impl
    return new DynamicEdmProperty(property.getName(), pType, isCollection);
  }

  protected void writeEntity(final ServiceMetadata metadata, final EdmEntityType entityType, final Entity entity,
      final ContextURL contextURL, final ExpandOption expand, Integer toDepth, 
      final SelectOption select, final boolean onlyReference, Set<String> ancestors, 
      String name, final JsonGenerator json)
      throws IOException, SerializerException, DecoderException {
    boolean cycle = false;
    if (expand != null) {
      if (ancestors == null) {
        ancestors = new HashSet<>();
      }
      cycle = !ancestors.add(getEntityId(entity, entityType, name));
    }
    try {
      json.writeStartObject();
      if (!isODataMetadataNone) {
        // top-level entity
        if (contextURL != null) {
          writeContextURL(contextURL, json);
          writeMetadataETag(metadata, json);
        }
        if (entity.getETag() != null) {
          json.writeStringField(constants.getEtag(), entity.getETag());
        }
        if (entityType.hasStream()) {
          if (entity.getMediaETag() != null) {
            json.writeStringField(constants.getMediaEtag(), entity.getMediaETag());
          }
          if (entity.getMediaContentType() != null) {
            json.writeStringField(constants.getMediaContentType(), entity.getMediaContentType());
          }
          if (entity.getMediaContentSource() != null) {
            json.writeStringField(constants.getMediaReadLink(), entity.getMediaContentSource().toString());
          }
          if (entity.getMediaEditLinks() != null && !entity.getMediaEditLinks().isEmpty()) {
            json.writeStringField(constants.getMediaEditLink(), entity.getMediaEditLinks().get(0).getHref());
          }
        }
      }
      if (cycle || onlyReference) {
        json.writeStringField(constants.getId(), getEntityId(entity, entityType, name));
      } else {
        final EdmEntityType resolvedType = resolveEntityType(metadata, entityType, entity.getType());
        if ((!isODataMetadataNone && !resolvedType.equals(entityType)) || isODataMetadataFull) {
          json.writeStringField(constants.getType(), "#" + entity.getType());
        }
        if ((!isODataMetadataNone && !areKeyPredicateNamesSelected(select, resolvedType)) || isODataMetadataFull) {
          json.writeStringField(constants.getId(), getEntityId(entity, resolvedType, name));
        }
        
        if (isODataMetadataFull) {
          if (entity.getSelfLink() != null) {
            json.writeStringField(constants.getReadLink(), entity.getSelfLink().getHref());
          }
          if (entity.getEditLink() != null) {
            json.writeStringField(constants.getEditLink(), entity.getEditLink().getHref());
          }
        }
        instanceAnnotSerializer.writeInstanceAnnotationsOnEntity(entity.getAnnotations(), json);        
        writeProperties(metadata, resolvedType, entity.getProperties(), select, json, entity, expand);
        writeNavigationProperties(metadata, resolvedType, entity, expand, toDepth, ancestors, name, json);
        writeOperations(entity.getOperations(), json);      
      }
      json.writeEndObject();
    } finally {
      if (expand != null && !cycle && ancestors != null) {
        ancestors.remove(getEntityId(entity, entityType, name));
      }
    }
  }

  protected void writeEntitySet(final ServiceMetadata metadata, final EdmEntityType entityType,
      final AbstractEntityCollection entitySet, final ExpandOption expand, Integer toDepth, final SelectOption select,
      final boolean onlyReference, final Set<String> ancestors, String name, final JsonGenerator json)
          throws IOException, SerializerException, DecoderException {
    json.writeStartArray();
    for (final Entity entity : entitySet) {
      if (onlyReference) {
        json.writeStartObject();
        json.writeStringField(constants.getId(), getEntityId(entity, entityType, name));
        json.writeEndObject();
      } else {
        writeEntity(metadata, entityType, entity, null, expand, toDepth, select, false, ancestors, name, json);
      }
    }
    json.writeEndArray();
  }

  private void writeOperations(final List<Operation> operations, final JsonGenerator json)
      throws IOException {
    if (isODataMetadataFull) {
      for (Operation operation : operations) {
        json.writeObjectFieldStart(operation.getMetadataAnchor());
        json.writeStringField(Constants.ATTR_TITLE, operation.getTitle());
        json.writeStringField(Constants.ATTR_TARGET, operation.getTarget().toASCIIString());
        json.writeEndObject();
      }
    }
  }
  
  //Modified
  private String getEntityId(Entity entity, EdmEntityType entityType, String name) throws SerializerException {
    if(entity != null && entity.getId() == null) {
      if(entityType == null || entityType.getKeyPredicateNames() == null 
          || name == null) {
        throw new SerializerException("Entity id is null.", SerializerException.MessageKeys.MISSING_ID);
      } else if(!areAllKeyAttributesPresent(entity, entityType)) {
        //avoid exception from missing id
        return null;
      }
      else {
        final UriHelper uriHelper = new UriHelperImpl(); 
        entity.setId(URI.create(name + '(' + uriHelper.buildKeyPredicate(entityType, entity) + ')'));
      }
    }
    return entity.getId().toASCIIString();
  }
  
  protected final boolean areAllKeyAttributesPresent(final Entity odataEntity, EdmEntityType entityType) {
    final List<String> keyNames = entityType.getKeyPredicateNames();
    for(String key: keyNames) {
      if(odataEntity.getProperty(key) == null) {
        return false;
      }
    }
    return true;
  }
  
  private boolean areKeyPredicateNamesSelected(SelectOption select, EdmEntityType type) {
    if (select == null || ExpandSelectHelper.isAll(select)) {
      return true;
    }
    final Set<String> selected = ExpandSelectHelper.getSelectedPropertyNames(select.getSelectItems());
    for (String key : type.getKeyPredicateNames()) {
      if (!selected.contains(key)) {
        return false;
      }
    }
    return true;
  }
  
}
