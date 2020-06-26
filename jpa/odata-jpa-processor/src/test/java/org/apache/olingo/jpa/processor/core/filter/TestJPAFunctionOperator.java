package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;

import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAOperationResultParameter;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceFunction;
import org.junit.Before;
import org.junit.Test;

public class TestJPAFunctionOperator {
  private CriteriaBuilder cb;
  private JPAFunctionOperator cut;
  private UriResourceFunction uriFunction;
  private JPAVisitor jpaVisitor;
  private JPAFunction jpaFunction;
  private JPAOperationResultParameter jpaResultParam;
  private List<UriParameter> uriParams;

  @Before
  public void setUp() throws Exception {

    cb = mock(CriteriaBuilder.class);
    jpaVisitor = mock(JPAVisitor.class);
    when(jpaVisitor.getCriteriaBuilder()).thenReturn(cb);
    uriFunction = mock(UriResourceFunction.class);
    jpaFunction = mock(JPAFunction.class);
    jpaResultParam = mock(JPAOperationResultParameter.class);
    when(jpaFunction.getResultParameter()).thenReturn(jpaResultParam);
    final List<UriResource> resources = new ArrayList<UriResource>();
    resources.add(uriFunction);

    uriParams = new ArrayList<UriParameter>();

    cut = new JPAFunctionOperator(jpaVisitor, uriParams, jpaFunction);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testReturnsExpression() throws ODataApplicationException {

    final Expression<?>[] jpaParameter = new Expression<?>[0];

    when(jpaFunction.getDBName()).thenReturn("Test");
    doReturn(new Integer(5).getClass()).when(jpaResultParam).getType();
    doReturn(ValueType.PRIMITIVE).when(jpaResultParam).getResultValueType();
    when(cb.function(jpaFunction.getDBName(), jpaResultParam.getType(), jpaParameter)).thenReturn(mock(
        Expression.class));
    when(jpaFunction.getResultParameter()).thenReturn(jpaResultParam);
    final Expression<?> act = cut.get();
    assertNotNull(act);
  }

  @Test
  public void testAbortOnNonFunctionReturnsCollection() {

    when(jpaFunction.getDBName()).thenReturn("org.apache.olingo.jpa::Siblings");
    when(Boolean.valueOf(jpaResultParam.isCollection())).thenReturn(Boolean.TRUE);

    try {
      cut.get();
    } catch (final ODataApplicationException e) {
      return;
    }
    fail("Function provided not checked");
  }

  @Test
  public void testAbortOnNonScalarFunction() {

    when(jpaFunction.getDBName()).thenReturn("org.apache.olingo.jpa::Siblings");
    when(Boolean.valueOf(jpaResultParam.isCollection())).thenReturn(Boolean.FALSE);
    doReturn(AdministrativeDivision.class).when(jpaResultParam).getType();

    try {
      cut.get();
    } catch (final ODataApplicationException e) {
      return;
    }
    fail("Function provided not checked");
  }
}
