package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;

/**
 * The implementor to handle HTTP servlet requests as an OData REST API.
 *
 * @author Ralf Zozmann
 *
 */
public class JPAODataServletHandler extends JPAODataGetHandler {

	public JPAODataServletHandler(final JPAAdapter mappingAdapter) throws ODataException {
		super(mappingAdapter);
	}

}
