package org.apache.olingo.jpa.client.example;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.apache.olingo.client.api.uri.FilterFactory;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.api.uri.URIFilter;
import org.apache.olingo.jpa.client.example.util.HandlerTestBase;
import org.apache.olingo.jpa.processor.core.testmodel.PersonDto;
import org.apache.olingo.jpa.processor.core.testmodel.PersonHandler;
import org.apache.olingo.jpa.processor.core.testmodel.PersonImageMeta;
import org.apache.olingo.jpa.processor.core.testmodel.PersonMeta;
import org.apache.olingo.jpa.processor.core.testmodel.PersonURIBuilder;
import org.apache.olingo.jpa.processor.core.testmodel.PostalAddressDataMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PersonHandlerTest extends HandlerTestBase {

  @Test
  public void testInvalidLoadEntitySetPerson() throws Exception {
    final PersonHandler handler = createLocalPersonAccess();
    final FilterFactory ff = handler.getFilterFactory();
    final URIFilter filter = ff.not(ff.eq(ff.getArgFactory().literal(PersonMeta.ID_NAME), ff.getArgFactory().literal(
        "97")));
    final PersonURIBuilder uriBuilder = handler.defineEndpoint().filter(filter);

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> handler.retrieve(uriBuilder));
  }

  @Test
  public void testLoadPerson() throws Exception {
    final PersonHandler handler = createLocalPersonAccess();
    final Map<QueryOption, Object> expandImage1Options = new HashMap<>();
    expandImage1Options.put(QueryOption.EXPAND, PersonImageMeta.OWNINGPERSON_NAME);
    final PersonURIBuilder uriBuilder = handler.defineEndpoint().appendKeySegment("97").expand(PersonMeta.ADDRESS_NAME
        + "/" + PostalAddressDataMeta.ADMINISTRATIVEDIVISION_NAME).expandWithOptions(
            PersonMeta.IMAGE1_NAME, expandImage1Options);
    final PersonDto dto = handler.retrieve(uriBuilder);
    Assertions.assertNotNull(dto);
    Assertions.assertEquals(1, dto.getPhoneNumbers().size());
    Assertions.assertEquals(dto.getPhoneNumbersAsString().size(), dto.getPhoneNumbers().size());
    Assertions.assertEquals("CHE", dto.getCountry());
    Assertions.assertEquals(Timestamp.valueOf("2016-07-20 09:21:23.0"), dto.getCreationDateTime());
    Assertions.assertNotNull(dto.getAddress());
    Assertions.assertEquals("23", dto.getAddress().getHouseNumber());
    Assertions.assertNotNull(dto.getAdministrativeInformation());
    Assertions.assertNotNull(dto.getAdministrativeInformation().getCreated());
    Assertions.assertEquals("99", dto.getAdministrativeInformation().getCreated().getBy());
    Assertions.assertNotNull(dto.getAdministrativeInformation().getUpdated());
    Assertions.assertNotNull(dto.getAddress().getAdministrativeDivision());
    Assertions.assertEquals("CH-BL", dto.getAddress().getAdministrativeDivision().getDivisionCode());

    Assertions.assertNotNull(dto.getImage1());
    Assertions.assertEquals("97", dto.getImage1().getPID());
    Assertions.assertNotNull(dto.getImage1().getOwningPerson());
    Assertions.assertEquals(dto.getImage1().getOwningPerson().getID(), dto.getID());
    Assertions.assertNull(dto.getImage1().getPersonReferenceWithoutMappedAttribute());
    Assertions.assertNull(dto.getImage1().getPersonWithDefaultIdMapping());
    Assertions.assertNotNull(dto.getImage1().getAdministrativeInformation());
    Assertions.assertNotNull(dto.getImage1().getAdministrativeInformation().getCreated());
    Assertions.assertNotNull(dto.getImage1().getAdministrativeInformation().getUpdated());
    Assertions.assertEquals("John Doe", dto.getImage1().getAdministrativeInformation().getUpdated().getBy());
  }

  @Test
  public void testUpdatePerson() throws Exception {
    final PersonHandler handler = createLocalPersonAccess();
    final PersonURIBuilder uriBuilder = handler.defineEndpoint().appendKeySegment("97").expand(PersonMeta.ADDRESS_NAME
        + "/" + PostalAddressDataMeta.ADMINISTRATIVEDIVISION_NAME, PersonMeta.IMAGE1_NAME);
    final PersonDto modifiedPerson = handler.retrieve(uriBuilder);
    modifiedPerson.setCountry("POL");
    modifiedPerson.setLastName("NewLastName");
    // TODO Olingo's server side deserializer doesn't suppport relationships (navigation property) as part of an
    // (embedded) complex type
    modifiedPerson.getAddress().setAdministrativeDivision(null);
    final PersonDto updatedPerson = handler.update(modifiedPerson);
    Assertions.assertEquals(modifiedPerson.getID(), updatedPerson.getID());
  }

}
