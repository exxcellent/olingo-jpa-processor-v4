package org.apache.olingo.jpa.servlet.example;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.processor.DefaultProcessor;
import org.apache.olingo.server.api.processor.ErrorProcessor;

public class ExampleErrorProcessor extends DefaultProcessor implements ErrorProcessor {

	Logger LOG = Logger.getLogger(ErrorProcessor.class.getName());
	
	@Override
	public void processError(ODataRequest request, ODataResponse response, ODataServerError serverError,
			ContentType responseFormat) {
		LOG.log(Level.SEVERE, serverError.getMessage(), serverError.getException());
		super.processError(request, response, serverError, responseFormat);		
	}

}
