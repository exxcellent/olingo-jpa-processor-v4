package org.apache.olingo.jpa.processor.core.filter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.query.JPAQueryBuilderIfc;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.VisitableExpression;

/**
 * Cross compiles Olingo generated AST of an OData filter into JPA criteria builder where condition.
 *
 * Details can be found:
 * <a href=
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part1-protocol/odata-v4.0-errata02-os-part1-protocol-complete.html#_Toc406398301"
 * >OData Version 4.0 Part 1 - 11.2.5.1 System Query Option $filter </a>
 * <a href=
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part2-url-conventions/odata-v4.0-errata02-os-part2-url-conventions-complete.html#_Toc406398094"
 * >OData Version 4.0 Part 2 - 5.1.1 System Query Option $filter</a>
 * <a href=
 * "https://tools.oasis-open.org/version-control/browse/wsvn/odata/trunk/spec/ABNF/odata-abnf-construction-rules.txt">
 * odata-abnf-construction-rules</a>
 * @author Oliver Grande
 *
 */
//TODO handle $it ...
public class JPAEntityFilterProcessor extends JPAAbstractFilterProcessor {
  private final JPAODataDatabaseProcessor converter;
  // TODO Check if it is allowed to select via navigation
  // ...Organizations?$select=Roles/RoleCategory eq 'C'
  // see also https://issues.apache.org/jira/browse/OLINGO-414
  final EntityManager em;
  final OData odata;
  final IntermediateServiceDocument sd;
  final List<UriResource> uriResourceParts;
  final JPAQueryBuilderIfc parent;

  public JPAEntityFilterProcessor(final OData odata, final IntermediateServiceDocument sd, final EntityManager em,
      final JPAStructuredType jpaEntityType, final JPAODataDatabaseProcessor converter,
      final UriInfoResource uriResource, final JPAQueryBuilderIfc parent) {
    this(odata, sd, em, jpaEntityType, converter, uriResource.getUriResourceParts(), extractFilterExpression(
        uriResource), parent);
  }

  public JPAEntityFilterProcessor(final OData odata, final IntermediateServiceDocument sd, final EntityManager em,
      final JPAStructuredType jpaEntityType, final JPAODataDatabaseProcessor converter,
      final List<UriResource> resourcePath,
      final VisitableExpression expression, final JPAQueryBuilderIfc parent) {

    super(jpaEntityType, expression);

    this.uriResourceParts = resourcePath;
    this.converter = converter;
    this.em = em;
    this.odata = odata;
    this.sd = sd;
    this.parent = parent;
  }

  public static VisitableExpression extractFilterExpression(final UriInfoResource uriResource) {
    if (uriResource.getFilterOption() == null) {
      return null;
    }
    return uriResource.getFilterOption().getExpression();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Expression<Boolean> compile() throws ExpressionVisitException, ODataApplicationException {

    if (getExpression() == null) {
      return null;
    }
    final ExpressionVisitor<JPAExpressionElement<?>> visitor = new JPAVisitor(this);
    final Expression<Boolean> finalExpression = (Expression<Boolean>) getExpression().accept(visitor).get();

    return finalExpression;
  }

  @Override
  public JPAODataDatabaseProcessor getConverter() {
    return converter;
  }

  @Override
  public EntityManager getEntityManager() {
    return em;
  }

  @Override
  public OData getOdata() {
    return odata;
  }

  @Override
  public IntermediateServiceDocument getSd() {
    return sd;
  }

  @Override
  public List<UriResource> getUriResourceParts() {
    return uriResourceParts;
  }

  public JPAQueryBuilderIfc getParent() {
    return parent;
  }

}
