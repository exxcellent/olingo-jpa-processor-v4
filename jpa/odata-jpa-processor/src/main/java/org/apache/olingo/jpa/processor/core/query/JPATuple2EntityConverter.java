package org.apache.olingo.jpa.processor.core.query;

import javax.persistence.Tuple;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriHelper;

class JPATuple2EntityConverter extends JPATupleAbstractConverter {

  public JPATuple2EntityConverter(final IntermediateServiceDocument sd, final JPAEntityType jpaTargetEntity,
      final UriHelper uriHelper, final ServiceMetadata serviceMetadata) throws ODataJPAModelException,
  ODataApplicationException {
    super(jpaTargetEntity, uriHelper, sd, serviceMetadata);
  }

  public EntityCollection convertQueryResult(final QueryEntityResult jpaQueryResult)
      throws ODataJPAModelException, ODataJPAConversionException {
    final EntityCollection odataEntityCollection = new EntityCollection();

    for (final Tuple row : jpaQueryResult.getQueryResult()) {
      final Entity odataEntity = convertTuple2ODataEntity(row, jpaQueryResult);
      odataEntityCollection.getEntities().add(odataEntity);
    }
    return odataEntityCollection;
  }

}
