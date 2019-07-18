package org.apache.olingo.jpa.processor.core.database;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;

public final class JPA_HANADatabaseProcessor extends JPA_DefaultDatabaseProcessor {
  @Override
  protected Expression<?> contains(final Expression<String> operand, final String literal)
      throws ODataApplicationException {
    // replace 'LIKE' with a better variant
    final CriteriaBuilder cb = getCriteriaBuilder();
    final StringBuffer contains = new StringBuffer();
    contains.append('%');
    contains.append(literal);
    contains.append('%');
    return cb.function("CONTAINS", String.class, operand, cb.literal(contains.toString()));
  }

}
