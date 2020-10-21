package org.apache.olingo.jpa.processor.core.filter;

import org.apache.olingo.server.api.uri.queryoption.expression.Member;

class JPALambdaAnyOperation extends JPALambdaOperation {

  public JPALambdaAnyOperation(final JPAAbstractFilterProcessor<?> jpaComplier, final Member member) {
    super(jpaComplier, member);
  }

}
