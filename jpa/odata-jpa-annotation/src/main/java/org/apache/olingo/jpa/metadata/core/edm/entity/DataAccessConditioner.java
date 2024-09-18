package org.apache.olingo.jpa.metadata.core.edm.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.From;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * The handler must have a default constructor.
 *
 */
public interface DataAccessConditioner<X> {

  /**
   * Optional additional AND WHERE clause to limit the results.
   *
   * @param from
   * The JPA 'from' table of entity where the conditioner is attached.
   * @return Additional WHERE clause expression or <code>null</code>.
   */
  public jakarta.persistence.criteria.Expression<Boolean> buildSelectCondition(EntityManager em, From<X, X> from)
      throws ODataApplicationException;
}
