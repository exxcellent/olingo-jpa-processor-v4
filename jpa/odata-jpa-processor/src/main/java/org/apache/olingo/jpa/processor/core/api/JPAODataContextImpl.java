package org.apache.olingo.jpa.processor.core.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;

class JPAODataContextImpl implements JPAODataContext {

	private final JPAEdmProvider jpaEdm;
	private final AbstractJPADatabaseProcessor databaseProcessor;
	private final JPAAdapter mappingAdapter;
	private final OData odata;
	private final ServiceMetadata serviceMetaData;
	private final List<EdmxReference> references = new LinkedList<EdmxReference>();
	private JPAServiceDebugger debugger;
	private JPADebugSupportWrapper debugSupport = new JPADebugSupportWrapper(new DefaultDebugSupport());
	private DependencyInjector dpi = null;
	private boolean disposed = false;

	public JPAODataContextImpl(final JPAAdapter mappingAdapter) throws ODataException {
		super();
		this.odata = OData.newInstance();
		this.mappingAdapter = mappingAdapter;

		jpaEdm = new JPAEdmProvider(mappingAdapter.getNamespace(), mappingAdapter.getMetamodel());
		databaseProcessor = mappingAdapter.getDatabaseAccessor();
		assert databaseProcessor != null;
		this.serviceMetaData = odata.createServiceMetadata(jpaEdm, references);
		registerDTOs();
	}

	void dispose() {
		mappingAdapter.dispose();
		disposed = true;
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
		if (disposed)
			throw new IllegalStateException("Already disposed");
		return odata;
	}

	@Override
	public DebugSupport getDebugSupport() {
		return debugSupport;
	}

	@Override
	public List<EdmxReference> getReferences() {
		return references;
	}

	@Override
	public JPAEdmProvider getEdmProvider() {
		if (disposed)
			throw new IllegalStateException("Already disposed");
		return jpaEdm;
	}

	ServiceMetadata getServiceMetaData() {
		return serviceMetaData;
	}

	@Override
	public JPAODataDatabaseProcessor getDatabaseProcessor() {
		if (disposed)
			throw new IllegalStateException("Already disposed");
		return databaseProcessor;
	}

	@Override
	public void setDebugSupport(final DebugSupport jpaDebugSupport) {
		this.debugSupport = new JPADebugSupportWrapper(jpaDebugSupport);
	}

	@Override
	public JPAServiceDebugger getDebugger() {
		return debugger;
	}

	void initDebugger(final String debugFormat) {
		// see org.apache.olingo.server.core.debug.ServerCoreDebugger
		boolean isDebugMode = false;

		if (debugSupport != null) {
			// Should we read the parameter from the servlet here and ignore multiple parameters?
			if (debugFormat != null) {
				debugSupport.init(odata);
				isDebugMode = debugSupport.isUserAuthorized();
			}
		}
		if (isDebugMode) {
			debugger = new JPACoreDebugger();
		} else {
			debugger = new JPAEmptyDebugger();
		}
		debugSupport.setDebugger(debugger);
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

	void initDependencyInjection(final DependencyInjector newDpi) {
		if (newDpi == null) {
			throw new IllegalArgumentException("New instance required");
		}
		this.dpi = newDpi;
		dpi.registerDependencyMapping(JPAAdapter.class, mappingAdapter);
		dpi.registerDependencyMapping(JPAEdmProvider.class, jpaEdm);

	}

	@Override
	public DependencyInjector getDependencyInjector() {
		if (dpi == null) {
			throw new IllegalStateException("DependencyInjector not yet initialized");
		}
		return dpi;
	}
}