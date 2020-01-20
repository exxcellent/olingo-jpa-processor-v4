package org.apache.olingo.jpa.metadata.core.edm.entity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.From;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * The handler must have a default constructor.
 *
 */
public interface DataAccessConditioner<X> {

  /**
   *
   * @param from
   *            The JPA 'from' table.
   * @return Additional WHERE clause expression or <code>null</code>.
   */
  public javax.persistence.criteria.Expression<Boolean> buildSelectCondition(EntityManager em, From<X, X> from)
      throws ODataApplicationException;
}
