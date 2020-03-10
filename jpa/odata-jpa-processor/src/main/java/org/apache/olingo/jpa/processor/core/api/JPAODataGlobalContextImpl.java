package org.apache.olingo.jpa.processor.core.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.api.DependencyInjector;
import org.apache.olingo.jpa.processor.api.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

class JPAODataGlobalContextImpl implements JPAODataGlobalContext {

  private final JPAEdmProvider jpaEdm;
  private final AbstractJPADatabaseProcessor databaseProcessor;
  private final JPAAdapter mappingAdapter;
  private final OData odata;
  private final ServiceMetadata serviceMetaData;
  private final List<EdmxReference> references = new LinkedList<EdmxReference>();
  private final DependencyInjectorImpl dpi;
  private final ServerCoreDebugger serverDebugger;
  private boolean disposed = false;

  public JPAODataGlobalContextImpl(final JPAAdapter mappingAdapter) throws ODataException {
    super();
    this.odata = OData.newInstance();
    this.mappingAdapter = mappingAdapter;
    this.dpi = new DependencyInjectorImpl();

    this.serverDebugger = new ServerCoreDebugger(odata);

    jpaEdm = new JPAEdmProvider(mappingAdapter.getNamespace(), mappingAdapter.getMetamodel());
    databaseProcessor = mappingAdapter.getDatabaseAccessor();
    assert databaseProcessor != null;
    this.serviceMetaData = odata.createServiceMetadata(jpaEdm, references);

    dpi.registerDependencyMapping(JPAAdapter.class, mappingAdapter);
    dpi.registerDependencyMapping(JPAEdmProvider.class, jpaEdm);
    dpi.registerDependencyMapping(JPAODataGlobalContext.class, this);

    registerDTOs();
  }

  void dispose() {
    mappingAdapter.dispose();
    dpi.dispose();
    disposed = true;
  }

  ServerCoreDebugger getServerDebugger() {
    return serverDebugger;
  }

  private void registerDTOs() throws ODataJPAModelException {
    final Collection<Class<?>> dtos = mappingAdapter.getDTOs();
    if (dtos == null || dtos.isEmpty()) {
      return;
    }

    final IntermediateServiceDocument sd = jpaEdm.getServiceDocument();
    for (final Class<?> dtoClass : dtos) {
      sd.createDTOType(dtoClass);
    }
  }

  @Override
  public OData getOdata() {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    return odata;
  }

  @Override
  public List<EdmxReference> getReferences() {
    return references;
  }

  @Override
  public JPAEdmProvider getEdmProvider() {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    return jpaEdm;
  }

  @Override
  public ServiceMetadata getServiceMetaData() {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    return serviceMetaData;
  }

  @Override
  public JPAODataDatabaseProcessor getDatabaseProcessor() {
    if (disposed) {
      throw new IllegalStateException("Already disposed");
    }
    return databaseProcessor;
  }

  /**
   *
   * @return The JPAAdapter, with refreshed preparation from {@link #getDependencyInjector() dependency injector}.
   */
  JPAAdapter refreshMappingAdapter() {
    try {
      getDependencyInjector().injectFields(mappingAdapter);
    } catch (final ODataApplicationException e) {
      throw new RuntimeException(e);
    }
    return mappingAdapter;
  }

  @Override
  public DependencyInjector getDependencyInjector() {
    return dpi;
  }

}