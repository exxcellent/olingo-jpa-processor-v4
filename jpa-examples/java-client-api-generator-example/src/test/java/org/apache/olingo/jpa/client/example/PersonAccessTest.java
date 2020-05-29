package org.apache.olingo.jpa.client.example;

import java.sql.Timestamp;

import org.apache.olingo.jpa.client.example.util.AccessTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.junit.Assert;
import org.junit.Test;

public class PersonAccessTest extends AccessTestBase {

  @Test
  public void testLoadPerson() throws Exception {
    final PersonAccess endpoint = createLocalPersonAccess();
    final PersonDto dto = endpoint.retrieve("97");
    Assert.assertNotNull(dto);
    Assert.assertEquals(1, dto.getPhoneNumbers().size());
    Assert.assertEquals(dto.getPhoneNumbersAsString().size(), dto.getPhoneNumbers().size());
    Assert.assertEquals("CHE", dto.getCountry());
    Assert.assertEquals(Timestamp.valueOf("2016-07-20 09:21:23.0"), dto.getCreationDateTime());
    Assert.assertNotNull(dto.getAddress());
    Assert.assertEquals("23", dto.getAddress().getHouseNumber());
    Assert.assertNotNull(dto.getAdministrativeInformation());
    Assert.assertNotNull(dto.getAdministrativeInformation().getCreated());
    Assert.assertEquals("99", dto.getAdministrativeInformation().getCreated().getBy());
    Assert.assertNotNull(dto.getAdministrativeInformation().getUpdated());
    // TODO relationships
  }

}
