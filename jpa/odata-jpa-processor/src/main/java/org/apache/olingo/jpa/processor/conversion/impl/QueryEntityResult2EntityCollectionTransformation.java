package org.apache.olingo.jpa.processor.conversion.impl;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.api.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.conversion.Transformation;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.DatabaseQueryResult2ODataEntityConverter;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.CountOption;

public class QueryEntityResult2EntityCollectionTransformation implements
Transformation<QueryEntityResult, EntityCollection> {

  @Inject
  private final JPAODataGlobalContext context = null;
  @Inject
  private final UriInfoResource uriResource = null;

  @Override
  public Class<QueryEntityResult> getInputType() {
    return QueryEntityResult.class;
  }

  @Override
  public Class<EntityCollection> getOutputType() {
    return EntityCollection.class;
  }

  @Override
  public EntityCollection transform(final QueryEntityResult input) throws SerializerException {
    if (context == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (uriResource == null) {
      throw new IllegalStateException("Dependency injection not working: " + UriInfoResource.class.getSimpleName()
          + " exprected");
    }
    try {
      return convertToEntityCollection(input);
    } catch (final ODataApplicationException e) {
      throw new SerializerException("", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  private EntityCollection convertToEntityCollection(final QueryEntityResult result) throws ODataApplicationException {
    // Convert tuple result into an OData Result
    EntityCollection entityCollection;
    try {
      entityCollection = new DatabaseQueryResult2ODataEntityConverter(context.getEdmProvider()
          .getServiceDocument(), context.getOdata().createUriHelper(), context.getServiceMetaData())
          .convertDBTuple2OData(result);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }

    // Count results if requested
    final CountOption countOption = uriResource/* getNavigation().getLastStep() */.getCountOption();
    if (countOption != null && countOption.getValue()) {
      entityCollection.setCount(Integer.valueOf(entityCollection.getEntities().size()));
    }

    return entityCollection;
  }
}
