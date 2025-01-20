package org.apache.olingo.jpa.servlet.example;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.processor.core.api.QueryRequestCustomizer;
import org.apache.olingo.jpa.processor.core.query.NavigationIfc;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.testmodel.Person;

public class ExampleQueryCustomizer implements QueryRequestCustomizer {
  @Override
  public void customizeQuery(final QueryCustomization context, final NavigationIfc queryScope) {
    if (!context.getEndTypeClass().equals(Organization.class)) {
      return;
    }
    final CriteriaBuilder cb = context.getEntityManager().getCriteriaBuilder();
    final Subquery<Integer> sq = context.createSubquery(Integer.class);
    final Root<Organization> from = sq.from(Organization.class);
    final Join<Organization, Person> join = from.join("creator");
    sq.select(cb.literal(Integer.valueOf(1)));
    sq.where(cb.equal(from.get("name1"), context.getEndFrom().get("name1")), cb.equal(join.get("firstName"),
        "Urs"));
    context.withWhereClause(cb.exists(sq));
  }
}
