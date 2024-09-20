package org.apache.olingo.jpa.processor.core.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.QueryCustomizer;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class TestQueryCustomizer extends TestBase {

  @Test
  public void testAbstractCriteriaQueryBuilderDI() throws IOException, ODataException {

    final boolean[] wasCalled = new boolean[] { false };
    final QueryCustomizer testCustomizer = new QueryCustomizer() {

      @Override
      public Expression<Boolean> restrictQuery(final QueryContext context, final NavigationIfc queryScope) {
        if (queryScope.getUriResourceParts().size() != 1 || !queryScope.getUriResourceParts().get(0)
            .getSegmentValue().equals("Persons")) {
          return null;
        }
        final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
        final Subquery<Integer> sq = context.createSubquery(Integer.class);
        final Root<Person> from = sq.from(Person.class);
        sq.select(cb.literal(Integer.valueOf(1)));
        sq.where(cb.equal(from.get("firstName"), context.getFrom().get("firstName")), cb.equal(from.get("firstName"),
            "Max"));
        wasCalled[0] = true;
        return cb.exists(sq);
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

}
