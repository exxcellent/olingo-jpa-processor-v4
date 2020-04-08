package org.apache.olingo.jpa.processor.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.AbstractEntityQueryResult;
import org.apache.olingo.jpa.processor.core.query.result.ExpandQueryEntityResult;
import org.apache.olingo.jpa.processor.core.query.result.QueryElementCollectionResult;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

public class DatabaseQueryResult2ODataEntityConverter extends AbstractEntityConverter {

  public DatabaseQueryResult2ODataEntityConverter(final IntermediateServiceDocument sd, final UriHelper uriHelper,
      final ServiceMetadata serviceMetadata) throws ODataJPAModelException,
  ODataApplicationException {
    super(uriHelper, sd, serviceMetadata);
  }

  public EntityCollection convertDBTuple2OData(final QueryEntityResult jpaQueryResult)
      throws ODataJPAModelException, ODataJPAConversionException {
    final EntityCollection odataEntityCollection = new EntityCollection();

    for (final Tuple row : jpaQueryResult.getQueryResult()) {
      final Entity odataEntity = convertTuple2ODataEntity(row, jpaQueryResult);
      odataEntityCollection.getEntities().add(odataEntity);
    }
    return odataEntityCollection;
  }

  @SuppressWarnings("null")
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

  /**
   * The given row is converted into a OData entity and added to the map of
   * already processed entities.<br/>
   * If the <code>row</code> is defining a entity that is already part of entity
   * collection (that happens if the {@link QueryEntityResult} is built from joined
   * tables) then the entity values are merged into the exiting entity and hat
   * already processed entity is returned; otherwise a new entity is created.
   *
   * @param alreadyProcessedEntities
   * The map containing all already converted
   * entities. The key in the map is the
   * stringified entity id.
   */
  private final Entity convertTuple2ODataEntity(final Tuple row, final AbstractEntityQueryResult jpaQueryResult)
      throws ODataJPAModelException, ODataJPAConversionException {
    final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
    final Entity odataEntity = new Entity();

    final JPAEntityType jpaEntityType = jpaQueryResult.getEntityType();
    odataEntity.setType(jpaEntityType.getExternalFQN().getFullQualifiedNameAsString());

    try {
      if (jpaEntityType.hasStream()) {
        odataEntity.setMediaContentType(determineContentType(jpaEntityType, row));
      } else {
        // for XML type must not be null
        odataEntity.setMediaContentType("");
      }
    } catch (final ODataJPAModelException e) {
      LOG.log(Level.WARNING, "Couldn't set media stream on entity type " + jpaEntityType.getExternalName(),
          e);
    }

    final List<Property> properties = odataEntity.getProperties();
    // TODO store @Version to fill ETag Header
    for (final TupleElement<?> element : row.getElements()) {
      convertJPAValue2ODataAttribute(row.get(element.getAlias()), element.getAlias(), "",
          jpaEntityType, complexValueBuffer, 0, properties);
    }

    createElementCollections(odataEntity, row, jpaQueryResult);

    odataEntity.setId(createId(odataEntity, jpaEntityType, false));

    // expands for (direct) relationship attributes
    for (final JPAAssociationPath association : jpaEntityType.getAssociationPathList()) {
      if (association.getPathElements().size() > 1) {
        // paths > 1 means a complex values is involved... this handled in code below
        continue;
      }
      if (!association.getLeaf().isAssociation()) {
        continue;
      }
      // consume complete by removing so we can check at the end whether all expands were processed
      final ExpandQueryEntityResult expandResult = jpaQueryResult.getExpandChildren().get(association);
      odataEntity.getNavigationLinks().addAll(createExpands(row, expandResult));
    }
    // expands for (nested) complex type attributes
    for (final String attribute : complexValueBuffer.keySet()) {
      if (attribute.indexOf(JPASelector.PATH_SEPERATOR) > -1) {
        // if the attribute name is an path to an nested complex type, then we can
        // continue, because the nested complex type is also a property in one of the
        // higher level attributes (without / in the key name)
        continue;
      }
      final Object entry = complexValueBuffer.get(attribute);
      if (entry instanceof ComplexValue) {
        final Collection<? extends Link> expands = createExpandsForComplexValue(attribute, row, jpaQueryResult);
        ((ComplexValue) entry).getNavigationLinks().addAll(expands);
      } else if (entry instanceof List) {
        @SuppressWarnings("unchecked")
        final List<ComplexValue> cvList = (List<ComplexValue>) entry;
        for (final ComplexValue cv : cvList) {
          final Collection<? extends Link> expands = createExpandsForComplexValue(attribute, row, jpaQueryResult);
          cv.getNavigationLinks().addAll(expands);
        }
      }
    }
    return odataEntity;
  }

  private Collection<? extends Link> createExpands(final Tuple owningEntityRow,
      final ExpandQueryEntityResult expandResult)
          throws ODataJPAModelException, ODataJPAConversionException {
    if (expandResult == null) {
      return Collections.emptyList();
    }
    // TODO Check how to convert Organizations('3')/AdministrativeInformation?$expand=Created/User
    final Link expand = convertTuples2ExpandLink(owningEntityRow, expandResult);
    if (expand == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(expand);
  }

  private Collection<? extends Link> createExpandsForComplexValue(final String complexValueAttributeName,
      final Tuple row,
      final AbstractEntityQueryResult jpaQueryResult) throws ODataJPAModelException, ODataJPAConversionException {
    final JPAEntityType jpaEntityType = jpaQueryResult.getEntityType();
    final JPASelector selector = jpaEntityType.getPath(complexValueAttributeName);
    if (selector == null) {
      LOG.log(Level.WARNING, "Problems to handle $expand for complex value '" + complexValueAttributeName + "' of "
          + jpaEntityType.getInternalName());
      return Collections.emptyList();
    }
    if (selector.getPathElements().size() > 1) {
      LOG.log(Level.WARNING, "Problems to handle $expand for complex value '" + complexValueAttributeName + "' of "
          + jpaEntityType.getInternalName());
      return Collections.emptyList();
    }
    final Collection<Link> expands = new LinkedList<Link>();
    final JPAAttribute<?> iAttribute = selector.getLeaf();
    final JPAStructuredType iComplexType = iAttribute.getStructuredType();
    // build expands for all relationships of complex type
    for (final JPAAssociationPath cvAssociation : iComplexType.getAssociationPathList()) {
      final String assoPath = complexValueAttributeName.concat(JPASelector.PATH_SEPERATOR).concat(cvAssociation
          .getAlias());
      final JPAAssociationPath association = jpaEntityType.getAssociationPath(assoPath);
      final ExpandQueryEntityResult expandResult = jpaQueryResult.getExpandChildren().get(association);
      expands.addAll(createExpands(row, expandResult));
    }
    return expands;
  }

  private Link convertTuples2ExpandLink(final Tuple owningEntityRow, final ExpandQueryEntityResult jpaExpandResult)
      throws ODataJPAModelException, ODataJPAConversionException {

    final JPAAssociationPath assoziation = jpaExpandResult.getNavigationPath();
    final EntityCollection expandCollection = createEntityCollection(owningEntityRow, jpaExpandResult);
    if (expandCollection.getEntities().isEmpty()) {
      return null;
    }
    final Link link = new Link();
    link.setTitle(assoziation.getLeaf().getExternalName());
    link.setRel(Constants.NS_ASSOCIATION_LINK_REL + link.getTitle());
    link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
    if (assoziation.getLeaf().isCollection()) {
      link.setInlineEntitySet(expandCollection);
      expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
    } else {
      if (expandCollection.getEntities() != null && !expandCollection.getEntities().isEmpty()) {
        final Entity expandEntity = expandCollection.getEntities().get(0);
        link.setInlineEntity(expandEntity);
        link.setHref(expandEntity.getId().toASCIIString());
      }
    }
    return link;
  }

  private EntityCollection createEntityCollection(final Tuple owningEntityRow,
      final ExpandQueryEntityResult jpaExpandResult)
          throws ODataJPAModelException, ODataJPAConversionException {

    final String owningEntityKey = jpaExpandResult.getNavigationKeyBuilder().buildKeyForNavigationOwningRow(
        owningEntityRow);
    final EntityCollection odataEntityCollection = new EntityCollection();
    final List<Tuple> subResult = jpaExpandResult.getAssociationResult(owningEntityKey);
    if (subResult != null) {
      for (final Tuple row : subResult) {
        final Entity entity = convertTuple2ODataEntity(row, jpaExpandResult);
        odataEntityCollection.getEntities().add(entity);
      }
    }
    return odataEntityCollection;
  }

  /**
   * Create entries for all @ElementCollection attributes and assign they as
   * {@link Property} to given <i>entity</i>.
   *
   * @param entityODataTarget
   * The entity to create element collection properties for
   * @throws ODataApplicationException
   */
  private void createElementCollections(final Entity entityODataTarget, final Tuple owningEntityRow,
      final AbstractEntityQueryResult jpaQueryResult) throws ODataJPAModelException, ODataJPAConversionException {
    final Map<JPAAttribute<?>, QueryElementCollectionResult> elementCollections = jpaQueryResult
        .getElementCollections();
    for (final Entry<JPAAttribute<?>, QueryElementCollectionResult> entry : elementCollections.entrySet()) {

      final String key = entry.getValue().getNavigationKeyBuilder().buildKeyForNavigationOwningRow(owningEntityRow);

      final Map<String, Object> complexValueBuffer = new HashMap<String, Object>();
      int index = -1;
      final List<Tuple> tuples = entry.getValue().getDirectMappingsResult(key);
      if (tuples == null) {
        continue;
      }
      for (final Tuple row : tuples) {
        index++;
        for (final TupleElement<?> element : row.getElements()) {
          /* final Property property = */ convertJPAValue2ODataAttribute(row.get(element.getAlias()),
              element.getAlias(), "", jpaQueryResult.getEntityType(), complexValueBuffer, index,
              entityODataTarget.getProperties());
        }

      }
    }
  }

}
