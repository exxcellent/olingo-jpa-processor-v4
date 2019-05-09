package org.apache.olingo.jpa.exception;

import org.apache.olingo.commons.api.ex.ODataError;
import org.apache.olingo.commons.api.ex.ODataException;

/**
 * 'Helper' exception to transport more details about internal state of business logic to clients. Mostly used for OData actions.
 *
 */
public class ODataErrorException extends ODataException {

	private static final long serialVersionUID = -2759540990572142254L;

	private final ODataError error;

	public ODataErrorException(final ODataError error) {
		super(error.getMessage());
		this.error = error;
	}

	public ODataError getError() {
		return error;
	}

}
