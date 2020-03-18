package org.apache.olingo.jpa.processor.transformation.impl;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.TransformationContextRequirement;
import org.apache.olingo.jpa.processor.transformation.TransformationDeclaration;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class QueryEntityResult2ODataResponseContentTransformation implements
Transformation<QueryEntityResult, ODataResponseContent> {

  public final static TransformationDeclaration<QueryEntityResult, ODataResponseContent> DEFAULT_DECLARATION =
      new TransformationDeclaration<>(
          QueryEntityResult.class, ODataResponseContent.class, new TransformationContextRequirement(
              JPAODataGlobalContext.class), new TransformationContextRequirement(
                  JPAODataRequestContext.class), new TransformationContextRequirement(
                      UriInfoResource.class), new TransformationContextRequirement(
                          ODataRequest.class), new TransformationContextRequirement(
                              RepresentationType.class, RepresentationType.COLLECTION_ENTITY),
          new TransformationContextRequirement(
              ContentType.class));

  @Inject
  private JPAODataRequestContext requestContext;

  @Override
  public Class<QueryEntityResult> getInputType() {
    return QueryEntityResult.class;
  }

  @Override
  public Class<ODataResponseContent> getOutputType() {
    return ODataResponseContent.class;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <I> Transformation<I, ODataResponseContent> createSubTransformation(final Class<I> newStart)
      throws SerializerException {
    if (newStart.isAssignableFrom(getInputType())) {
      return (Transformation<I, ODataResponseContent>) this;
    }
    if (requestContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (newStart.isAssignableFrom(EntityCollection.class)) {
      return (Transformation<I, ODataResponseContent>) createStep2();
    }
    throw new SerializerException("Sub transformation not supported",
        SerializerException.MessageKeys.UNSUPPORTED_FORMAT);
  }

  private QueryEntityResult2EntityCollectionTransformation createStep1() throws SerializerException {
    final QueryEntityResult2EntityCollectionTransformation step1 =
        new QueryEntityResult2EntityCollectionTransformation();
    try {
      requestContext.getDependencyInjector().injectDependencyValues(step1);
    } catch (final ODataApplicationException e) {
      throw new SerializerException("Could not create instance of transformation", e,
          SerializerException.MessageKeys.IO_EXCEPTION);
    }

    return step1;
  }

  private EntityCollection2ODataResponseContentTransformation createStep2() throws SerializerException {
    final EntityCollection2ODataResponseContentTransformation step2 =
        new EntityCollection2ODataResponseContentTransformation();
    try {
      requestContext.getDependencyInjector().injectDependencyValues(step2);
    } catch (final ODataApplicationException e) {
      throw new SerializerException("Could not create instance of transformation", e,
          SerializerException.MessageKeys.IO_EXCEPTION);
    }
    return step2;
  }

  @Override
  public ODataResponseContent transform(final QueryEntityResult input) throws SerializerException {
    if (requestContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    final QueryEntityResult2EntityCollectionTransformation step1 = createStep1();
    final EntityCollection2ODataResponseContentTransformation step2 = createStep2();
    final EntityCollection entityCollection = step1.transform(input);
    return step2.transform(entityCollection);
  }

}
