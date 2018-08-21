package org.apache.olingo.jpa.processor.core.dto;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTOHandler;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.junit.Before;
import org.junit.Test;

public class TestDependencyInjection extends TestBase {

	@ODataDTO(handler = DIDtoHandler.class)
	public static class DIDto {
		@Id
		private long id;
	}

	public static class DIDtoHandler implements ODataDTOHandler<DIDto> {

		@Inject
		private HttpServletRequest request;

		@Inject
		private HttpServletResponse response;

		@Inject
		private JPAAdapter persistenceAdapter;

		@Inject
		private JPAEdmProvider edmProvider;

		@Inject
		private EntityManager em;

		@Override
		public Collection<DIDto> read(final UriInfoResource requestedResource) throws RuntimeException {
			checkInjection();
			final Collection<DIDto> result = new LinkedList<>();

			final DIDto dto1 = new DIDto();
			dto1.id = 1;
			result.add(dto1);

			final DIDto dto2 = new DIDto();
			dto2.id = 2;
			result.add(dto2);

			return result;
		};

		@Override
		public void write(final UriInfoResource requestedResource, final DIDto dto) throws RuntimeException {
			checkInjection();
		}

		private void checkInjection() throws RuntimeException {
			if (request == null) {
				throw new IllegalStateException("HttpServletRequest not injected");
			}
			if (response == null) {
				throw new IllegalStateException("HttpServletResponse not injected");
			}
			if (persistenceAdapter == null) {
				throw new IllegalStateException("JPAAdapter not injected");
			}
			if (edmProvider == null) {
				throw new IllegalStateException("JPAEdmProvider not injected");
			}
			if (em == null) {
				throw new IllegalStateException("EntityManager not injected");
			}

		}
	}

	@Before
	public void setup() throws ODataJPAModelException {
		persistenceAdapter.registerDTO(DIDto.class);
	}

	@Test
	public void testReadDTO() throws IOException, ODataException, SQLException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DIDtos");
		helper.assertStatus(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() == 2);
	}

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

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidType() {
		final DependencyInjector injector = new DependencyInjector();
		injector.registerDependencyMapping(Integer.class, Integer.valueOf(2));
	}
}
