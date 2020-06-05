package org.apache.olingo.jpa.client.example;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.jpa.client.example.util.AccessTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.PersonAccess;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.apache.olingo.jpa.processor.core.testmodel.PersonURIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class PersonAccessTest extends AccessTestBase {

  @Test
  public void testLoadPerson() throws Exception {
    final PersonAccess access = createLocalPersonAccess();
    final Map<QueryOption, Object> expandImage1Options = new HashMap<>();
    expandImage1Options.put(QueryOption.EXPAND, "OwningPerson");
    final PersonURIBuilder uriBuilder = access.defineEndpoint().appendKeySegment("97").expandWithOptions("Image1",
        expandImage1Options);
    final PersonDto dto = access.retrieve(uriBuilder);
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

    Assert.assertNotNull(dto.getImage1());
    Assert.assertEquals("97", dto.getImage1().getPID());
    Assert.assertNotNull(dto.getImage1().getOwningPerson());
    Assert.assertEquals(dto.getImage1().getOwningPerson().getID(), dto.getID());
    Assert.assertNull(dto.getImage1().getPersonReferenceWithoutMappedAttribute());
    Assert.assertNull(dto.getImage1().getPersonWithDefaultIdMapping());
    Assert.assertNotNull(dto.getImage1().getAdministrativeInformation());
    Assert.assertNotNull(dto.getImage1().getAdministrativeInformation().getCreated());
    Assert.assertNotNull(dto.getImage1().getAdministrativeInformation().getUpdated());
    Assert.assertEquals("John Doe", dto.getImage1().getAdministrativeInformation().getUpdated().getBy());
  }

}
