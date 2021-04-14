package org.apache.olingo.jpa.processor.core.security;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.Id;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTOHandler;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.dto.EnvironmentInfo;
import org.apache.olingo.jpa.processor.core.testmodel.dto.sub.SystemRequirement;
import org.apache.olingo.jpa.processor.core.util.PrincipalMock;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.security.AccessDefinition;
import org.apache.olingo.jpa.security.ODataEntityAccess;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.junit.Before;
import org.junit.Test;

public class TestAnnotationBasedSecurityInceptor extends TestBase {

  @ODataEntityAccess({ @AccessDefinition(method = HttpMethod.GET, authenticationRequired = false),
    @AccessDefinition(method = HttpMethod.PUT),
    @AccessDefinition(method = HttpMethod.PATCH, rolesAllowed = { "role.patch" }) })
  @ODataDTO(handler = DefaultResourceSecurityDtoHandler.class)
  public static class DefaultResourceSecurityDto {

    @Id
    private final long id = 1;

  }

  public static class DefaultResourceSecurityDtoHandler implements ODataDTOHandler<DefaultResourceSecurityDto> {

    @Override
    public Collection<DefaultResourceSecurityDto> read(final UriInfoResource requestedResource)
        throws RuntimeException {
      return Collections.singletonList(new DefaultResourceSecurityDto());
    };

    @Override
    public void write(final UriInfoResource requestedResource, final DefaultResourceSecurityDto dto)
        throws RuntimeException {
      // do nothing
    }

  }

  @ODataEntityAccess({ @AccessDefinition(method = HttpMethod.POST, authenticationRequired = true) })
  @ODataDTO(handler = ActionInResourceSecuredDtoHandler.class)
  public static class ActionInResourceSecuredDto {

    @Id
    private final long id = 1;

    @EdmAction
    public void actionTakingSecurityFromResource() {
      // do nothing
    }
  }

  public static class ActionInResourceSecuredDtoHandler implements ODataDTOHandler<ActionInResourceSecuredDto> {

    @Override
    public Collection<ActionInResourceSecuredDto> read(final UriInfoResource requestedResource)
        throws RuntimeException {
      return Collections.singletonList(new ActionInResourceSecuredDto());
    };

    @Override
    public void write(final UriInfoResource requestedResource, final ActionInResourceSecuredDto dto)
        throws RuntimeException {
      // do nothing
    }

  }

  @Before
  public void setup() throws ODataJPAModelException {
    persistenceAdapter.registerDTO(DefaultResourceSecurityDto.class);
    persistenceAdapter.registerDTO(ActionInResourceSecuredDto.class);
  }

  @Test
  public void testDeriveActionSecurityFromResource() throws IOException, ODataException, SQLException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("ActionInResourceSecuredDtos")
        .appendKeySegment(Integer.valueOf(1)).appendActionCallSegment(ActionInResourceSecuredDto.class.getPackage()
            .getName() + ".actionTakingSecurityFromResource");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.execute(HttpStatusCode.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void testReadDTO1() throws IOException, ODataException, SQLException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("ActionInResourceSecuredDtos");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    // the GET must result in a FORBIDDEN without security configuration
    helper.execute(HttpStatusCode.FORBIDDEN.getStatusCode());
  }

  @Test
  public void testReadDTO2() throws IOException, ODataException, SQLException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DefaultResourceSecurityDtos");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValues().size() == 1);
  }

  @Test
  public void testPutWithoutRoleDTO() throws IOException, ODataException, SQLException {
    final StringBuffer requestBody = new StringBuffer("{\"Id\": 2}");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DefaultResourceSecurityDtos")
        .appendKeySegment(Integer.valueOf(1));
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, requestBody.toString(),
        HttpMethod.PUT);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.setUser(new PrincipalMock("user123"));
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValue().get("Id").asInt() == 2);
  }

  @Test
  public void testAcceptPatchDTO() throws IOException, ODataException, SQLException {
    final StringBuffer requestBody = new StringBuffer("{\"Id\": 2}");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DefaultResourceSecurityDtos")
        .appendKeySegment(Integer.valueOf(1));
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, requestBody.toString(),
        HttpMethod.PATCH);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.setUser(new PrincipalMock("user123", new String[] { "role.patch" }));
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValue().get("Id").asInt() == 2);
  }

  @Test
  public void testRejectPatchDTO() throws IOException, ODataException, SQLException {
    final StringBuffer requestBody = new StringBuffer("{\"Id\": 2}");
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DefaultResourceSecurityDtos")
        .appendKeySegment(Integer.valueOf(1));
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, requestBody.toString(),
        HttpMethod.PATCH);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.setUser(new PrincipalMock("user123", new String[] { "role.wrong" }));
    helper.execute(HttpStatusCode.FORBIDDEN.getStatusCode());
  }

  @Test
  public void testDTOUnsecureActionCall() throws IOException, ODataException, NoSuchMethodException {
    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithNoSecurity");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(helper.getJsonObjectValue().get("value").asInt() == 42);
  }

  @Test
  public void testDTOAuthenticatedActionCall() throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithOnlyAuthentication");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.setSecurityInceptor(new AnnotationBasedSecurityInceptor());
    final String userName = "abcUser";
    helper.setUser(new PrincipalMock(userName));
    helper.execute(HttpStatusCode.OK.getStatusCode());
    assertTrue(userName.equals(helper.getJsonObjectValue().get("value").asText()));

  }

  @Test
  public void testDTOAuthenticatedActionCallRejectedWithoutUser()
      throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithOnlyAuthentication");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    helper.execute(HttpStatusCode.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void testDTOAuthenticatedActionCallRejectedWrongRole()
      throws IOException, ODataException, NoSuchMethodException {

    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithOnlyRole");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    final String userName = "abcUser";
    helper.setUser(new PrincipalMock(userName, new String[] { "role.dummy" }));
    helper.execute(HttpStatusCode.FORBIDDEN.getStatusCode());
  }

  @Test
  public void testDTOAuthenticatedActionCallAcceptRightRole()
      throws IOException, ODataException, NoSuchMethodException {
    persistenceAdapter.registerDTO(EnvironmentInfo.class);
    persistenceAdapter.registerDTO(SystemRequirement.class);
    final URIBuilder uriBuilder = newUriBuilder().appendActionCallSegment("actionWithOnlyRole");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder, null, HttpMethod.POST);
    final String userName = "superUser";
    helper.setUser(new PrincipalMock(userName, new String[] { "access" }));
    helper.execute(HttpStatusCode.NO_CONTENT.getStatusCode());
  }

}
