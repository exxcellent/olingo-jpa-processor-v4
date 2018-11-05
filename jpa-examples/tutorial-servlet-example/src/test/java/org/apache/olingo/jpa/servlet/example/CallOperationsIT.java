package org.apache.olingo.jpa.servlet.example;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.olingo.client.api.communication.response.ODataInvokeResponse;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientInvokeResult;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.domain.ClientCollectionValueImpl;
import org.apache.olingo.client.core.domain.ClientComplexValueImpl;
import org.apache.olingo.client.core.domain.ClientPrimitiveValueImpl;
import org.apache.olingo.client.core.domain.ClientPropertyImpl;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.otherpackage.TestEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 *
 * @author Ralf Zozmann
 *
 * @see https://templth.wordpress.com/2014/12/03/accessing-odata-v4-service-with-olingo/
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CallOperationsIT {

	private ODataEndpointTestDefinition endpoint;

	@Before
	public void setup() {
		endpoint = new ODataEndpointTestDefinition();
	}

	@Test
	public void test02ActionBound1() throws Exception {
		// URIBuilder uriBuilder = endpoint.newUri().appendOperationCallSegment("ClearPersonsCustomStrings");
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("99")
				.appendOperationCallSegment(Constant.PUNIT_NAME + ".ClearPersonsCustomStrings");
		final Map<String, ClientValue> functionParameters = new HashMap<>();
		final ODataInvokeResponse<ClientInvokeResult> response = endpoint.callAction(uriBuilder, ClientInvokeResult.class,
				functionParameters);
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.NO_CONTENT.getStatusCode());
		response.close();
	}

	@Test
	public void test03ActionBound2() throws Exception {
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("99")
				.appendOperationCallSegment(Constant.PUNIT_NAME + ".DoNothingAction1");
		final Map<String, ClientValue> functionParameters = new HashMap<>();
		functionParameters.put("affectedPersons",
				new ClientCollectionValueImpl<ClientPrimitiveValue>(EdmPrimitiveTypeKind.String.getFullQualifiedName().getName())
				.add(new ClientPrimitiveValueImpl.BuilderImpl().buildString("abc")));
		functionParameters.put("minAny", new ClientPrimitiveValueImpl.BuilderImpl().buildInt32(Integer.valueOf(123)));
		functionParameters.put("maxAny", new ClientPrimitiveValueImpl.BuilderImpl().buildInt32(Integer.valueOf(4000)));
		final ODataInvokeResponse<ClientEntity> response = endpoint.callAction(uriBuilder, ClientEntity.class, functionParameters);
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		final ClientEntity body = response.getBody();
		// the package name differs from oData namespace, so we can only compare the simple name
		Assert.assertTrue(Organization.class.getSimpleName().equals(body.getTypeName().getName()));
		Assert.assertNotNull(body.getProperty("Country"));
		response.close();
	}

	@Test
	public void test01ActionBound3() throws Exception {
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendKeySegment("99")
				.appendOperationCallSegment(Constant.PUNIT_NAME + ".SendBackTheInput");
		final Map<String, ClientValue> functionParameters = new HashMap<>();
		final String teststring = "teststring";
		functionParameters.put("input", new ClientPrimitiveValueImpl.BuilderImpl().buildString(teststring));
		final ODataInvokeResponse<ClientProperty> response = endpoint.callAction(uriBuilder, ClientProperty.class, functionParameters);
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		final String retValue = response.getBody().getPrimitiveValue().toCastValue(String.class);
		Assert.assertTrue(teststring.equals(retValue));
		response.close();
	}

	@Ignore("No functions are available in test environment")
	@Test
	public void test05FunctionUnbound() throws Exception {
		final URIBuilder uriBuilder = endpoint.newUri().appendEntitySetSegment("Persons").appendOperationCallSegment("IS_PRIME");
		final Map<String, ClientValue> functionParameters = new HashMap<>();
		functionParameters.put("Number", new ClientPrimitiveValueImpl.BuilderImpl().buildDecimal(BigDecimal.valueOf(123.456)));
		final ODataInvokeResponse<ClientInvokeResult> response = endpoint.callFunction(uriBuilder, ClientInvokeResult.class,
				functionParameters);
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		// String sCount = response.getBody().toCastValue(String.class);
		// Assert.assertTrue(Integer.valueOf(sCount).intValue() > 0);
		response.close();
	}

	@Ignore
	@Test
	public void test04DataConversionUnboundActionUnbound() throws Exception {
		final URIBuilder uriBuilder = endpoint.newUri().appendOperationCallSegment("unboundActionCheckAllValueSettings");
		final Map<String, ClientValue> actionParameters = new HashMap<String, ClientValue>();
		final ClientComplexValue modifiedFristObject = new ClientComplexValueImpl(Constant.PUNIT_NAME + ".DatatypeConversionEntity");
		actionParameters.put("jpaEnity", modifiedFristObject);

		String dateString = Integer.toString(LocalDate.now().getYear() - 1) + "-01-01";
		modifiedFristObject.add(new ClientPropertyImpl("ADate1", new ClientPrimitiveValueImpl.BuilderImpl().buildString(dateString)));
		dateString = Integer.toString(LocalDate.now().getYear()) + "-01-01";
		modifiedFristObject.add(new ClientPropertyImpl("ADate11", new ClientPrimitiveValueImpl.BuilderImpl().buildString(dateString)));
		dateString = Integer.toString(LocalDate.now().getYear() + 1) + "-01-01";
		modifiedFristObject.add(new ClientPropertyImpl("ADate2", new ClientPrimitiveValueImpl.BuilderImpl().buildString(dateString)));
		dateString = Integer.toString(LocalDate.now().getYear()) + "-01-31";
		modifiedFristObject.add(new ClientPropertyImpl("ADate3", new ClientPrimitiveValueImpl.BuilderImpl().buildString(dateString)));
		modifiedFristObject
		.add(new ClientPropertyImpl("AUrl", new ClientPrimitiveValueImpl.BuilderImpl().buildString("http://www.anywhere.org")));
		modifiedFristObject.add(
				new ClientPropertyImpl("ADecimal", new ClientPrimitiveValueImpl.BuilderImpl().buildDecimal(BigDecimal.valueOf(123.456))));
		modifiedFristObject
		.add(new ClientPropertyImpl("AStringMappedEnum", new ClientPrimitiveValueImpl.BuilderImpl().buildString(IsoEra.CE.name())));
		modifiedFristObject.add(new ClientPropertyImpl("AOrdinalMappedEnum",
				new ClientPrimitiveValueImpl.BuilderImpl().buildString(ChronoUnit.HOURS.name())));
		modifiedFristObject.add(new ClientPropertyImpl("AEnumFromOtherPackage",
				new ClientPrimitiveValueImpl.BuilderImpl().buildString(TestEnum.Two.name())));
		modifiedFristObject.add(new ClientPropertyImpl("Uuid", new ClientPrimitiveValueImpl.BuilderImpl().buildGuid(UUID.randomUUID())));
		modifiedFristObject.add(new ClientPropertyImpl("ABoolean", new ClientPrimitiveValueImpl.BuilderImpl().buildBoolean(Boolean.TRUE)));

		final ODataInvokeResponse<ClientProperty> response = endpoint.callAction(uriBuilder, ClientProperty.class, actionParameters);
		Assert.assertTrue(response.getStatusCode() == HttpStatusCode.OK.getStatusCode());
		final Boolean success = response.getBody().getPrimitiveValue().toCastValue(Boolean.class);
		response.close();
		Assert.assertTrue(success.booleanValue());
	}

}
