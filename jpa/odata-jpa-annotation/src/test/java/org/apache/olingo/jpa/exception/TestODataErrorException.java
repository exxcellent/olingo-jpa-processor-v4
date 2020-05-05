package org.apache.olingo.jpa.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.olingo.commons.api.ex.ODataError;
import org.junit.Test;

public class TestODataErrorException {

  @Test
  public void testDefaultLocale() {
    final ODataError error = new ODataError().setMessage("foo");
    final ODataErrorException ex = new ODataErrorException(error, null);
    assertNull(ex.getLocale());
    assertEquals(error.getMessage(), ex.getMessage());
    assertEquals(error, ex.getError());
  }

}
