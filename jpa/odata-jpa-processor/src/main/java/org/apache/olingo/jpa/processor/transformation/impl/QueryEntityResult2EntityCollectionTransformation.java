package org.apache.olingo.jpa.processor.transformation.impl;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.DatabaseQueryResult2ODataEntityConverter;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.queryoption.CountOption;

public class QueryEntityResult2EntityCollectionTransformation implements
Transformation<QueryEntityResult, EntityCollection> {

  @Inject
  private final JPAODataGlobalContext globalContext = null;
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

  @SuppressWarnings("unchecked")
  @Override
  public <I> Transformation<I, EntityCollection> createSubTransformation(final Class<I> newStart)
      throws SerializerException {
    if (newStart.isAssignableFrom(getInputType())) {
      return (Transformation<I, EntityCollection>) this;
    }
    throw new SerializerException("No sub transformation possible", SerializerException.MessageKeys.UNSUPPORTED_FORMAT);
  }

  @Override
  public EntityCollection transform(final QueryEntityResult input) throws SerializerException {
    if (globalContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (uriResource == null) {
      throw new IllegalStateException("Dependency injection not working: " + UriInfoResource.class.getSimpleName()
          + " expected");
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
      entityCollection = new DatabaseQueryResult2ODataEntityConverter(globalContext.getEdmProvider()
          .getServiceDocument(), globalContext.getOdata().createUriHelper(), globalContext.getServiceMetaData())
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
