package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 *
 * @author Ralf Zozmann
 *
 * @param <T> The result type of expression building for value.
 */
public interface JPAExpressionElement<T> {
  public T get() throws ODataApplicationException;
}