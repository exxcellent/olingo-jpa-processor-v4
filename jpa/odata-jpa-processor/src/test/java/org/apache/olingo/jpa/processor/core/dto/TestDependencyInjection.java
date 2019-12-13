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

import org.apache.olingo.client.api.uri.URIBuilder;
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

  @ODataDTO(handler = DtoHandler.class)
  public static class Dto {
    @Id
    private long id;
  }

  public static class DtoHandler implements ODataDTOHandler<Dto> {

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
    public Collection<Dto> read(final UriInfoResource requestedResource) throws RuntimeException {
      checkInjection();
      final Collection<Dto> result = new LinkedList<>();

      final Dto dto1 = new Dto();
      dto1.id = 1;
      result.add(dto1);

      final Dto dto2 = new Dto();
      dto2.id = 2;
      result.add(dto2);

      return result;
    };

    @Override
    public void write(final UriInfoResource requestedResource, final Dto dto) throws RuntimeException {
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
    persistenceAdapter.registerDTO(Dto.class);
  }

  @Test
  public void testReadDTO() throws IOException, ODataException, SQLException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Dtos");
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getValues().size() == 2);
  }

  @Test
  public void testWriteDTO() throws IOException, ODataException, SQLException {
    final int iId = (int) System.currentTimeMillis();
    final String sId = Integer.toString(iId);
    final StringBuffer requestBody = new StringBuffer("{");
    requestBody.append("\"Id\": " + sId);
    requestBody.append("}");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Dtos").appendKeySegment(Integer.valueOf(iId));
    final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, uriBuilder, requestBody.toString(),
        HttpMethod.PUT);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidType() {
    final DependencyInjector injector = new DependencyInjector();
    injector.registerDependencyMapping(Integer.class, Integer.valueOf(2));
  }
}
