package org.apache.olingo.jpa.client.example;

import org.apache.olingo.jpa.client.example.util.AccessTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.junit.Assert;
import org.junit.Test;

public class PersonAccessTest extends AccessTestBase {

  @Test
  public void testLoadPerson() throws Exception {
    final PersonAccess endpoint = createLocalPersonAccess();
    final PersonDto dto = endpoint.retrieve("96");
    Assert.assertNotNull(dto);
    Assert.assertEquals(3, dto.getPhoneNumbers().size());
  }

}
