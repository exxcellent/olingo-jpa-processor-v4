package org.apache.olingo.jpa.processor.core.exception;

import static org.junit.Assert.assertEquals;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAMessageKey;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Test;

public class TestODataJPAProcessorException {

	public static enum MessageKeys implements ODataJPAMessageKey {
		RESULT_NOT_FOUND;

		@Override
		public String getKey() {
			return name();
		}

	}

	@Test
	public void checkSimpleProcessorExeption() {
		try {
			throw new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_UPDATE,
			        HttpStatusCode.CONFLICT);
		} catch (final ODataApplicationException e) {
			assertEquals(HttpStatusCode.CONFLICT.getStatusCode(), e.getStatusCode());
		}
	}

	@Test
	public void checkSimpleRaiseExeption() {
		try {
			throw new ODataJPASerializerException(ODataJPASerializerException.MessageKeys.RESULT_NOT_FOUND,
			        HttpStatusCode.BAD_REQUEST);
		} catch (final ODataApplicationException e) {
			assertEquals("No result was fond by Serializer", e.getMessage());
			assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
		}
	}

	@Test
	public void checkSimpleViaMessageKeyRaiseExeption() {
		try {
			throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.PARAMETER_CONVERSION_ERROR,
			        HttpStatusCode.INTERNAL_SERVER_ERROR, "Willi", "Hugo");
		} catch (final ODataApplicationException e) {
			assertEquals("Unable to convert value Willi of parameter Hugo", e.getMessage());
			assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
		}
	}

}
