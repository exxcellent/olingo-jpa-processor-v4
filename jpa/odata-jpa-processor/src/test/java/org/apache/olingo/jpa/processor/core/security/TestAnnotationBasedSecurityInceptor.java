package org.apache.olingo.jpa.processor.core.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.PrincipalMock;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestAnnotationBasedSecurityInceptor extends TestBase {

	@Before
	public void setup() throws ODataJPAModelException {
		persistenceAdapter.registerDTO(EnvironmentInfo.class);
	}

	@Ignore
	@Test
	public void testReadDTO() throws IOException, ODataException, SQLException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"EnvironmentInfos");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() == 2);
	}

	@Test
	public void testDTOUnsecureActionCall() throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"actionWithNoSecurity", null,
				HttpMethod.POST);
		helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValue().get("value").asInt() == 42);

	}

	@Test
	public void testDTOAuthenticatedActionCall() throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"actionWithOnlyAuthentication", null, HttpMethod.POST);
		helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
		final String userName = "abcUser";
		helper.setUser(new PrincipalMock(userName));
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(userName.equals(helper.getValue().get("value").asText()));

	}

	@Test
	public void testDTOAuthenticatedActionCallRejectedWithoutUser()
			throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"actionWithOnlyAuthentication", null, HttpMethod.POST);
		helper.execute(HttpStatusCode.UNAUTHORIZED.getStatusCode());
	}

	@Test
	public void testDTOAuthenticatedActionCallRejectedWrongRole()
			throws IOException, ODataException, NoSuchMethodException {

		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"actionWithOnlyRole", null, HttpMethod.POST);
		final String userName = "abcUser";
		helper.setUser(new PrincipalMock(userName, new String[] { "role.dummy" }));
		helper.execute(HttpStatusCode.FORBIDDEN.getStatusCode());
	}

	@Test
	public void testDTOAuthenticatedActionCallAcceptRightRole()
			throws IOException, ODataException, NoSuchMethodException {
		persistenceAdapter.registerDTO(EnvironmentInfo.class);
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "actionWithOnlyRole", null,
				HttpMethod.POST);
		final String userName = "superUser";
		helper.setUser(new PrincipalMock(userName, new String[] { "access" }));
		helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Ignore
	@Test
	public void testWriteDTO() throws IOException, ODataException, SQLException {
		final String id = Integer.toString((int) System.currentTimeMillis());
		final StringBuffer requestBody = new StringBuffer("{");
		requestBody.append("\"Id\": " + id);
		requestBody.append("}");

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DIDtos(" + id + ")",
				requestBody, HttpMethod.PUT);
		helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
		helper.execute(HttpStatusCode.OK.getStatusCode());
	}

}
