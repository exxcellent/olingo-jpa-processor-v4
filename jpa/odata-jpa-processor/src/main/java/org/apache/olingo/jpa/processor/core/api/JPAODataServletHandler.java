package org.apache.olingo.jpa.processor.core.api;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.security.AnnotationBasedSecurityInceptor;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
import org.apache.olingo.jpa.processor.impl.JPAODataActionProcessor;
import org.apache.olingo.jpa.processor.impl.JPAODataBatchProcessor;
import org.apache.olingo.jpa.processor.impl.JPAStructureProcessor;
import org.apache.olingo.jpa.processor.transformation.Transformation;
import org.apache.olingo.jpa.processor.transformation.TransformationDeclaration;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.processor.Processor;
import org.apache.olingo.server.api.serializer.RepresentationType;

/**
 * The implementor to handle HTTP servlet requests as an OData REST API.
 *
 * @author Ralf Zozmann
 *
 */
public class JPAODataServletHandler {

  private static class CustomFormat<I, O> {
    private final ContentType type;
    private final TransformationDeclaration<I, O> tDeclaration;
    private final Class<? extends Transformation<I, O>> tClass;
    private final RepresentationType[] representationTypes;

    CustomFormat(final ContentType type, final TransformationDeclaration<I, O> tDeclaration,
        final Class<? extends Transformation<I, O>> tClass, final RepresentationType... representationTypes) {
      this.type = type;
      this.tDeclaration = tDeclaration;
      this.tClass = tClass;
      this.representationTypes = representationTypes;
    }
  }
  static final Logger LOG = Logger.getLogger(JPAODataServletHandler.class.getName());

  private final JPAODataGlobalContextImpl globalContext;
  private final List<CustomFormat<?, ?>> customOutputFormats = new LinkedList<>();
  private SecurityInceptor securityInceptor = new AnnotationBasedSecurityInceptor();// having one as default

  public JPAODataServletHandler(final JPAAdapter mappingAdapter) throws ODataException {
    super();
    this.globalContext = new JPAODataGlobalContextImpl(mappingAdapter);
  }

  public final JPAODataGlobalContext getJPAODataContext() {
    return globalContext;
  }

  @SuppressWarnings("unchecked")
  public void process(final HttpServletRequest request, final HttpServletResponse response) {

    if ("OPTIONS".equals(request.getMethod().toUpperCase(Locale.ENGLISH))) {
      // special handling as long as HttpMethod.OPTIONS is not existing/supported by Olingo
      handleOPTIONSRequest(request, response);
      return;
    }
    try {
      final JPAODataHttpHandlerImpl handler = new JPAODataHttpHandlerImpl(this, globalContext, request, response);
      final JPAODataRequestContext requestContext = handler.getRequestContext();

      // bring custom output transformations into effect for request
      for (final CustomFormat<?, ?> customFormat : customOutputFormats) {
        requestContext.getTransformerFactory().registerTransformation(
            (TransformationDeclaration<Object, Object>) customFormat.tDeclaration,
            (Class<? extends Transformation<Object, Object>>) customFormat.tClass);
        handler.getContentSupport().activateCustomContentType(customFormat.type, customFormat.representationTypes);
      }

      final Collection<Processor> processors = collectProcessors(requestContext);
      for (final Processor p : processors) {
        handler.register(p);
      }

      handler.process(request, response);
    } catch (final ODataException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * As default we have to implement a behavior for preflight-requests in a CORS scenario to handle the
   * same-origin-policy (SOP) in modern browsers.
   * @see https://fetch.spec.whatwg.org/#cors-preflight-request
   * @see https://web.dev/cross-origin-resource-sharing/
   */
  private void handleOPTIONSRequest(final HttpServletRequest request, final HttpServletResponse response) {
    final String corsMethod = request.getHeader("Access-Control-Request-Method");
    try {
      if (corsMethod == null) {
        // if not an pre-flight request, then answer with NOT SUPPORTED
        response.sendError(HttpStatusCode.METHOD_NOT_ALLOWED.getStatusCode(), request.getMethod()
            + " is only allowed for CORS pre-flight checks");
      } else {
        answerCrossOriginRequest(request, response);
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * This method is called to modify the answer for CORS pre-flight request (OPTIONS request).
   * @see https://fetch.spec.whatwg.org/#cors-preflight-request
   */
  protected void answerCrossOriginRequest(final HttpServletRequest request, final HttpServletResponse response)
      throws IOException {
    response.sendError(HttpStatusCode.FORBIDDEN.getStatusCode(), "CORS is not allowed per default");
  }

  /**
   * Convenience method to register a transformation as custom output format for an content type.
   *
   * @see org.apache.olingo.jpa.processor.core.util.ExtensibleContentTypeSupport
   */
  public <I, O> void activateCustomResponseTransformation(final TransformationDeclaration<I, O> tDeclaration,
      final Class<? extends Transformation<I, O>> tClass, final ContentType contentType,
          final RepresentationType... representationTypes) {
    final CustomFormat<I, O> customFormat = new CustomFormat<>(contentType, tDeclaration, tClass, representationTypes);
    customOutputFormats.add(customFormat);
  }

  /**
   * Client hook method to modify response before send back...
   */
  protected void modifyResponse(final ODataResponse response) {
    // set all response as not cachable as default
    if (!response.getAllHeaders().containsKey(HttpHeader.CACHE_CONTROL)) {
      response.setHeader(HttpHeader.CACHE_CONTROL, "max-age=0, no-cache, no-store, must-revalidate");
    }
  }

  /**
   * Client hook method to add custom resources (like dependencies for dependency
   * injection support or transformations) before anything is done with the request context.
   *
   * @param requestContext The context of current request where processing will starting.
   */
  protected void prepareRequestContext(final ModifiableJPAODataRequestContext requestContext) {
    // do nothing in default implementation
  }

  /**
   * Client hook method to change custom resources (like dependencies for dependency injection support) after
   * initialization of request handling (also after security checks), but before processing of request. A transaction
   * maybe in progress...
   *
   * @param requestContext The context of current request where processing will starting.
   */
  protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
    // do nothing in default implementation
  }

  public void dispose() {
    globalContext.dispose();
  }

  /**
   * Set or replace the security inceptor. A <code>null</code> parameter will
   * disable security constraints.
   */
  public void setSecurityInceptor(final SecurityInceptor securityInceptor) {
    this.securityInceptor = securityInceptor;
  }

  /**
   *
   * @return The security inceptor or <code>null</code> if no one is set.
   */
  SecurityInceptor getSecurityInceptor() {
    return securityInceptor;
  }

  /**
   * Client expendable list of processors.
   *
   * @return The collection of processors to use to handle the request.
   */
  protected Collection<Processor> collectProcessors(final JPAODataRequestContext requestContext) {
    final Collection<Processor> processors = new LinkedList<>();
    processors.add(new JPAStructureProcessor(requestContext));
    processors.add(new JPAODataActionProcessor(requestContext));
    processors.add(new JPAODataBatchProcessor());
    return processors;
  }

}
