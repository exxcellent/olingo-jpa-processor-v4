package org.apache.olingo.jpa.client.example;

import org.apache.olingo.jpa.client.example.util.AccessTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.AbstractDatatypeConversionEntityAccess;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntityDto;
import org.junit.Assert;
import org.junit.Test;

public class DatatypeConversionEntityAccessTest extends AccessTestBase {

  @Test
  public void testLoadDatatypeConversionEntity() throws Exception {
    final AbstractDatatypeConversionEntityAccess endpoint = createLocalEntityAccess(
        AbstractDatatypeConversionEntityAccess.class);
    final DatatypeConversionEntityDto dto = endpoint.retrieve(Integer.valueOf(2));
    Assert.assertNotNull(dto);
    Assert.assertEquals(Integer.valueOf(2000), dto.getAIntegerYear());
  }

}
