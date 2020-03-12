package org.apache.olingo.jpa.processor.core.api;

import java.util.Locale;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import org.apache.olingo.jpa.processor.DependencyInjector;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.jpa.processor.debug.JPACoreDebugger;
import org.apache.olingo.jpa.processor.transformation.TransformingFactory;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

class JPAODataRequestContextImpl extends AbstractContextImpl implements JPAODataRequestContext {

  private final DependencyInjectorImpl di;
  private final AbstractContextImpl parentContext;
  private final TransformingFactory transformerFactory;
  private final HttpServletRequest request;
  private final HttpServletResponse response;
  private final EntityManager em;
  private JPADebugSupportWrapper debugSupport = null;
  private JPAServiceDebugger serviceDebugger = null;
  private boolean disposed = false;
  private DependencyInjector diOverlay = null;

  public JPAODataRequestContextImpl(final EntityManager em, final AbstractContextImpl parentContext,
      final HttpServletRequest request,
      final HttpServletResponse response) throws ODataException {
    this.em = em;
    this.parentContext = parentContext;
    this.transformerFactory = new TransformingFactory(this);

    setDebugSupport(new DefaultDebugSupport());

    this.di = new DependencyInjectorImpl(parentContext.getDependencyInjectorImpl());
    di.registerDependencyMapping(JPAODataRequestContext.class, this);
    di.registerDependencyMapping(EntityManager.class, em);
    di.registerDependencyMapping(HttpServletRequest.class, request);
    di.registerDependencyMapping(HttpServletResponse.class, response);

    this.request = request;
    this.response = response;
    initializeRequestContext(request);
  }

  @Override
  public EntityManager getEntityManager() {
    return em;
  }

  void dispose() {
    diOverlay = null;
    disposed = true;
  }

  //  @Override
  //  public DebugSupport getDebugSupport() {
  //    return debugSupport;
  //  }

  @Override
  public void setDebugSupport(final DebugSupport jpaDebugSupport) {
    this.debugSupport = new JPADebugSupportWrapper(jpaDebugSupport);
    parentContext.getServerDebugger().setDebugSupportProcessor(debugSupport);
  }

  @Override
  public JPAServiceDebugger getServiceDebugger() {
    return serviceDebugger;
  }

  private void initializeRequestContext(final HttpServletRequest request) {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    parentContext.getServerDebugger().resolveDebugMode(request);
    if (parentContext.getServerDebugger().isDebugMode()) {
      serviceDebugger = new JPACoreDebugger();
    } else {
      serviceDebugger = new JPAEmptyDebugger();
    }
    debugSupport.setDebugger(serviceDebugger);
    // this is really, really bad
    ODataJPAException.setLocales(request.getLocales());
    ODataJPAProcessException.setLocales(request.getLocales());
  }

  //  @Override
  //  public String getParameter(final String name) {
  //    if (request == null) {
  //      throw new IllegalArgumentException("Not initialized with request");
  //    }
  //    return request.getHeader(name);
  //  }

  //  @Override
  //  public Enumeration<String> getParameters(final String name) {
  //    if (request == null) {
  //      throw new IllegalArgumentException("Not initialized with request");
  //    }
  //    return request.getHeaders(name);
  //  }

  @Override
  public Locale getLocale() {
    if (request == null) {
      throw new IllegalArgumentException("Not initialized with request");
    }
    return request.getLocale();
  }

  @Override
  public DependencyInjector getDependencyInjector() {
    if (diOverlay != null) {
      return diOverlay;
    }
    return di;
  }

  @Override
  public TransformingFactory getTransformerFactory() {
    return transformerFactory;
  }

  @Override
  public OData getOdata() {
    return parentContext.getOdata();
  }

  @Override
  public ServiceMetadata getServiceMetaData() {
    return parentContext.getServiceMetaData();
  }

  @Override
  public JPAODataDatabaseProcessor getDatabaseProcessor() {
    return parentContext.getDatabaseProcessor();
  }

  @Override
  public JPAEdmProvider getEdmProvider() {
    return parentContext.getEdmProvider();
  }

  //  @Override
  //  public List<EdmxReference> getReferences() {
  //    return parentContext.getReferences();
  //  }

  public void startDependencyInjectorOverlay() {
    if (diOverlay != null) {
      throw new IllegalStateException("overlay already running");
    }
    diOverlay = new DependencyInjectorImpl(di);
  }

  public void stopDependencyInjectorOverlay() {
    diOverlay = null;
  }

  @Override
  ServerCoreDebugger getServerDebugger() {
    return parentContext.getServerDebugger();
  }

  @Override
  protected DependencyInjectorImpl getDependencyInjectorImpl() {
    return (DependencyInjectorImpl) getDependencyInjector();
  }

  @Override
  public JPAODataRequestContext createSubRequestContext() throws ODataException {
    return new JPAODataRequestContextImpl(em, this, request, response);
  }

}