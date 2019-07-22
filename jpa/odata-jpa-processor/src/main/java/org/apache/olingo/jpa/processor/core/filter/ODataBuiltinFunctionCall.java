package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;

public interface ODataBuiltinFunctionCall extends JPAExpression<Object> {

  public MethodKind getFunctionKind();

  public JPAExpressionElement<?> getParameter(int index);

  public int noParameters();

  /**
   *
   * @return The type of function call or <code>null</code> for no result or
   *         unknown type.
   */
  public EdmPrimitiveTypeKind getResultType();

}