package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.core.uri.queryoption.expression.TypeLiteralImpl;

public class JPALiteralTypeOperand implements JPAExpressionElement<EdmType> {
  private final EdmType type;

  public JPALiteralTypeOperand(final EdmType type) {
    this.type = type;
  }

  @Override
  public EdmType get() throws ODataApplicationException {
    return type;
  }

  @Override
  public Expression getQueryExpressionElement() {
    return new TypeLiteralImpl(type);
  }
}
