package org.apache.olingo.jpa.processor.core.query;

import java.util.List;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.ExpandQueryEntityResult;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

class JPATupleExpandResultConverter extends JPATupleAbstractConverter {

  private final Tuple owningEnityRow;
  private final JPAAssociationPath assoziation;

  public JPATupleExpandResultConverter(final JPAEntityType jpaTargetEntity, final Tuple parentRow,
      final JPAAssociationPath assoziation, final UriHelper uriHelper, final IntermediateServiceDocument sd,
      final ServiceMetadata serviceMetadata) {

    super(jpaTargetEntity, uriHelper, sd, serviceMetadata);
    this.owningEnityRow = parentRow;
    this.assoziation = assoziation;
  }

  public Link convertTuples2ExpandLink(final ExpandQueryEntityResult jpaExpandResult)
      throws ODataJPAModelException, ODataJPAConversionException {
    final Link link = new Link();
    link.setTitle(assoziation.getLeaf().getExternalName());
    link.setRel(Constants.NS_ASSOCIATION_LINK_REL + link.getTitle());
    link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
    final EntityCollection expandCollection = createEntityCollection(jpaExpandResult);
    if (assoziation.getLeaf().isCollection()) {
      link.setInlineEntitySet(expandCollection);
      expandCollection.setCount(Integer.valueOf(expandCollection.getEntities().size()));
      // TODO link.setHref(parentUri.toASCIIString());
    } else {
      if (expandCollection.getEntities() != null && !expandCollection.getEntities().isEmpty()) {
        final Entity expandEntity = expandCollection.getEntities().get(0);
        link.setInlineEntity(expandEntity);
        link.setHref(createId(expandEntity, jpaExpandResult.getEntityType(), getUriHelper()).toASCIIString());
      }
    }
    return link;
  }

  private EntityCollection createEntityCollection(final ExpandQueryEntityResult jpaExpandResult)
      throws ODataJPAModelException, ODataJPAConversionException {

    final String owningEntityKey = jpaExpandResult.getNavigationKeyBuilder().buildKeyForNavigationOwningRow(
        owningEnityRow);
    final EntityCollection odataEntityCollection = new EntityCollection();
    final List<Tuple> subResult = jpaExpandResult.getDirectMappingsResult(owningEntityKey);
    if (subResult != null) {

      for (final Tuple row : subResult) {
        final Entity entity = convertTuple2ODataEntity(row, jpaExpandResult);
        odataEntityCollection.getEntities().add(entity);
      }
    }
    // TODO odataEntityCollection.setId(createId());
    return odataEntityCollection;
  }

}
