package org.apache.olingo.jpa.processor.transformation.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.core.api.QueryResponseCustomizer;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.query.DatabaseQueryResult2ODataEntityConverter;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.TransformationContextRequirement;
import org.apache.olingo.jpa.processor.transformation.TransformationDeclaration;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class QueryEntityResult2EntityCollectionTransformation implements
Transformation<QueryEntityResult, EntityCollection> {

  public final static TransformationDeclaration<QueryEntityResult, EntityCollection> DEFAULT_DECLARATION =
      new TransformationDeclaration<>(
          QueryEntityResult.class, EntityCollection.class, 
          new TransformationContextRequirement(JPAODataGlobalContext.class), 
          new TransformationContextRequirement(UriInfoResource.class),
          new TransformationContextRequirement(QueryResponseCustomizer.class)
      );

  private final Logger log = Logger.getLogger(Transformation.class.getName());
  
  @Inject
  private final JPAODataGlobalContext globalContext = null;
  @Inject
  private final UriInfoResource uriResource = null;
  @Inject
  private QueryResponseCustomizer responseCustomizer;

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
    if (globalContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (uriResource == null) {
      throw new IllegalStateException("Dependency injection not working: " + UriInfoResource.class.getSimpleName()
          + " expected");
    }
    try {
      EntityCollection result = convertToEntityCollection(input);
      if(responseCustomizer != null) {
        log.log(Level.FINE, "Response customizer present, modify entity collection before building response from it...");
        EntityCollection modifiedCollection = responseCustomizer.customizeResult(globalContext.getServiceMetaData().getEdm(), input.getEntityType().getTypeClass(), result);
        if(modifiedCollection != null) {
          result = modifiedCollection;
        }
      }
      return result;      
    } catch (final ODataApplicationException e) {
      throw new SerializerException("", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  private EntityCollection convertToEntityCollection(final QueryEntityResult result) throws ODataApplicationException {
    // Convert tuple result into an OData Result
    try {
      return new DatabaseQueryResult2ODataEntityConverter(globalContext.getEdmProvider()
          .getServiceDocument(), globalContext.getOdata().createUriHelper(), globalContext.getServiceMetaData())
          .convertDBTuple2OData(result);
    } catch (final ODataJPAModelException e) {
      throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.QUERY_RESULT_CONV_ERROR,
          HttpStatusCode.INTERNAL_SERVER_ERROR, e);
    }
  }
}
