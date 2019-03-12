package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestJPAArithmeticOperator {
	private CriteriaBuilder cb;

	private JPAODataDatabaseProcessor converter;
	private Path<Integer> expression;

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		converter = mock(JPAODataDatabaseProcessor.class);
		cb = mock(CriteriaBuilder.class);
		expression = mock(Path.class);
	}

	@Test
	public void testMemberLiteralGetLeft_Member() throws ODataApplicationException {
		final JPAMemberOperator left = mock(JPAMemberOperator.class);
		final JPALiteralOperator right = mock(JPALiteralOperator.class);

		when(right.getLiteralValue()).thenReturn(Integer.valueOf(5));
		when(left.get()).thenAnswer(new Answer<Path<Integer>>() {
			@Override
			public Path<Integer> answer(final InvocationOnMock invocation) throws Throwable {
				return expression;
			}
		});

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(expression, cut.getLeft(cb));
	}

	@Test
	public void testLiteralMemberGetLeft_Member() throws ODataApplicationException {
		final JPAMemberOperator right = mock(JPAMemberOperator.class);
		final JPALiteralOperator left = mock(JPALiteralOperator.class);

		when(left.getLiteralValue()).thenReturn(Integer.valueOf(5));
		when(right.get()).thenAnswer(new Answer<Path<Integer>>() {
			@Override
			public Path<Integer> answer(final InvocationOnMock invocation) throws Throwable {
				return expression;
			}
		});

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(expression, cut.getLeft(cb));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetLeftLiteralLiteral_Left() throws ODataApplicationException {
		final JPALiteralOperator right = mock(JPALiteralOperator.class);
		final JPALiteralOperator left = mock(JPALiteralOperator.class);
		final Integer leftValue = new Integer(5);

		final Expression<Number> result = mock(Expression.class);

		when(left.get()).thenAnswer(new Answer<Expression<Number>>() {
			@Override
			public Expression<Number> answer(final InvocationOnMock invocation) throws Throwable {
				return cb.literal(leftValue);
			}
		});
		when(right.getLiteralValue()).thenReturn(Integer.valueOf(10));

		when(cb.literal(leftValue)).thenAnswer(new Answer<Expression<Number>>() {
			@Override
			public Expression<Number> answer(final InvocationOnMock invocation) throws Throwable {
				invocation.getArguments();
				return result;
			}
		});

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		final Expression<Number> act = cut.getLeft(cb);
		assertEquals(result, act);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetLeftMemberMember_Left() throws ODataApplicationException {
		final JPAMemberOperator right = mock(JPAMemberOperator.class);
		final JPAMemberOperator left = mock(JPAMemberOperator.class);

		final Path<Integer> expressionRight = mock(Path.class);

		when(right.get()).thenAnswer(new Answer<Path<Integer>>() {
			@Override
			public Path<Integer> answer(final InvocationOnMock invocation) throws Throwable {
				return expressionRight;
			}
		});
		when(left.get()).thenAnswer(new Answer<Path<Integer>>() {
			@Override
			public Path<Integer> answer(final InvocationOnMock invocation) throws Throwable {
				return expression;
			}
		});

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(expression, cut.getLeft(cb));

	}

	@Test
	public void testMemberLiteralGetRightAsNumber_Right() throws ODataApplicationException {
		final JPAMemberOperator left = mock(JPAMemberOperator.class);
		final JPALiteralOperator right = mock(JPALiteralOperator.class);
		final JPASimpleAttribute attribute = mock(JPASimpleAttribute.class);

		when(right.getLiteralValue(attribute)).thenReturn(new BigDecimal("5.1"));
		when(left.determineAttribute()).thenReturn(attribute);

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(new BigDecimal("5.1"), cut.getRightAsNumber(cb));
	}

	@Test
	public void testLiteralMemberGetRightAsNumber_Left() throws ODataApplicationException {
		final JPAMemberOperator right = mock(JPAMemberOperator.class);
		final JPALiteralOperator left = mock(JPALiteralOperator.class);
		final JPASimpleAttribute attribute = mock(JPASimpleAttribute.class);

		when(left.getLiteralValue(attribute)).thenReturn(new BigDecimal("5.1"));
		when(right.determineAttribute()).thenReturn(attribute);

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(new BigDecimal("5.1"), cut.getRightAsNumber(cb));
	}

	@Test
	public void testLiteralLiteralGetRightAsNumber_Right() throws ODataApplicationException {
		final JPALiteralOperator right = mock(JPALiteralOperator.class);
		final JPALiteralOperator left = mock(JPALiteralOperator.class);

		when(left.getLiteralValue()).thenReturn(new BigDecimal("5.1"));
		when(right.getLiteralValue()).thenReturn(new BigDecimal("10.1"));

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		assertEquals(new BigDecimal("10.1"), cut.getRightAsNumber(cb));
	}

	@Test
	public void testGetMemberMemberGetRightAsNumber_Exeption() throws ODataApplicationException {
		final JPAMemberOperator right = mock(JPAMemberOperator.class);
		final JPAMemberOperator left = mock(JPAMemberOperator.class);
		final JPASimpleAttribute attribute = mock(JPASimpleAttribute.class);

		when(left.determineAttribute()).thenReturn(attribute);
		when(right.determineAttribute()).thenReturn(attribute);

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		try {
			cut.getRightAsNumber(cb);
		} catch (final ODataApplicationException e) {
			return;
		}
		fail("Exception expecetd");
	}

	@Test
	public void testGetBooleanMemberGetRightAsNumber_Exeption() throws ODataApplicationException {
		final JPAMemberOperator right = mock(JPAMemberOperator.class);
		final JPABooleanOperatorImp left = mock(JPABooleanOperatorImp.class);

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		try {
			cut.getRightAsNumber(cb);
		} catch (final ODataApplicationException e) {
			return;
		}
		fail("Exception expecetd");
	}

	@Test
	public void testGetMemberBooleanGetRightAsNumber_Exeption() throws ODataApplicationException {
		final JPAMemberOperator left = mock(JPAMemberOperator.class);
		final JPABooleanOperatorImp right = mock(JPABooleanOperatorImp.class);

		final JPAArithmeticOperator cut = new JPAArithmeticOperatorImp(converter, BinaryOperatorKind.ADD, left, right);
		try {
			cut.getRightAsNumber(cb);
		} catch (final ODataApplicationException e) {
			return;
		}
		fail("Exception expecetd");
	}
}
