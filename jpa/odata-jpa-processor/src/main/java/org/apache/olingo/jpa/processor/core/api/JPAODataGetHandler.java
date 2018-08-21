package org.apache.olingo.jpa.processor.core.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.database.JPADefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPAODataDatabaseOperations;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.processor.JPAEntityProcessor;
import org.apache.olingo.jpa.processor.core.processor.JPAODataActionProcessor;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugInformation;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.apache.olingo.server.api.processor.Processor;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.core.ODataHttpHandlerImpl;
import org.apache.olingo.server.core.uri.parser.Parser;

public class JPAODataGetHandler {

	private static final Logger LOG = Logger.getLogger(JPAODataGetHandler.class.getName());

	private final JPAODataContextImpl context;
	private SecurityInceptor securityInceptor = null;

	public JPAODataGetHandler(final JPAAdapter mappingAdapter) throws ODataException {
		super();
		this.context = new JPAODataContextImpl(mappingAdapter);
	}

	public JPAODataSessionContextAccess getJPAODataContext() {
		return context;
	}

	public void process(final HttpServletRequest request, final HttpServletResponse response) {
		final JPAODataHttpHandlerImpl handler = new JPAODataHttpHandlerImpl(context, securityInceptor);
		context.getEdmProvider().setRequestLocales(request.getLocales());
		context.initDebugger(request.getParameter(DebugSupport.ODATA_DEBUG_QUERY_PARAMETER));
		handler.register(context.getDebugSupport());

		final DependencyInjector dpi = new DependencyInjector();
		dpi.registerDependencyMapping(HttpServletRequest.class, request);
		dpi.registerDependencyMapping(HttpServletResponse.class, response);
		prepareDependencyInjection(dpi);
		context.initDependencyInjection(dpi);

		final Collection<Processor> processors = collectProcessors(request, response, handler.getEntityManager());
		for(final Processor p: processors) {
			handler.register(p);
		}
		handler.process(request, response);
	}

	/**
	 * Client hook method to add custom resources as dependencies for dependency
	 * injection support.
	 *
	 * @param dpi
	 *            The injector used to handle injection of registered dependencies.
	 */
	protected void prepareDependencyInjection(final DependencyInjector dpi) {
		// do nothing in default implementation
	}

	/**
	 * Set or replace the security inceptor. A <code>null</code> parameter will
	 * disable security constraints.
	 */
	public void setSecurityInceptor(final SecurityInceptor securityInceptor) {
		this.securityInceptor = securityInceptor;
	}

	/**
	 * Client expendable list of processors.
	 *
	 * @return The collection of processors to use to handle the request.
	 */
	//TODO replace EntityManager by JPAAdapter
	protected Collection<Processor> collectProcessors(final HttpServletRequest request, final HttpServletResponse response, final EntityManager em) {
		final Collection<Processor> processors = new LinkedList<>();
		processors.add(new JPAEntityProcessor(context, em));
		processors.add(new JPAODataRequestProcessor(context, em));
		processors.add(new JPAODataActionProcessor(context, em));
		processors.add(new JPAODataBatchProcessor());
		return processors;
	}

	private static class JPAODataContextImpl implements JPAODataContext {
		private final JPAEdmProvider jpaEdm;
		private JPAODataDatabaseProcessor databaseProcessor;
		private final JPAAdapter mappingAdapter;
		private final OData odata;
		private final ServiceMetadata serviceMetaData;
		private final List<EdmxReference> references = new LinkedList<EdmxReference>();
		private JPAODataDatabaseOperations operationConverter;
		private JPAServiceDebugger debugger;
		private JPADebugSupportWrapper debugSupport = new JPADebugSupportWrapper(new DefaultDebugSupport());
		private DependencyInjector dpi = new DependencyInjector();

		public JPAODataContextImpl(final JPAAdapter mappingAdapter) throws ODataException {
			super();
			this.odata = OData.newInstance();
			this.mappingAdapter = mappingAdapter;

			operationConverter = new JPADefaultDatabaseProcessor();
			jpaEdm = new JPAEdmProvider(mappingAdapter.getNamespace(), mappingAdapter.getMetamodel());
			databaseProcessor = mappingAdapter.getDatabaseAccessor();
			this.serviceMetaData = odata.createServiceMetadata(jpaEdm, references);
			registerDTOs();
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
			return odata;
		}

		@Override
		public DebugSupport getDebugSupport() {
			return debugSupport;
		}

		@Override
		public JPAODataDatabaseOperations getOperationConverter() {
			return operationConverter;
		}

		@Override
		public List<EdmxReference> getReferences() {
			return references;
		}

		@Override
		public void setOperationConverter(final JPAODataDatabaseOperations jpaOperationConverter) {
			operationConverter = jpaOperationConverter;
		}

		@Override
		public void setMetadataPostProcessor(final JPAEdmMetadataPostProcessor postProcessor) throws ODataException {
			IntermediateServiceDocument.setPostProcessor(postProcessor);
		}

		@Override
		public JPAEdmProvider getEdmProvider() {
			return jpaEdm;
		}

		public ServiceMetadata getServiceMetaData() {
			return serviceMetaData;
		}

		@Override
		public JPAODataDatabaseProcessor getDatabaseProcessor() {
			return databaseProcessor;
		}

		@Override
		public void setDatabaseProcessor(final JPAODataDatabaseProcessor databaseProcessor) {
			this.databaseProcessor = databaseProcessor;
		}

		@Override
		public void setDebugSupport(final DebugSupport jpaDebugSupport) {
			this.debugSupport = new JPADebugSupportWrapper(jpaDebugSupport);
		}

		@Override
		public JPAServiceDebugger getDebugger() {
			return debugger;
		}

		@Override
		public void initDebugger(final String debugFormat) {
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
				debugger = new JPACoreDeugger();
			} else {
				debugger = new JPAEmptyDebugger();
			}
			debugSupport.setDebugger(debugger);
		}

		JPAAdapter getMappingAdapter() {
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
			return dpi;
		}
	}

	private static class JPADebugSupportWrapper implements DebugSupport {

		final private DebugSupport debugSupport;
		private JPAServiceDebugger debugger;

		public JPADebugSupportWrapper(final DebugSupport debugSupport) {
			super();
			this.debugSupport = debugSupport;
		}

		@Override
		public void init(final OData odata) {
			debugSupport.init(odata);
		}

		@Override
		public boolean isUserAuthorized() {
			return debugSupport.isUserAuthorized();
		}

		@Override
		public ODataResponse createDebugResponse(final String debugFormat, final DebugInformation debugInfo) {
			debugInfo.getRuntimeInformation().addAll(debugger.getRuntimeInformation());
			return debugSupport.createDebugResponse(debugFormat, debugInfo);
		}

		void setDebugger(final JPAServiceDebugger debugger) {
			this.debugger = debugger;
		}
	}

	private static class JPAODataHttpHandlerImpl extends ODataHttpHandlerImpl {

		private final EntityManager em;
		private final JPAODataContextImpl context;
		private final SecurityInceptor securityInceptor;

		public JPAODataHttpHandlerImpl(final JPAODataContextImpl context, final SecurityInceptor securityInceptor) {
			super(context.getOdata(), context.getServiceMetaData());
			this.context = context;
			this.em = context.getMappingAdapter().createEntityManager();
			this.securityInceptor = securityInceptor;
		}

		EntityManager getEntityManager() {
			return em;
		}

		@Override
		public ODataResponse process(final ODataRequest request) {
			final JPAAdapter mappingAdapter = context.getMappingAdapter();
			context.getDependencyInjector().registerDependencyMapping(EntityManager.class, em);

			checkSecurity(request);

			try {
				mappingAdapter.beginTransaction(em);
				final ODataResponse odataResponse = super.process(request);
				// TODO at this point the response is already sent to the client, but we have to
				// commit and commit may fail... how can we manage that?
				if (odataResponse.getStatusCode() >= 200 && odataResponse.getStatusCode() < 300) {
					mappingAdapter.commitTransaction(em);
				} else {
					LOG.log(Level.WARNING, "Do not commit request transaction, because response is not 2xx");
					mappingAdapter.cancelTransaction(em);
				}
				return odataResponse;
			} catch (final RuntimeException ex) {
				// do not commit on exceptions
				mappingAdapter.cancelTransaction(em);
				throw ex;
			}
		}

		private void checkSecurity(final ODataRequest request) throws RuntimeException {
			if (securityInceptor == null) {
				return;
			}
			try {
				context.getDependencyInjector().injectFields(securityInceptor);
				final UriInfo uriInfo = new Parser(context.getServiceMetaData().getEdm(), context.getOdata())
						.parseUri(request.getRawODataPath(),
								request.getRawQueryPath(), null, request.getRawBaseUri());
				securityInceptor.authorize(request, uriInfo);
			} catch (final ODataLibraryException | ODataApplicationException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
