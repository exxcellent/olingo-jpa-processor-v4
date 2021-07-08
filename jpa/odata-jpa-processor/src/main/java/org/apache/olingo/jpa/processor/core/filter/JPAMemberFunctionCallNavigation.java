package org.apache.olingo.jpa.processor.core.filter;

import java.util.Arrays;
import java.util.List;

import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;
import org.apache.olingo.server.core.uri.queryoption.expression.MethodImpl;

class JPAMemberFunctionCallNavigation extends AbstractMemberNavigation {

  private final MethodKind methodCall;
  private final JPAMemberOperand<?> jpaMember;
  private final Expression[] parameters;
  private final int memberIndex;

  JPAMemberFunctionCallNavigation(final JPAEntityFilterProcessor<?> jpaComplier, final MethodKind methodCall,
      final List<JPAExpressionElement<?>> parameters) {

    super(jpaComplier);
    this.methodCall = methodCall;
    // build a new/internal parameter list with placeholder for 'navigation member'
    this.parameters = new Expression[parameters.size()];
    jpaMember = (JPAMemberOperand<?>) parameters.stream().filter(e -> JPAMemberOperand.class.isInstance(e)).findFirst()
        .get();
    for (int i = 0; i < parameters.size(); i++) {
      if (parameters.get(i) instanceof JPAMemberOperand) {
        continue;
      }
      this.parameters[i] = parameters.get(i).getQueryExpressionElement();
    }
    memberIndex = parameters.indexOf(jpaMember);
    assert memberIndex > -1;
  }

  @Override
  protected JPAMemberOperand<?> getNavigatingMember() {
    return jpaMember;
  }

  @Override
  public Expression getQueryExpressionElement() {
    // reuse existing call, but with original member (having all the navigation parts)
    return (Expression) buildResultingExpression(jpaMember.getMember());
  }

  @Override
  protected VisitableExpression buildResultingExpression(final Member attribute) {
    final List<Expression> unwrappedParameters = Arrays.asList(parameters);
    unwrappedParameters.set(memberIndex, attribute);
    return new MethodImpl(methodCall, unwrappedParameters);
  }

}
