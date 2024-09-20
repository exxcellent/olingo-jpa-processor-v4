package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.test.util.TestMappingRoot;
import org.apache.olingo.jpa.processor.core.testmodel.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestIntermediateAction extends TestMappingRoot {
  private TestHelper helper;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
  }

  @Test
  public void rejectNotAnnotatedMethod() throws ODataJPAModelException {
    final Method[] notAnnotatedMethod = new Method[] { null };
    for (final Method method : Person.class.getMethods()) {
      final EdmAction action = method.getAnnotation(EdmAction.class);
      // take the first not annotated method
      if (action == null) {
        notAnnotatedMethod[0] = method;
        break;
      }
    }
    if (notAnnotatedMethod[0] == null) {
      throw new IllegalStateException("Couldn't find a JAVA method without @" + EdmAction.class.getSimpleName()
          + " annotation for testing");
    }
    Assertions.assertThrows(IllegalArgumentException.class, () -> new IntermediateAction(new JPAEdmNameBuilder(
        PUNIT_NAME), Person.class, notAnnotatedMethod[0],
        helper.getServiceDocument()));
  }

  @Test
  public void checkReflectionResult1() throws ODataJPAModelException {
    final Method method = helper.getActionMethod(helper.getEntityType("Person"), "ClearPersonsCustomStrings");
    assertNotNull(method);
    final IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
        Person.class, method, helper.getServiceDocument());
    assertNotNull(action);
    // a bound action must have different parameters in CSDL and on JPA side
    assertEquals(0, action.getParameters().size());
    assertEquals(1, action.getEdmItem().getParameters().size());
    assertNull(action.getResultParameter());// void
    assertNull(action.getEdmItem().getReturnType());
  }

  @Test
  public void checkReflectionResult2() throws ODataJPAModelException {
    final Method method = helper.getActionMethod(helper.getEntityType("Person"), "DoNothingAction1");
    assertNotNull(method);
    final IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
        Person.class, method, helper.getServiceDocument());
    assertFalse(action.getEdmItem().getReturnType().isNullable());
  }

  @Test
  public void checkReflectionResult3() throws ODataJPAModelException {
    final Method method = helper.getActionMethod(helper.getEntityType("Person"), "DoNothingAction2");
    assertNotNull(method);
    final IntermediateAction action = new IntermediateAction(new JPAEdmNameBuilder(PUNIT_NAME),
        Person.class, method, helper.getServiceDocument());
    assertNotNull(action);
    assertEquals(1, action.getParameters().size());
    // the CSDL has the entity as additional parameter for bound action
    assertEquals(2, action.getEdmItem().getParameters().size());
    assertEquals(Person.class, action.getResultParameter().getType());
    assertTrue(action.getEdmItem().getReturnType().isNullable());
    assertTrue(action.getResultParameter().isCollection());
  }
}
