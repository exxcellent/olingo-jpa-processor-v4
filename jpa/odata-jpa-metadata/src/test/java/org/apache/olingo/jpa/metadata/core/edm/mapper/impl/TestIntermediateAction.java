package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateAction extends TestMappingRoot {
	private TestHelper helper;

	@Before
	public void setup() throws ODataJPAModelException {
		helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectNotAnnotatedMethod() throws ODataJPAModelException {
		Method notAnnotatedMethod = null;
		for (Method method : Person.class.getMethods()) {
			final EdmAction action = method.getAnnotation(EdmAction.class);
			// take the first not annotated method
			if (action == null) {
				notAnnotatedMethod = method;
				break;
			}
		}
		if (notAnnotatedMethod == null)
			throw new IllegalStateException("Couldn't find a JAVA method without @" + EdmAction.class.getSimpleName()
					+ " annotation for testing");
		new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME), notAnnotatedMethod, helper.schema);
		org.junit.Assert.fail("Action constructor has not thrown an exception");
	}

	@Test
	public void checkReflectionResult1() throws ODataJPAModelException {
		Method method = helper.getActionMethod(helper.getEntityType("Person"), "ClearPersonsCustomStrings");
		assertNotNull(method);
		IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
				method, helper.schema);
		assertNotNull(action);
		// a bound action must have different parameters in CSDL and on JPA side
		assertEquals(0, action.getParameters().size());
		assertEquals(1, action.getEdmItem().getParameters().size());
		assertEquals(void.class, action.getResultParameter().getType());
		assertNull(action.getEdmItem().getReturnType());
	}

	@Test
	public void checkReflectionResult2() throws ODataJPAModelException {
		Method method = helper.getActionMethod(helper.getEntityType("Person"), "DoNothingAction1");
		assertNotNull(method);
		IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
				method, helper.schema);
		assertFalse(action.getEdmItem().getReturnType().isNullable());
	}
	
	@Ignore("Unbound actions are currently not supported")
	@Test
	public void checkReflectionResult3() throws ODataJPAModelException {
		Method method = helper.getActionMethod(helper.getEntityType("Person"), "DoNothingAction2");
		assertNotNull(method);
		IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
				method, helper.schema);
		assertNotNull(action);
		// a unbound action must have same parameters in CSDL and on JPA side
		assertEquals(1, action.getParameters().size());
		assertEquals(1, action.getEdmItem().getParameters().size());
		assertEquals(Collection.class, action.getResultParameter().getType());
		assertTrue(action.getEdmItem().getReturnType().isNullable());
	}
}
