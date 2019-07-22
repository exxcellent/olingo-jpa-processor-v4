package org.apache.olingo.jpa.processor.core.filter;

public interface JPAExpressionOperator<E extends Enum<?>, Y> extends JPAExpression<Y> {
  public E getOperator();

}
