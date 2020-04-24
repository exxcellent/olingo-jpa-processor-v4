package org.apache.olingo.jpa.processor.core.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.ModifiableDependencyInjector;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;

class JPAODataGlobalContextImpl extends AbstractContextImpl implements JPAODataGlobalContext {

  private final JPAEdmProvider jpaEdm;
  private final JPAODataDatabaseProcessor databaseProcessor;
  private final JPAAdapter mappingAdapter;
  private final OData odata;
  private final ServiceMetadata serviceMetaData;
  private final List<EdmxReference> references = new LinkedList<EdmxReference>();
  private final DependencyInjectorImpl di;
  private final ServerCoreDebugger serverDebugger;
  private boolean disposed = false;

  public JPAODataGlobalContextImpl(final JPAAdapter mappingAdapter) throws ODataException {
    super();
    this.odata = OData.newInstance();
    this.mappingAdapter = mappingAdapter;
    this.di = new DependencyInjectorImpl();

    this.serverDebugger = new ServerCoreDebugger(odata);

    jpaEdm = new JPAEdmProvider(mappingAdapter.getNamespace(), mappingAdapter.getMetamodel());
    databaseProcessor = mappingAdapter.getDatabaseAccessor();
    assert databaseProcessor != null;
    this.serviceMetaData = odata.createServiceMetadata(jpaEdm, references);

    di.registerDependencyMapping(JPAAdapter.class, mappingAdapter);
    di.registerDependencyMapping(JPAEdmProvider.class, jpaEdm);
    di.registerDependencyMapping(JPAODataGlobalContext.class, this);

    registerDTOs();
  }

  void dispose() {
    mappingAdapter.dispose();
    di.dispose();
    disposed = true;
  }

  @Override
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

  @Override
  public ModifiableDependencyInjector getDependencyInjector() {
    return di;
  }

  @Override
  protected JPAAdapter getPersistenceAdapter() {
    return mappingAdapter;
  }
}