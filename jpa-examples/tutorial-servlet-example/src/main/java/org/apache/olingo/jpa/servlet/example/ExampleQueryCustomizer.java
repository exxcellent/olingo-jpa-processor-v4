package org.apache.olingo.jpa.servlet.example;

import org.apache.olingo.jpa.processor.core.api.QueryCustomizer;
import org.apache.olingo.jpa.processor.core.query.NavigationIfc;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.Person;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class ExampleQueryCustomizer implements QueryCustomizer {
  @Override
  public Expression<Boolean> restrictQuery(final QueryContext context, final NavigationIfc queryScope) {
    if (queryScope.getUriResourceParts().size() != 1 || !queryScope.getUriResourceParts().get(0)
        .getSegmentValue().equals("Organizations")) {
      return null;
    }
    final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
    final Subquery<Integer> sq = context.createSubquery(Integer.class);
    final Root<Organization> from = sq.from(Organization.class);
    final Join<Organization, Person> join = from.join("creator");
    sq.select(cb.literal(Integer.valueOf(1)));
    sq.where(cb.equal(from.get("name1"), context.getFrom().get("name1")), cb.equal(join.get("firstName"),
        "Urs"));
    return cb.exists(sq);
  }
}
