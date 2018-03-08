package org.apache.olingo.jpa.processor.core.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
  public void checkSimpleRaiseExeption() {
    try {
      RaiseExeption();
    } catch (final ODataApplicationException e) {
      assertEquals("No result was fond by Serializer", e.getMessage());
      assertEquals(400, e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void checkSimpleViaMessageKeyRaiseExeption() {
    try {
      RaiseExeptionWithParam();
    } catch (final ODataApplicationException e) {
      assertEquals("Unable to convert value Willi of parameter Hugo", e.getMessage());
      assertEquals(500, e.getStatusCode());
      return;
    }
    fail();
  }

  private void RaiseExeptionWithParam() throws ODataJPAProcessException {
    throw new ODataJPADBAdaptorException(ODataJPADBAdaptorException.MessageKeys.PARAMETER_CONVERSION_ERROR,
        HttpStatusCode.INTERNAL_SERVER_ERROR, "Willi", "Hugo");
  }

  private void RaiseExeption() throws ODataJPAProcessException {
    throw new ODataJPASerializerException(ODataJPASerializerException.MessageKeys.RESULT_NOT_FOUND,
        HttpStatusCode.BAD_REQUEST);
  }

}
