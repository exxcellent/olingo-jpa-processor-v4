package org.apache.olingo.jpa.processor.core.api;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import org.apache.olingo.jpa.processor.api.DependencyInjector;
import org.apache.olingo.jpa.processor.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.conversion.TransformingFactory;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.jpa.processor.debug.JPACoreDebugger;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;

class JPAODataRequestContextImpl implements JPAODataSessionContextAccess {

  private final DependencyInjectorImpl dpi;
  private final JPAODataGlobalContextImpl globalContext;
  private final TransformingFactory transformerFactory;
  private final HttpServletRequest request;
  private final EntityManager em;
  private JPADebugSupportWrapper debugSupport = null;
  private JPAServiceDebugger serviceDebugger = null;
  private boolean disposed = false;
  private DependencyInjector dpiOverlay = null;

  public JPAODataRequestContextImpl(final EntityManager em, final JPAODataGlobalContextImpl globalContext,
      final HttpServletRequest request,
      final HttpServletResponse response) throws ODataException {
    this.em = em;
    this.globalContext = globalContext;
    this.transformerFactory = new TransformingFactory(this);

    setDebugSupport(new DefaultDebugSupport());

    this.dpi = new DependencyInjectorImpl((DependencyInjectorImpl) globalContext.getDependencyInjector());
    dpi.registerDependencyMapping(JPAODataSessionContextAccess.class, this);
    dpi.registerDependencyMapping(EntityManager.class, em);
    dpi.registerDependencyMapping(HttpServletRequest.class, request);
    dpi.registerDependencyMapping(HttpServletResponse.class, response);

    this.request = request;
    initializeRequestContext(request);
  }

  @Override
  public EntityManager getEntityManager() {
    return em;
  }

  void dispose() {
    dpiOverlay = null;
    disposed = true;
  }

  @Override
  public DebugSupport getDebugSupport() {
    return debugSupport;
  }

  @Override
  public void setDebugSupport(final DebugSupport jpaDebugSupport) {
    this.debugSupport = new JPADebugSupportWrapper(jpaDebugSupport);
    globalContext.getServerDebugger().setDebugSupportProcessor(debugSupport);
  }

  @Override
  public JPAServiceDebugger getServiceDebugger() {
    return serviceDebugger;
  }

  private void initializeRequestContext(final HttpServletRequest request) {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    globalContext.getServerDebugger().resolveDebugMode(request);
    if (globalContext.getServerDebugger().isDebugMode()) {
      serviceDebugger = new JPACoreDebugger();
    } else {
      serviceDebugger = new JPAEmptyDebugger();
    }
    debugSupport.setDebugger(serviceDebugger);
    // this is really, really bad
    ODataJPAException.setLocales(request.getLocales());
    ODataJPAProcessException.setLocales(request.getLocales());
  }

  @Override
  public String getParameter(final String name) {
    if (request == null) {
      throw new IllegalArgumentException("Not initialized with request");
    }
    return request.getHeader(name);
  }

  @Override
  public Enumeration<String> getParameters(final String name) {
    if (request == null) {
      throw new IllegalArgumentException("Not initialized with request");
    }
    return request.getHeaders(name);
  }

  @Override
  public Locale getLocale() {
    if (request == null) {
      throw new IllegalArgumentException("Not initialized with request");
    }
    return request.getLocale();
  }

  @Override
  public DependencyInjector getDependencyInjector() {
    if (dpiOverlay != null) {
      return dpiOverlay;
    }
    return dpi;
  }

  @Override
  public TransformingFactory getTransformerFactory() {
    return transformerFactory;
  }

  @Override
  public OData getOdata() {
    return globalContext.getOdata();
  }

  @Override
  public ServiceMetadata getServiceMetaData() {
    return globalContext.getServiceMetaData();
  }

  @Override
  public JPAODataDatabaseProcessor getDatabaseProcessor() {
    return globalContext.getDatabaseProcessor();
  }

  @Override
  public JPAEdmProvider getEdmProvider() {
    return globalContext.getEdmProvider();
  }

  @Override
  public List<EdmxReference> getReferences() {
    return globalContext.getReferences();
  }

  public void startDependencyInjectorOverlay() {
    if (dpiOverlay != null) {
      throw new IllegalStateException("overlay laready running");
    }
    dpiOverlay = new DependencyInjectorImpl(dpi);
  }

  public void stopDependencyInjectorOverlay() {
    dpiOverlay = null;
  }

}