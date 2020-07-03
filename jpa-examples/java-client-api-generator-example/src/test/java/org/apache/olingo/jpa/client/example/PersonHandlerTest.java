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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PersonHandlerTest extends HandlerTestBase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testInvalidLoadEntitySetPerson() throws Exception {
    final PersonHandler handler = createLocalPersonAccess();
    final FilterFactory ff = handler.getFilterFactory();
    final URIFilter filter = ff.not(ff.eq(ff.getArgFactory().literal(PersonMeta.ID_NAME), ff.getArgFactory().literal(
        "97")));
    final PersonURIBuilder uriBuilder = handler.defineEndpoint().filter(filter);

    thrown.expect(IllegalArgumentException.class);
    handler.retrieve(uriBuilder);
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
    Assert.assertNotNull(dto.getAddress().getAdministrativeDivision());
    Assert.assertEquals("CH-BL", dto.getAddress().getAdministrativeDivision().getDivisionCode());

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

  @Test
  public void testUpdatePerson() throws Exception {
    final PersonHandler handler = createLocalPersonAccess();
    final PersonURIBuilder uriBuilder = handler.defineEndpoint().appendKeySegment("97").expand(PersonMeta.ADDRESS_NAME
        + "/" + PostalAddressDataMeta.ADMINISTRATIVEDIVISION_NAME, PersonMeta.IMAGE1_NAME);
    final PersonDto modifiedPerson = handler.retrieve(uriBuilder);
    modifiedPerson.setCountry("POL");
    final PersonDto updatedPerson = handler.update(modifiedPerson);
  }

}
