package org.apache.olingo.jpa.servlet.example;

import javax.persistence.Entity;

import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.domain.ClientCollectionValue;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.ChangeInformation;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.testmodel.Phone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Ralf Zozmann
 *
 * @see https://templth.wordpress.com/2014/12/03/accessing-odata-v4-service-with-olingo/
 *
 */
public class CreateUpdateEntitiesIT {

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
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("DatatypeConversionEntities");
		final ClientObjectFactory factory = endpoint.getObjectFactory();
		final FullQualifiedName fqn = new FullQualifiedName(Constant.PUNIT_NAME,
				DatatypeConversionEntity.class.getAnnotation(Entity.class).name());
		final ClientEntity entity = factory.newEntity(fqn);
		ClientProperty property;

		property = factory.newPrimitiveProperty("ID",
				factory.newPrimitiveValueBuilder().buildInt32(Integer.valueOf((int) System.currentTimeMillis())));
		entity.getProperties().add(property);

		// property = factory.newPrimitiveProperty("ADate1",
		// factory.newPrimitiveValueBuilder().buildString("0610-01-01"));
		// entity.getProperties().add(property);

		property = factory.newPrimitiveProperty("ADate3", factory.newPrimitiveValueBuilder().buildString("0610-01-01"));
		entity.getProperties().add(property);

		final ODataEntityCreateResponse<ClientEntity> responseCreate = endpoint.createEntity(uriBuilder, entity);
		Assert.assertTrue(responseCreate.getStatusCode() == HttpStatusCode.CREATED.getStatusCode());
		final ClientEntity bodyCreate = responseCreate.getBody();
		responseCreate.close();
		Assert.assertNotNull(bodyCreate);
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
			property = factory.newPrimitiveProperty("PhoneNumber",
					factory.newPrimitiveValueBuilder().buildString(
							"0049/" + (System.currentTimeMillis() / 10000) + "/4567-" + Integer.toString(i)));
			complexValuePhone.add(property);
			phones.add(complexValuePhone);
		}

		return factory.newCollectionProperty("PhoneNumbers", phones);
	}
}
