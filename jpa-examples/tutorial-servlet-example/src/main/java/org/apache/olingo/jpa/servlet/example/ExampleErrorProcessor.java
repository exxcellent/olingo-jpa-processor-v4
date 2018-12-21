package org.apache.olingo.jpa.servlet.example;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.processor.DefaultProcessor;
import org.apache.olingo.server.api.processor.ErrorProcessor;

/**
 * Example processor logging errors.
 *
 * @author Ralf Zozmann
 *
 */
public class ExampleErrorProcessor extends DefaultProcessor implements ErrorProcessor {

	Logger LOG = Logger.getLogger(ErrorProcessor.class.getName());

	@Override
	public void processError(final ODataRequest request, final ODataResponse response, final ODataServerError serverError,
			final ContentType responseFormat) {
		LOG.log(Level.SEVERE, serverError.getMessage(), serverError.getException());
		super.processError(request, response, serverError, responseFormat);
	}

}
