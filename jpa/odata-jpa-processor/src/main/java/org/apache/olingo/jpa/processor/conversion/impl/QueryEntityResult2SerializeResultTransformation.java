package org.apache.olingo.jpa.processor.conversion.impl;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.processor.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.conversion.Transformation;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;

public class QueryEntityResult2SerializeResultTransformation implements
Transformation<QueryEntityResult, SerializerResult> {

  @Inject
  private final JPAODataSessionContextAccess context = null;
  @Inject
  private RepresentationType outputStructure;
  @Inject
  private ContentType responseFormat;


  @Override
  public Class<QueryEntityResult> getInputType() {
    return QueryEntityResult.class;
  }

  @Override
  public Class<SerializerResult> getOutputType() {
    return SerializerResult.class;
  }

  @Override
  public SerializerResult transform(final QueryEntityResult input) throws SerializerException {
    if (outputStructure == null) {
      throw new SerializerException("Missing configuration value", SerializerException.MessageKeys.IO_EXCEPTION);
    }
    if (responseFormat == null) {
      throw new SerializerException("Missing configuration value", SerializerException.MessageKeys.IO_EXCEPTION);
    }

    final QueryEntityResult2EntityCollectionTransformation step1 =
        new QueryEntityResult2EntityCollectionTransformation();
    // configure
    try {
      context.getDependencyInjector().injectFields(step1);
    } catch (final ODataApplicationException e) {
      throw new SerializerException("Could not create instance of builtin transformation", e,
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }

    final EntityCollection entityCollection = step1.transform(input);

    // FIXME
    throw new UnsupportedOperationException();
  }

}
