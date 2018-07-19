package org.apache.olingo.jpa.servlet.example;

import org.apache.olingo.jpa.processor.core.security.RequestSecurityHandler;
import org.apache.olingo.jpa.processor.core.security.SecurityInceptor;
import org.apache.olingo.server.api.ODataRequest;

public class ExampleSecurityInceptor implements SecurityInceptor {

	@Override
	public RequestSecurityHandler createRequestSecurityHandler(ODataRequest odRequest) {
		// TODO Auto-generated method stub
		return null;
	}

}
