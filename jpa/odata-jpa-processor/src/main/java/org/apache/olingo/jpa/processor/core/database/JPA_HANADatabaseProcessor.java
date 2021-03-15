package org.apache.olingo.jpa.processor.core.database;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.server.api.ODataApplicationException;

public final class JPA_HANADatabaseProcessor extends AbstractJPADatabaseProcessor {

  @Override
  protected Expression<?> concat(final Expression<String> operand1, final Expression<String> operand2) throws ODataApplicationException {
    final CriteriaBuilder cb = getCriteriaBuilder();
    return cb.function("CONCAT", String.class, operand1, operand2);
  }

  @Override
  protected Expression<Boolean> contains(final Expression<String> operand, final String literal)
      throws ODataApplicationException {
    // use workaround to produce case-insensitive search
    final CriteriaBuilder cb = getCriteriaBuilder();
    final Expression<String> lowerOperand = cb.lower(operand);
    return super.contains(lowerOperand, literal.toLowerCase());
  }
}
