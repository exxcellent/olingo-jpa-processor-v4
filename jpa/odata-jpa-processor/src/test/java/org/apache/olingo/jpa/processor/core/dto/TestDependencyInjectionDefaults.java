package org.apache.olingo.jpa.processor.core.dto;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.security.AnnotationBasedSecurityInceptor;
import org.apache.olingo.jpa.processor.core.util.PrincipalMock;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.UriInfo;
import org.junit.Test;

public class TestDependencyInjectionDefaults extends TestBase {

  private class DPITestGenericJPAPersistenceAdapter extends TestGenericJPAPersistenceAdapter {

    @javax.inject.Inject
    JPAAdapter adpterJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    JPAAdapter adpterOlingo;

    @javax.inject.Inject
    JPAEdmProvider edmProviderJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    JPAEdmProvider edmProviderOlingo;

    @javax.inject.Inject
    JPAODataGlobalContext contextGlobalJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    JPAODataGlobalContext contextGlobalOlingo;

    @javax.inject.Inject
    Principal prinicpalJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    Principal prinicpalOlingo;

    @javax.inject.Inject
    JPAODataRequestContext contextRequestJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    JPAODataRequestContext contextRequestOlingo;

    @javax.inject.Inject
    HttpServletRequest requestJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    HttpServletRequest requestServletOlingo;

    @javax.inject.Inject
    HttpServletResponse responseServletJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    HttpServletResponse responseServletOlingo;

    @javax.inject.Inject
    EntityManager emJavaX;

    @org.apache.olingo.jpa.cdi.Inject
    EntityManager emOlingo;

    public DPITestGenericJPAPersistenceAdapter(final String pUnit, final DataSourceHelper.DatabaseType dbType) {
      super(pUnit, dbType);
    }

    @Override
    public EntityManager createEntityManager() throws RuntimeException {
      // global
      checkPresense(JPAAdapter.class, adpterJavaX, "adpterJavaX");
      checkPresense(JPAAdapter.class, adpterOlingo, "adpterOlingo");
      checkPresense(JPAEdmProvider.class, edmProviderJavaX, "edmProviderJavaX");
      checkPresense(JPAEdmProvider.class, edmProviderOlingo, "edmProviderOlingo");
      checkPresense(JPAODataGlobalContext.class, contextGlobalJavaX, "contextGlobalJavaX");
      checkPresense(JPAODataGlobalContext.class, contextGlobalOlingo, "contextGlobalOlingo");
      // request
      checkAbsence(JPAODataRequestContext.class, contextRequestJavaX, "contextRequestJavaX");
      checkAbsence(JPAODataRequestContext.class, contextRequestOlingo, "contextRequestOlingo");
      checkAbsence(Principal.class, prinicpalJavaX, "prinicpalJavaX");
      checkAbsence(Principal.class, prinicpalOlingo, "prinicpalOlingo");
      checkAbsence(HttpServletRequest.class, requestJavaX, "requestJavaX");
      checkAbsence(HttpServletRequest.class, requestServletOlingo, "requestServletOlingo");
      checkAbsence(HttpServletResponse.class, responseServletJavaX, "responseServletJavaX");
      checkAbsence(HttpServletResponse.class, responseServletOlingo, "responseServletOlingo");
      checkAbsence(EntityManager.class, emJavaX, "emJavaX");
      checkAbsence(EntityManager.class, emOlingo, "emOlingo");

      return super.createEntityManager();
    }

    @Override
    public void beginTransaction(final EntityManager em) throws RuntimeException {
      // global
      checkPresense(JPAAdapter.class, adpterJavaX, "adpterJavaX");
      checkPresense(JPAAdapter.class, adpterOlingo, "adpterOlingo");
      checkPresense(JPAEdmProvider.class, edmProviderJavaX, "edmProviderJavaX");
      checkPresense(JPAEdmProvider.class, edmProviderOlingo, "edmProviderOlingo");
      checkPresense(JPAODataGlobalContext.class, contextGlobalJavaX, "contextGlobalJavaX");
      checkPresense(JPAODataGlobalContext.class, contextGlobalOlingo, "contextGlobalOlingo");
      // request
      checkPresense(JPAODataRequestContext.class, contextRequestJavaX, "contextRequestJavaX");
      checkPresense(JPAODataRequestContext.class, contextRequestOlingo, "contextRequestOlingo");
      checkPresense(Principal.class, prinicpalJavaX, "prinicpalJavaX");
      checkPresense(Principal.class, prinicpalOlingo, "prinicpalOlingo");
      checkPresense(HttpServletRequest.class, requestJavaX, "requestJavaX");
      checkPresense(HttpServletRequest.class, requestServletOlingo, "requestServletOlingo");
      checkPresense(HttpServletResponse.class, responseServletJavaX, "responseServletJavaX");
      checkPresense(HttpServletResponse.class, responseServletOlingo, "responseServletOlingo");
      checkPresense(EntityManager.class, emJavaX, "emJavaX");
      checkPresense(EntityManager.class, emOlingo, "emOlingo");

      // additional check
      assert adpterJavaX == this;
      assert adpterOlingo == this;
      assert emJavaX == em;
      assert emOlingo == em;

      super.beginTransaction(em);
    }

  }

  private class DPITestAnnotationBasedSecurityInceptor extends AnnotationBasedSecurityInceptor {

    @org.apache.olingo.jpa.cdi.Inject
    JPAAdapter adpterOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    JPAEdmProvider edmProviderOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    JPAODataGlobalContext contextGlobalOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    Principal prinicpalOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    JPAODataRequestContext contextRequestOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    HttpServletRequest requestServletOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    HttpServletResponse responseServletOlingo;

    @org.apache.olingo.jpa.cdi.Inject
    EntityManager emOlingo;

    @Override
    public void authorize(final ODataRequest odRequest, final UriInfo uriInfo) throws ODataApplicationException {
      // global
      checkPresense(JPAAdapter.class, adpterOlingo, "adpterOlingo");
      checkPresense(JPAEdmProvider.class, edmProviderOlingo, "edmProviderOlingo");
      checkPresense(JPAODataGlobalContext.class, contextGlobalOlingo, "contextGlobalOlingo");
      // request
      checkPresense(JPAODataRequestContext.class, contextRequestOlingo, "contextRequestOlingo");
      checkPresense(HttpServletRequest.class, requestServletOlingo, "requestServletOlingo");
      checkPresense(HttpServletResponse.class, responseServletOlingo, "responseServletOlingo");
      checkPresense(EntityManager.class, emOlingo, "emOlingo");
      // just prepared here
      checkAbsence(Principal.class, prinicpalOlingo, "prinicpalOlingo");
      super.authorize(odRequest, uriInfo);
    }
  }

  void checkPresense(final Class<?> expectedType, final Object value, final String name) {
    if (value == null) {
      throw new IllegalStateException("'" + name + "' of type " + expectedType.getSimpleName() + " not injected");
    } else if (!expectedType.isInstance(value)) {
      throw new IllegalArgumentException("'" + name + "' of type " + expectedType.getSimpleName() + " has wrong type: "
          + value.getClass().getSimpleName());
    }
  }

  void checkAbsence(final Class<?> expectedType, final Object value, final String name) {
    if (value != null) {
      throw new IllegalStateException("'" + name + "' of type " + expectedType.getSimpleName() + " is present");
    }
  }


  @Override
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    return new DPITestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
        DataSourceHelper.DatabaseType.H2);
  }

  @Test
  public void testDefaultInjectionPresence() throws IOException, ODataException, SQLException {

    final URIBuilder uriBuilder = newUriBuilder().appendMetadataSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder);
    helper.setSecurityInceptor(new DPITestAnnotationBasedSecurityInceptor());
    helper.setUser(new PrincipalMock("user123"));
    // 1. call
    helper.execute(HttpStatusCode.OK.getStatusCode());
    // 2. call (previous values must be removed)
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }


}
