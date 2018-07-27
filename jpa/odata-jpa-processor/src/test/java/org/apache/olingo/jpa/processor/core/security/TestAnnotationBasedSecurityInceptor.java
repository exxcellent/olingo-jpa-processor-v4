package org.apache.olingo.jpa.processor.core.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataGetHandler;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestAnnotationBasedSecurityInceptor extends TestBase {

	@Before
	public void setup() throws ODataJPAModelException {
		persistenceAdapter.registerDTO(EnvironmentInfo.class);
	}

	@Test
	public void testReadDTO() throws IOException, ODataException, SQLException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"EnvironmentInfos") {
			@Override
			protected void adaptHander(final JPAODataGetHandler handler) {
				handler.setSecurityInceptor(new ServletSecurityAnnotationBasedSecurityInceptor());
			}
		};
		helper.assertStatus(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() == 2);
	}

	@Ignore
	@Test
	public void testWriteDTO() throws IOException, ODataException, SQLException {
		final String id = Integer.toString((int) System.currentTimeMillis());
		final StringBuffer requestBody = new StringBuffer("{");
		requestBody.append("\"Id\": " + id);
		requestBody.append("}");

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "DIDtos(" + id + ")",
				requestBody, HttpMethod.PUT);
		helper.assertStatus(HttpStatusCode.OK.getStatusCode());
	}

}
