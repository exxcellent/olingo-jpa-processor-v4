package org.apache.olingo.jpa.processor.core.api;

import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.core.ODataExceptionHelper;
import org.apache.olingo.server.core.ODataHandlerImpl;
import org.apache.olingo.server.core.ODataHttpHandlerImpl;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;
import org.apache.olingo.server.core.uri.parser.Parser;
import org.apache.olingo.server.core.uri.parser.UriParserException;
import org.apache.olingo.server.core.uri.parser.UriParserSemanticException;
import org.apache.olingo.server.core.uri.parser.UriParserSyntaxException;
import org.apache.olingo.server.core.uri.validator.UriValidationException;

/**
 * @see org.apache.olingo.server.core.ODataHttpHandlerImpl
 */
class JPAODataHttpHandlerImpl extends ODataHttpHandlerImpl {

	private final JPAODataServletHandler servletHandler;
	private final JPAODataContextImpl context;
	private final EntityManager em;

	public JPAODataHttpHandlerImpl(final JPAODataServletHandler servletHandler) {
		super(servletHandler.getJPAODataContext().getOdata(),
		        ((JPAODataContextImpl) servletHandler.getJPAODataContext()).getServiceMetaData());
		this.servletHandler = servletHandler;
		this.context = (JPAODataContextImpl) servletHandler.getJPAODataContext();
		this.em = context.refreshMappingAdapter().createEntityManager();
	}

	EntityManager getEntityManager() {
		return em;
	}

	@Override
	public ODataResponse process(final ODataRequest request) {
		context.getDependencyInjector().registerDependencyMapping(EntityManager.class, em);

		try {
			checkSecurity(request);
		} catch (final UriParserException e) {
			JPAODataServletHandler.LOG.log(Level.FINE, "Failed to preprocess request for security checks", e);
			return wrapIntoErrorResponse(request, e);
		} catch (final ODataLibraryException | ODataApplicationException e) {
			JPAODataServletHandler.LOG.log(Level.FINER, "Security check failed", e);
			return wrapIntoErrorResponse(request, e);
		}

		final JPAAdapter mappingAdapter = context.refreshMappingAdapter();
		try {
			mappingAdapter.beginTransaction(em);

			final ODataResponse odataResponse = super.process(request);

			// TODO at this point the response is already sent to the client, but we have to
			// commit and commit may fail... how can we manage that?
			if (odataResponse.getStatusCode() >= 200 && odataResponse.getStatusCode() < 300) {
				mappingAdapter.commitTransaction(em);
			} else {
				JPAODataServletHandler.LOG.log(Level.WARNING, "Do not commit request transaction, because response is not 2xx");
				mappingAdapter.cancelTransaction(em);
			}
			// give implementors the chance to modify the response (set cache control etc.)
			servletHandler.modifyResponse(odataResponse);
			return odataResponse;
		} catch (final RuntimeException ex) {
			// do not commit on exceptions
			mappingAdapter.cancelTransaction(em);
			throw ex;
		}
	}

	private ODataResponse wrapIntoErrorResponse(final ODataRequest request, final ODataException ex) {
		ODataServerError serverError;
		// rethrow exception to simplify handling of exception occurred while security
		// check
		try {
			throw ex;
		} catch (final ODataApplicationException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e);
		} catch (final UriValidationException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e, null);
		} catch (final UriParserSemanticException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e, null);
		} catch (final UriParserSyntaxException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e, null);
		} catch (final UriParserException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e, null);
		} catch (final ODataLibraryException e) {
			serverError = ODataExceptionHelper.createServerErrorObject(e, null);
		} catch (final ODataException e) {
			throw new UnsupportedOperationException(e);
		}
		// duplicate usage of classes not accessible on super class to delegate
		// exception handling to other handler
		final ODataResponse errorResponse = new ODataResponse();
		final ServerCoreDebugger debugger = new ServerCoreDebugger(context.getOdata());
		final ODataHandlerImpl handler = new ODataHandlerImpl(context.getOdata(), context.getServiceMetaData(),
		        debugger);
		handler.handleException(request, errorResponse, serverError, ex);
		return errorResponse;
	}

	private void checkSecurity(final ODataRequest request) throws ODataLibraryException, ODataApplicationException {
		final SecurityInceptor securityInceptor = servletHandler.getSecurityInceptor();
		if (securityInceptor == null) {
			return;
		}
		context.getDependencyInjector().injectFields(securityInceptor);
		final UriInfo uriInfo = new Parser(context.getServiceMetaData().getEdm(), context.getOdata())
		        .parseUri(request.getRawODataPath(),
		                request.getRawQueryPath(), null, request.getRawBaseUri());
		securityInceptor.authorize(request, uriInfo);
		// prepare the principal for DPI in case of a happened authentication
		final HttpServletRequest httpRequest = context.getDependencyInjector().getDependencyValue(HttpServletRequest.class);
		context.getDependencyInjector().registerDependencyMapping(java.security.Principal.class, httpRequest.getUserPrincipal());
	}
}