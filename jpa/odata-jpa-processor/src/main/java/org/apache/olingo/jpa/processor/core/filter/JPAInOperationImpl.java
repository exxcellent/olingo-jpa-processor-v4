package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAFilterException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.core.uri.queryoption.expression.BinaryImpl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.In;
import jakarta.persistence.criteria.Expression;

class JPAInOperationImpl implements JPAExpressionOperation<BinaryOperatorKind, Boolean> {
  private final CriteriaBuilder cb;
  private final JPAExpressionElement<?> left;
  private final List<JPAExpressionElement<?>> right;
  private final EdmType rightType;

  public JPAInOperationImpl(final JPAEntityFilterProcessor<?> jpaComplier, final JPAExpressionElement<?> left, final List<JPAExpressionElement<?>> right) throws ODataApplicationException {
    this.cb = jpaComplier.getEntityManager().getCriteriaBuilder();
    this.left = left;
    this.right = right;
    this.rightType = determineType(right);
  }

  private static EdmType determineType(List<JPAExpressionElement<?>> right) throws ODataApplicationException {
    if(right.isEmpty()) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.NOT_SUPPORTED_FILTER, HttpStatusCode.EXPECTATION_FAILED, "Empty collection");
    }
    for(JPAExpressionElement<?> element: right) {
      if(!JPALiteralOperand.class.isInstance(element)) {
        throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.EXPECTATION_FAILED, "elements in collection must be literals");
      }
    }
    
    EdmType expectedType = ((Literal)((JPALiteralOperand)right.get(0)).getQueryExpressionElement()).getType();
    if(!EdmPrimitiveType.class.isInstance(expectedType)) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.EXPECTATION_FAILED, "first element in collection must be a primitive type");
    }
    EdmPrimitiveType numberType = EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int64);
    if(numberType.isCompatible((EdmPrimitiveType) expectedType)) {
      //an entry of value '0' will have type SByte and other entries other number types... we force to the highest one
      expectedType = numberType;
    }
    if(expectedType == null) {
      throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.EXPECTATION_FAILED, "first element in collection has no implicite type");
    }
    for(int i=1;i<right.size();i++) {
      EdmType type = ((Literal)((JPALiteralOperand)right.get(i)).getQueryExpressionElement()).getType();
      if(!EdmPrimitiveType.class.isInstance(expectedType)) {
        throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.EXPECTATION_FAILED, "element in collection must be a primitive type");
      }
      if(!((EdmPrimitiveType)expectedType).isCompatible((EdmPrimitiveType) type)) {
        throw new ODataJPAFilterException(ODataJPAFilterException.MessageKeys.RUNTIME_PROBLEM, HttpStatusCode.EXPECTATION_FAILED, "all elements in collection must be of same type");
      }
    }
    return expectedType;
  }
  
  @Override
  public Expression<Boolean> get() throws ODataApplicationException {
    In<Object> inClause = cb.in((Expression<?>) left.get());
    for(JPAExpressionElement<?> element: right) {
      inClause.value(((JPALiteralOperand)element).getLiteralExpression());
    }
    return inClause;
  }

  @Override
  public org.apache.olingo.server.api.uri.queryoption.expression.Expression getQueryExpressionElement() {
    final List<org.apache.olingo.server.api.uri.queryoption.expression.Expression> inElements = right.stream().map(r -> r.getQueryExpressionElement()).toList();
    return new BinaryImpl(left.getQueryExpressionElement(), BinaryOperatorKind.IN, inElements, rightType);
  }

  @Override
  public BinaryOperatorKind getOperator() {
    return BinaryOperatorKind.IN;
  }

}
