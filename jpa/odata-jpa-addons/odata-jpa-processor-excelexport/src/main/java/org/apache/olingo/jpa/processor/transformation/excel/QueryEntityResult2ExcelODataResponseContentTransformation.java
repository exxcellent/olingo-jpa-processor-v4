package org.apache.olingo.jpa.processor.transformation.excel;

import java.io.IOException;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.TransformationContextRequirement;
import org.apache.olingo.jpa.processor.transformation.TransformationDeclaration;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfoResource;

public class QueryEntityResult2ExcelODataResponseContentTransformation implements
Transformation<QueryEntityResult, ODataResponseContent> {

  public final static ContentType CONTENTTYPE_EXCEL = ContentType.create(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  public final static RepresentationType[] SUPPORTED_REPRESENTATIONTYPES = new RepresentationType[] {
      RepresentationType.COLLECTION_ENTITY };

  public final static TransformationDeclaration<QueryEntityResult, ODataResponseContent> DEFAULT_DECLARATION =
      new TransformationDeclaration<>(
          QueryEntityResult.class, ODataResponseContent.class, new TransformationContextRequirement(
              JPAODataGlobalContext.class), new TransformationContextRequirement(
                  JPAODataRequestContext.class), new TransformationContextRequirement(
                      UriInfoResource.class), new TransformationContextRequirement(
                          ODataRequest.class), new TransformationContextRequirement(
                              RepresentationType.class, SUPPORTED_REPRESENTATIONTYPES),
          new TransformationContextRequirement(
              ContentType.class, CONTENTTYPE_EXCEL));

  @Inject
  private JPAODataRequestContext requestContext;

  @Inject
  private RepresentationType representationType;

  @Inject
  private Configuration exportConfiguration;

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
    throw new SerializerException("Sub transformation not supported",
        SerializerException.MessageKeys.UNSUPPORTED_FORMAT);
  }

  @Override
  public final ODataResponseContent transform(final QueryEntityResult input) throws SerializerException {
    if (!input.getExpandChildren().isEmpty()) {
      throw new SerializerException("$expand's are not supported for Excel export", SerializerException.MessageKeys.NOT_IMPLEMENTED);
    }
    if (requestContext == null) {
      throw new IllegalStateException("Dependency injection not working: " + JPAODataGlobalContext.class.getSimpleName()
          + " expected");
    }
    if (representationType == null) {
      throw new IllegalStateException("Dependency injection not working: " + RepresentationType.class.getSimpleName()
          + " expected");
    }
    try {
      final ExcelConverter converter = createConverter(exportConfiguration);
      return converter.produceExcel(input, representationType);
    } catch (final IOException | ODataJPAModelException | ODataJPAConversionException e) {
      throw new SerializerException("Couldn't export as Excel", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

  /**
   *
   * @param configuration The configuration to use or <code>null</code> if no one is given.
   * @return The converter instance to use.
   */
  protected ExcelConverter createConverter(final Configuration configuration) {
    return new ExcelConverter(configuration);
  }

}
