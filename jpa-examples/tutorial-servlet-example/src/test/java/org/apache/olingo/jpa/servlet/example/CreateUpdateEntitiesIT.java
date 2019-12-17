package org.apache.olingo.jpa.servlet.example;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.logging.Logger;

import javax.persistence.Entity;

import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.ChangeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.testmodel.Phone;
import org.apache.olingo.jpa.test.util.Constant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Ralf Zozmann
 *
 * @see https://templth.wordpress.com/2014/12/03/accessing-odata-v4-service-with-olingo/
 *
 */
public class CreateUpdateEntitiesIT {

  private final Logger log = Logger.getLogger(CreateUpdateEntitiesIT.class.getName());

  private ODataEndpointTestDefinition endpoint;

  @Before
  public void setup() {
    endpoint = new ODataEndpointTestDefinition();
  }

  @Test
  public void test1() {
    URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons");
    // create
    final ClientEntity entity = createPerson();
    final ODataEntityCreateResponse<ClientEntity> responseCreate = endpoint.createEntity(uriBuilder, entity);
    Assert.assertTrue(responseCreate.getStatusCode() == HttpStatusCode.CREATED.getStatusCode());
    final ClientEntity bodyCreate = responseCreate.getBody();
    responseCreate.close();
    Assert.assertNotNull(bodyCreate);
    //update
    final String id = entity.getProperty("ID").getPrimitiveValue().toString();
    uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment(id);
    entity.getProperties().clear();
    replaceProperty(entity, "FirstName", "modified name");
    final ODataEntityUpdateResponse<ClientEntity> responseUpdate = endpoint.updateEntity(uriBuilder, entity);
    Assert.assertTrue(responseUpdate.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final ClientEntity bodyUpdate = responseUpdate.getBody();
    responseUpdate.close();
    Assert.assertNotNull(bodyUpdate);
  }

  @Test
  public void test2() {
    final ClientEntity bodyCreate = createDatatypeConversionEntity((int) System.currentTimeMillis());
    Assert.assertNotNull(bodyCreate);
  }

  @Ignore("Activate only on demand")
  @Test
  public void testMassCreation() throws Exception {
    // create
    final int number = 10000;
    long start = System.currentTimeMillis();
    final int idOffset = (int) System.currentTimeMillis();
    for (int i = 0; i < number; i++) {
      final ClientEntity bodyCreate = createDatatypeConversionEntity(idOffset + i);
      Assert.assertNotNull(bodyCreate);
    }
    log.info((System.currentTimeMillis() - start) / 1000 + " sec to create " + number + " entities");

    // count
    URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("DatatypeConversionEntities").count();
    final ODataRetrieveResponse<ClientPrimitiveValue> responseCount = endpoint.retrieveValue(uriBuilder,
        "Count entities in database");
    Assert.assertTrue(responseCount.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    final String sCount = responseCount.getBody().toCastValue(String.class);
    final int iCount = Integer.valueOf(sCount).intValue();
    Assert.assertTrue(iCount > 0);
    responseCount.close();

    // load
    start = System.currentTimeMillis();
    uriBuilder = endpoint.newUri().appendEntitySetSegment("DatatypeConversionEntities");
    final ODataRetrieveResponse<ClientEntitySet> responseSelectAll = endpoint.retrieveEntityCollection(uriBuilder,
        "Load all data conversion entities");
    Assert.assertTrue(responseSelectAll.getStatusCode() == HttpStatusCode.OK.getStatusCode());
    //		final ClientEntitySet body = response.getBody();
    final InputStream is = responseSelectAll.getRawResponse();
    int size = 0;
    while (is.read() != -1) {
      size++;
    }
    responseSelectAll.close();
    //		System.out.println("Read " + body.getEntities().size() + " entities in "
    //				+ (System.currentTimeMillis() - start) / 1000 + " sec");
    final long duration = (System.currentTimeMillis() - start) / 1000;
    log.info("Read " + iCount + " entities in " + size + " bytes in " + duration + " sec");
    Assert.assertTrue("Loading 10000 entries takes more than 4 seconds", duration < 4);
  }

  private ClientEntity createDatatypeConversionEntity(final int ID) {
    final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("DatatypeConversionEntities");
    final ClientObjectFactory factory = endpoint.getObjectFactory();
    final FullQualifiedName fqn = new FullQualifiedName(Constant.PUNIT_NAME,
        DatatypeConversionEntity.class.getAnnotation(Entity.class).name());
    final ClientEntity entity = factory.newEntity(fqn);
    ClientProperty property;

    property = factory.newPrimitiveProperty("ID",
        factory.newPrimitiveValueBuilder().buildInt32(Integer.valueOf(ID)));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ADate1", factory.newPrimitiveValueBuilder().buildString("1610-10-11"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ADate3", factory.newPrimitiveValueBuilder().buildString("0610-01-01"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ATimestamp1SqlTimestamp",
        factory.newPrimitiveValueBuilder().buildString("2016-01-20T09:21:23+01:00"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ATimestamp2",
        factory.newPrimitiveValueBuilder().buildString("2026-01-20T00:21:23"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ATime1", factory.newPrimitiveValueBuilder().buildString("22:19:40"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ADecimal",
        factory.newPrimitiveValueBuilder().buildDecimal(BigDecimal.valueOf(17.12345)));
    entity.getProperties().add(property);

    // do something dynamic
    property = factory.newPrimitiveProperty("AUrl",
        factory.newPrimitiveValueBuilder()
        .buildString("http://www.anywhere.org/reverse-" + Long.toString(ID) + ".pdf"));
    entity.getProperties().add(property);

    property = factory.newEnumProperty("AEnumFromOtherPackage",
        factory.newEnumValue("org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum", "Three"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("AIntBoolean",
        factory.newPrimitiveValueBuilder().buildBoolean(Boolean.TRUE));
    entity.getProperties().add(property);

    final ODataEntityCreateResponse<ClientEntity> responseCreate = endpoint.createEntity(uriBuilder, entity);
    Assert.assertTrue(responseCreate.getStatusCode() == HttpStatusCode.CREATED.getStatusCode());
    final ClientEntity bodyCreate = responseCreate.getBody();
    responseCreate.close();
    return bodyCreate;
  }

  private void replaceProperty(final ClientEntity entity, final String propertyName, final String newValue) {
    final ClientObjectFactory factory = endpoint.getObjectFactory();
    ClientProperty property = entity.getProperty(propertyName);
    if(property != null) {
      entity.getProperties().remove(property);
    }
    property = factory.newPrimitiveProperty(propertyName, factory.newPrimitiveValueBuilder().buildString(newValue));
    entity.getProperties().add(property);

  }

  private ClientEntity createPerson() {
    final ClientObjectFactory factory = endpoint.getObjectFactory();
    final FullQualifiedName fqn = new FullQualifiedName(Constant.PUNIT_NAME, Person.class.getAnnotation(Entity.class).name());
    final ClientEntity entity = factory.newEntity(fqn);
    ClientProperty property;

    property = factory.newPrimitiveProperty("FirstName", factory.newPrimitiveValueBuilder().buildString("myFirstName"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("Country", factory.newPrimitiveValueBuilder().buildString("DEU"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("Type", factory.newPrimitiveValueBuilder().buildString("1"));
    entity.getProperties().add(property);

    property = factory.newPrimitiveProperty("ID", factory.newPrimitiveValueBuilder().buildString(Long.toString(System.currentTimeMillis())));
    entity.getProperties().add(property);

    entity.getProperties().add(createAdministrativeInformation());
    entity.getProperties().add(createPhoneNumbers());

    return entity;
  }

  private ClientProperty createAdministrativeInformation() {
    final ClientObjectFactory factory = endpoint.getObjectFactory();
    ClientProperty property;

    final ClientComplexValue complexValueAdministrativeInformation = factory.newComplexValue(Constant.PUNIT_NAME+"."+AdministrativeInformation.class.getSimpleName());

    final ClientComplexValue complexValueChangeInformation = factory.newComplexValue(Constant.PUNIT_NAME+"."+ChangeInformation.class.getSimpleName());
    final ClientProperty propertyChangeInformation = factory.newComplexProperty("Created", complexValueChangeInformation);
    property = factory.newPrimitiveProperty("By", factory.newPrimitiveValueBuilder().buildString("by Me"));
    complexValueChangeInformation.add(property);

    complexValueAdministrativeInformation.add(propertyChangeInformation);
    return factory.newComplexProperty("AdministrativeInformation", complexValueAdministrativeInformation);
  }

  private ClientProperty createPhoneNumbers() {
    final ClientObjectFactory factory = endpoint.getObjectFactory();
    final ClientCollectionValue<ClientValue> phones = factory
        .newCollectionValue(Constant.PUNIT_NAME + "." + Phone.class.getSimpleName());

    ClientProperty property;
    for (int i = 0; i < 3; i++) {
      final ClientComplexValue complexValuePhone = factory
          .newComplexValue(Constant.PUNIT_NAME + "." + Phone.class.getSimpleName());
      property = factory.newPrimitiveProperty("phoneNumber",
          factory.newPrimitiveValueBuilder().buildString(
              "0049/" + (System.currentTimeMillis() / 10000) + "/4567-" + Integer.toString(i)));
      complexValuePhone.add(property);
      phones.add(complexValuePhone);
    }

    return factory.newCollectionProperty("PhoneNumbers", phones);
  }
}
