package org.apache.olingo.jpa.processor.transformation.excel;

import java.io.IOException;

import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.serializer.SerializerException;

public class QueryEntityResult2ExcelODataResponseContentTransformation implements
Transformation<QueryEntityResult, ODataResponseContent> {

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
  public ODataResponseContent transform(final QueryEntityResult input) throws SerializerException {
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
      final ExcelConverter converter = new ExcelConverter(exportConfiguration);
      return converter.produceExcel(input, representationType);
    } catch (final IOException | ODataJPAModelException | ODataJPAConversionException e) {
      throw new SerializerException("Couldn't export as Excel", e, SerializerException.MessageKeys.IO_EXCEPTION);
    }
  }

}
