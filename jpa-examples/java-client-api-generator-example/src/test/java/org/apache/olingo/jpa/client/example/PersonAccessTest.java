package org.apache.olingo.jpa.client.example;

import org.apache.olingo.jpa.processor.core.testmodel.AbstractDatatypeConversionEntityAccess;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntityDto;
import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.junit.Assert;
import org.junit.Test;

/**
 * Simple unit tests for generated code. For real world integration test see <i>olingo-generic-servlet-example</i>.
 * @author Ralf Zozmann
 *
 */
public class PersonAccessTest extends AccessTestBase {

  @Test
  public void testLoadPerson() throws Exception {
    final PersonAccess endpoint = createLocalPersonAccess();
    final PersonDto dto = endpoint.retrieve("99");
    Assert.assertNotNull(dto);
    Assert.assertEquals(3, dto.getPhoneNumbers().size());
  }


  @Test
  public void testLoadDatatypeConversionEntity() throws Exception {
    final AbstractDatatypeConversionEntityAccess endpoint = createLocalEntityAccess(
        AbstractDatatypeConversionEntityAccess.class);
    final DatatypeConversionEntityDto dto = endpoint.retrieve(Integer.valueOf(2));
    Assert.assertNotNull(dto);
    Assert.assertEquals(Integer.valueOf(2000), dto.getAIntegerYear());
  }

}
