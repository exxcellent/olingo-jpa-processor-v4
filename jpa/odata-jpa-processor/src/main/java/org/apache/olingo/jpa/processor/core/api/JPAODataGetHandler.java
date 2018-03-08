package org.apache.olingo.jpa.processor.core.api;

import java.util.ArrayList;
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
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.ServiceDocument;
import org.apache.olingo.jpa.processor.core.database.JPADefaultDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPAODataDatabaseOperations;
import org.apache.olingo.jpa.processor.core.mapping.JPAPersistenceAdapter;
import org.apache.olingo.jpa.processor.core.processor.JPAEntityProcessor;
import org.apache.olingo.jpa.processor.core.processor.JPAODataActionProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.debug.DebugInformation;
import org.apache.olingo.server.api.debug.DebugSupport;
import org.apache.olingo.server.api.debug.DefaultDebugSupport;
import org.apache.olingo.server.api.processor.Processor;
import org.apache.olingo.server.core.ODataHttpHandlerImpl;

public class JPAODataGetHandler {
	private final JPAODataContext context;
	private final OData odata;
	private final Logger log = Logger.getLogger(JPAODataGetHandler.class.getName());
	private final JPAPersistenceAdapter mappingAdapter;

	public JPAODataGetHandler(final JPAPersistenceAdapter mappingAdapter) throws ODataException {
		super();
		this.mappingAdapter = mappingAdapter;
		this.context = new JPAODataContextImpl();
		this.odata = OData.newInstance();

	}

	public JPAODataContext getJPAODataContext() {
		return context;
	}

	public void process(final HttpServletRequest request, final HttpServletResponse response) {
		final JPAODataHttpHandlerImpl handler = new JPAODataHttpHandlerImpl();
		context.getEdmProvider().setRequestLocales(request.getLocales());
		context.initDebugger(request.getParameter(DebugSupport.ODATA_DEBUG_QUERY_PARAMETER));
		handler.register(context.getDebugSupport());
		final Collection<Processor> processors = collectProcessors(request, response, handler.getEntityManager());
		for(final Processor p: processors) {
			handler.register(p);
		}
		handler.process(request, response);
	}

	/**
	 * Client expendable list of processors.
	 *
	 * @return The collection of processors to use to handle the request.
	 */
	//TODO replace EntityManager by JPAPersistenceAdapter
	protected Collection<Processor> collectProcessors(final HttpServletRequest request, final HttpServletResponse response, final EntityManager em) {
		final Collection<Processor> processors = new LinkedList<>();
		processors.add(new JPAEntityProcessor(context, em));
		processors.add(new JPAODataRequestProcessor(context, em));
		processors.add(new JPAODataActionProcessor(context, em));
		processors.add(new JPAODataBatchProcessor());
		return processors;
	}

	private class JPAODataContextImpl implements JPAODataContext {
		private List<EdmxReference> references = new ArrayList<EdmxReference>();
		private JPADebugSupportWrapper debugSupport = new JPADebugSupportWrapper(new DefaultDebugSupport());
		private JPAODataDatabaseOperations operationConverter;
		private final JPAEdmProvider jpaEdm;
		private JPAODataDatabaseProcessor databaseProcessor;
		private JPAServiceDebugger debugger;

		public JPAODataContextImpl() throws ODataException {
			super();

			operationConverter = new JPADefaultDatabaseProcessor();
			jpaEdm = new JPAEdmProvider(mappingAdapter.getNamespace(), mappingAdapter.getMetamodel());
			databaseProcessor = mappingAdapter.getDatabaseAccessor();
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
		public void setReferences(final List<EdmxReference> references) {
			this.references = references;
		}

		@Override
		public void setMetadataPostProcessor(final JPAEdmMetadataPostProcessor postProcessor) throws ODataException {
			ServiceDocument.setPostProcessor(postProcessor);
		}

		@Override
		public JPAEdmProvider getEdmProvider() {
			return jpaEdm;
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
	}

	private class JPADebugSupportWrapper implements DebugSupport {

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

	private class JPAODataHttpHandlerImpl extends ODataHttpHandlerImpl {

		private final EntityManager em;

		public JPAODataHttpHandlerImpl() {
			super(odata, odata.createServiceMetadata(context.getEdmProvider(), context.getReferences()));
			this.em = mappingAdapter.createEntityManager();
		}

		EntityManager getEntityManager() {
			return em;
		}

		@Override
		public ODataResponse process(final ODataRequest request) {
			try {
				mappingAdapter.beginTransaction(em);
				final ODataResponse odataResponse = super.process(request);
				// TODO at this point the response is already sent to the client, but we have to
				// commit and commit may fail... how can we manage that?
				if (odataResponse.getStatusCode() >= 200 && odataResponse.getStatusCode() < 300) {
					mappingAdapter.commitTransaction(em);
				} else {
					log.log(Level.WARNING, "Do not commit request transaction, because response is not 2xx");
					mappingAdapter.cancelTransaction(em);
				}
				return odataResponse;
			} catch (final RuntimeException ex) {
				// do not commit on exceptions
				mappingAdapter.cancelTransaction(em);
				throw ex;
			}
		}
	}
}
