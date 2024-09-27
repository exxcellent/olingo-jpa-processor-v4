package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.QueryCustomizer;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.server.api.uri.UriResourceCount;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestQueryCustomizer extends TestBase {

  @Test
  public void testEntityAndElementCollectionAndExpandBuilder() throws IOException, ODataException {

    final boolean[] wasCalled = new boolean[] { false };
    final QueryCustomizer testCustomizer = new QueryCustomizer() {

      @Override
      public void customizeQuery(final QueryCustomization context, final NavigationIfc queryScope) {
        if (!context.getStartTypeClass().equals(Person.class)) {
          return;
        }
        final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
        final Subquery<Integer> sq = context.createSubquery(Integer.class);
        final Root<Person> from = sq.from(Person.class);
        sq.select(cb.literal(Integer.valueOf(1)));
        sq.where(cb.equal(from.get("firstName"), context.getStartFrom().get("firstName")), cb.equal(from.get(
            "firstName"),
            "Max"));
        wasCalled[0] = true;
        context.withWhereClause(cb.exists(sq));
      }
    };

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").expand("*");
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {
      @Override
      protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.modifyRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(QueryCustomizer.class, testCustomizer);
      }
    };
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final ArrayNode result = helper.getJsonObjectValues();
    assertTrue(wasCalled[0]);
    assertEquals(1, result.size());

  }

  @Test
  public void testCountBuilder() throws IOException, ODataException {

    final boolean[] wasCalled = new boolean[] { false };
    final QueryCustomizer testCustomizer = new QueryCustomizer() {

      @Override
      public void customizeQuery(final QueryCustomization context, final NavigationIfc queryScope) {
        if (queryScope.getUriResourceParts().size() != 2 || !(queryScope.getUriResourceParts().get(
            1) instanceof UriResourceCount)) {
          return;
        }
        final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
        final Subquery<Integer> sq = context.createSubquery(Integer.class);
        final Root<Person> from = sq.from(Person.class);
        sq.select(cb.literal(Integer.valueOf(1)));
        final Predicate or = cb.or(cb.equal(from.get("firstName"), "Max"), cb.equal(from.get("firstName"), "John"));
        sq.where(cb.equal(from.get("firstName"), context.getEndFrom().get("firstName")), or);
        wasCalled[0] = true;
        context.withWhereClause(cb.exists(sq));
      }
    };
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Persons").appendCountSegment();
    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {
      @Override
      protected void modifyRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.modifyRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(QueryCustomizer.class, testCustomizer);
      }
    };
    helper.setRequestedResponseContentType(ContentType.JSON_FULL_METADATA.toContentTypeString());
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final long result = Long.valueOf(helper.getRawResult()).longValue();
    assertTrue(wasCalled[0]);
    assertEquals(2, result);

  }

}
