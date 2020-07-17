package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 *
 * @author Ralf Zozmann
 *
 */
class IntermediateActionFactory {

  private final static Logger LOGGER = Logger.getLogger(IntermediateActionFactory.class.getName());

  Map<? extends String, ? extends IntermediateAction> create(final JPAEdmNameBuilder nameBuilder,
      final Class<?> jpaEntityClass, final IntermediateServiceDocument isd) throws ODataJPAModelException {
    final Map<String, IntermediateAction> actionList = new HashMap<String, IntermediateAction>();

    for (final Method method : jpaEntityClass.getMethods()) {
      final EdmAction action = method.getAnnotation(EdmAction.class);
      if (action == null) {
        continue;
      }
      if(method.isAnnotationPresent(EdmIgnore.class)) {
        LOGGER.log(Level.WARNING,
            "Java method " + buildMethodSignature(method) + " has conflicting annotations @"
                + EdmAction.class.getSimpleName()+" and @"
                + EdmIgnore.class.getSimpleName() + ". Method will be ignored as edm:Action!");
        continue;
      }
      final Class<?> actionEntityClass = determineMostGenericEntityClass(jpaEntityClass,
          method.getDeclaringClass());
      if (jpaEntityClass != actionEntityClass) {
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.log(Level.FINEST,
              "Java method '" + method.getName() + "(...)' will be handled as action of "
                  + actionEntityClass.getSimpleName() + ", not as action of "
                  + jpaEntityClass.getSimpleName());
        }
        continue;
      }
      putAction(nameBuilder, isd, actionList, jpaEntityClass, method);
    }
    return actionList;
  }

  private void putAction(final JPAEdmNameBuilder nameBuilder, final IntermediateServiceDocument isd,
      final Map<String, IntermediateAction> actionList, final Class<?> jpaEntityClass, final Method actionMethod)
          throws ODataJPAModelException {
    final IntermediateAction action = new IntermediateAction(nameBuilder, jpaEntityClass, actionMethod, isd);
    if (actionList.containsKey(action.getInternalName())) {
      final IllegalStateException cause = new IllegalStateException("Duplicated action (name): "
          + action.getInternalName() + " -> " + buildMethodSignature(action.getJavaMethod()));
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INNER_EXCEPTION, cause);
    }
    actionList.put(action.getInternalName(), action);
  }

  /**
   *
   * @return The (highest, means most generic) class acting as separate entity in
   *         OData (used as entity for [bound] action).
   */
  private static @NotNull Class<?> determineMostGenericEntityClass(final Class<?> entityClass,
      final Class<?> methodDeclaringClass) {
    if (isJPAEntityClass(methodDeclaringClass)) {
      return methodDeclaringClass;
    }
    // work from owning entity class upwards to the method declaring class as
    // stopper criteria
    Class<?> currentEntityClass = entityClass;
    while (isJPAEntityClass(currentEntityClass.getSuperclass())
        && currentEntityClass.getSuperclass() != methodDeclaringClass) {
      currentEntityClass = currentEntityClass.getSuperclass();
    }
    return currentEntityClass;
  }

  /**
   *
   * @param clazz
   * @return TRUE if given class has the annotation {@link Entity @Entity.}
   */
  private static boolean isJPAEntityClass(final Class<?> clazz) {
    return (clazz.getAnnotation(Entity.class) != null);
  }

  private static String buildMethodSignature(final Method actionMethod) {
    return actionMethod.getDeclaringClass().getSimpleName()
        + "::" + actionMethod.getName() + "(...)";
  }
}
