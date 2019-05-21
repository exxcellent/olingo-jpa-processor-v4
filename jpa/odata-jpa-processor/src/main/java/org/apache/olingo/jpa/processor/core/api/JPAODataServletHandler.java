package org.apache.olingo.jpa.processor.core.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.processor.JPAEntityProcessor;
import org.apache.olingo.jpa.processor.core.processor.JPAODataActionProcessor;
import org.apache.olingo.jpa.processor.core.security.AnnotationBasedSecurityInceptor;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.processor.Processor;

/**
 * The implementor to handle HTTP servlet requests as an OData REST API.
 *
 * @author Ralf Zozmann
 *
 */
public class JPAODataServletHandler {

	static final Logger LOG = Logger.getLogger(JPAODataServletHandler.class.getName());

	private final JPAODataContextImpl context;
	private SecurityInceptor securityInceptor = new AnnotationBasedSecurityInceptor();// having one as default

	public JPAODataServletHandler(final JPAAdapter mappingAdapter) throws ODataException {
		super();
		this.context = new JPAODataContextImpl(mappingAdapter);
	}

	public final JPAODataSessionContextAccess getJPAODataContext() {
		return context;
	}

	public void process(final HttpServletRequest request, final HttpServletResponse response) {
		final DependencyInjector dpi = new DependencyInjector();
		dpi.registerDependencyMapping(HttpServletRequest.class, request);
		dpi.registerDependencyMapping(HttpServletResponse.class, response);
		prepareDependencyInjection(dpi);
		context.initDependencyInjection(dpi);

		final JPAODataHttpHandlerImpl handler = new JPAODataHttpHandlerImpl(this);
		context.initializeRequestContext(request);
		handler.register(context.getDebugSupport());

		final Collection<Processor> processors = collectProcessors(request, response, handler.getEntityManager());
		for (final Processor p : processors) {
			handler.register(p);
		}
		handler.process(request, response);
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
	 * Client hook method to add custom resources as dependencies for dependency
	 * injection support.
	 *
	 * @param dpi
	 *            The injector used to handle injection of registered dependencies.
	 */
	protected void prepareDependencyInjection(final DependencyInjector dpi) {
		// do nothing in default implementation
	}

	public void dispose() {
		context.dispose();
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
	// TODO replace EntityManager by JPAAdapter
	protected Collection<Processor> collectProcessors(final HttpServletRequest request, final HttpServletResponse response,
	        final EntityManager em) {
		final Collection<Processor> processors = new LinkedList<>();
		processors.add(new JPAEntityProcessor(context, em));
		processors.add(new JPAODataRequestProcessor(context, em));
		processors.add(new JPAODataActionProcessor(context, em));
		processors.add(new JPAODataBatchProcessor());
		return processors;
	}

}
