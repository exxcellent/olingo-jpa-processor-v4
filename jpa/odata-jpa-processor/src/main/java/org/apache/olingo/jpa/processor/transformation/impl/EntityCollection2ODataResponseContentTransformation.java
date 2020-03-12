package org.apache.olingo.jpa.processor.transformation.impl;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPASerializerException;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializeCollection;
import org.apache.olingo.jpa.processor.core.serializer.JPASerializer;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent.ContentState;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class EntityCollection2ODataResponseContentTransformation implements
    Transformation<EntityCollection, ODataResponseContent> {

  @Inject
  private JPAODataGlobalContext globalContext;
  @Inject
  private RepresentationType outputStructure;
  @Inject
  private ContentType responseFormat;
  @Inject
  private UriInfoResource uriResource;
  @Inject
  private ODataRequest odataRequest;

  @Override
  public Class<EntityCollection> getInputType() {
    return EntityCollection.class;
  }

  @Override
  public Class<ODataResponseContent> getOutputType() {
    return ODataResponseContent.class;
  }

  @Override
  public ODataResponseContent transform(final EntityCollection entityCollection) throws SerializerException {
    if (outputStructure == null) {
      throw new IllegalStateException("Dependency injection not working: " + RepresentationType.class.getSimpleName()
          + " expected");
    }
    if (responseFormat == null) {
      throw new IllegalStateException("Dependency injection not working: " + ContentType.class.getSimpleName()
          + " expected");
    }
    if (uriResource == null) {
      throw new IllegalStateException("Dependency injection not working: " + UriInfoResource.class.getSimpleName()
          + " expected");
    }
    if (globalContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (odataRequest == null) {
      throw new IllegalStateException("Dependency injection not working: " + ODataRequest.class.getSimpleName()
          + " expected");
    }

    final JPASerializer serializer = createSerializer();
    try {
      final SerializerResult sResult = serializer.serialize(odataRequest, entityCollection);
      final ContentState state = determineContentState(entityCollection);
      return new ODataResponseContent(state, sResult.getContent());
    } catch (final ODataJPASerializerException e) {
      throw new SerializerException("Problem while serialization", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <I> Transformation<I, ODataResponseContent> createSubTransformation(final Class<I> newStart)
      throws SerializerException {
    if (newStart.isAssignableFrom(getInputType())) {
      return (Transformation<I, ODataResponseContent>) this;
    }
    throw new SerializerException("No sub transformation possible", SerializerException.MessageKeys.UNSUPPORTED_FORMAT);
  }

  private JPASerializer createSerializer() throws SerializerException {
    switch (outputStructure) {
    case COLLECTION_ENTITY:
      return new JPASerializeCollection(globalContext.getServiceMetaData(), globalContext.getOdata(), responseFormat,
          uriResource);
    default:
      throw new SerializerException("Output serializer for " + outputStructure + " not supported",
          SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
  }

  private ContentState determineContentState(final EntityCollection entityCollection) {
    if (outputStructure == null) {
      throw new IllegalStateException("Dependency injection not working: " + RepresentationType.class.getSimpleName()
          + " expected");
    }
    if (entityCollection.getEntities() == null) {
      return ContentState.NULL;
    }
    switch (outputStructure) {
    case COLLECTION_COMPLEX:
    case COLLECTION_ENTITY:
    case COLLECTION_PRIMITIVE:
    case COLLECTION_REFERENCE:
      if (entityCollection.getEntities().isEmpty()) {
        return ContentState.EMPTY_COLLECTION;
      }
      return ContentState.PRESENT;
    default:
      // single
      if (entityCollection.getEntities().isEmpty()) {
        return ContentState.NULL;
      }
      return ContentState.PRESENT;
    }
  }

}
