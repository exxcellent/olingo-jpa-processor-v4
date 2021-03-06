package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.core.uri.queryoption.expression.MethodImpl;

class ODataBuiltinFunctionCallImpl implements ODataBuiltinFunctionCall {
  private final MethodKind methodCall;
  private final List<JPAExpressionElement<?>> parameters;
  private final JPAODataDatabaseProcessor converter;

  public ODataBuiltinFunctionCallImpl(final JPAODataDatabaseProcessor converter, final MethodKind methodCall,
      final List<JPAExpressionElement<?>> parameters) {
    super();
    this.methodCall = methodCall;
    this.parameters = parameters;
    this.converter = converter;
  }

  @Override
  public org.apache.olingo.server.api.uri.queryoption.expression.Expression getQueryExpressionElement() {
    final List<org.apache.olingo.server.api.uri.queryoption.expression.Expression> unwrappedParameters = parameters
        .stream().map(e -> e.getQueryExpressionElement()).collect(Collectors.toList());
    return new MethodImpl(methodCall, unwrappedParameters);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Expression<Object> get() throws ODataApplicationException {
    return (Expression<Object>) converter.convertBuiltinFunction(methodCall, parameters);
  }

  @Override
  public MethodKind getFunctionKind() {
    return methodCall;
  }

  @Override
  public JPAExpressionElement<?> getParameter(final int index) {
    return parameters.get(index);
  }

  @Override
  public int noParameters() {
    return parameters.size();
  }

  @Override
  public EdmPrimitiveTypeKind getResultType() {
    switch (methodCall) {
    case DATE:
      return EdmPrimitiveTypeKind.Date;
    case TIME:
      return EdmPrimitiveTypeKind.TimeOfDay;
    default:
      return null;
    }
  }
}
