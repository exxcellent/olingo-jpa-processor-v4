package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;

/**
 *
 * @author Ralf Zozmann
 *
 * @param <T> The result type of expression building for value.
 */
public interface JPAExpressionElement<T> {

  /**
   *
   * @return Normally a {@link javax.persistence.criteria.Expression}
   */
  public T get() throws ODataApplicationException;

  /**
   * The current {@link JPAExpressionElement} normally wraps the elements from query expressions to hold additional
   * informations. But for filtering in case of navigation paths (to members) we have to (re)built new expressions with
   * a subset of expression elements to eliminate the navigation path in front.
   *
   * @return The unwrapped query expression element used as input for {@link JPAVisitor}.
   */
  public Expression getQueryExpressionElement();
}