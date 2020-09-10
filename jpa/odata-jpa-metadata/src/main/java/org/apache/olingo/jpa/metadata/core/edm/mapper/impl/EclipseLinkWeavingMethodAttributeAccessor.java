package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;

/**
 * Special method access implementation for properties affected by weaving of EclipseLink to workaround the
 * {@link org.eclipse.persistence.indirection.ValueHolder ValueHolder} problem for relationship attributes.
 *
 * @see org.eclipse.persistence.internal.jpa.weaving.ClassWeaver
 * @author Ralf Zozmann
 *
 */
public class EclipseLinkWeavingMethodAttributeAccessor extends MethodAttributeAccessor {

  private final static Logger LOG = Logger.getLogger(JPAAttributeAccessor.class.getName());

  private static final String PERSISTENCE_FIELDNAME_PREFIX = "_persistence_";
  private static final String PERSISTENCE_FIELDNAME_POSTFIX = "_vh";// mark value holder getter/setter method

  private static boolean weavingDetectedAlreadyLogged = false;

  public EclipseLinkWeavingMethodAttributeAccessor(final Attribute<?, ?> jpaAttribute) {
    super(findReadMethod(jpaAttribute), findWriteMethod(jpaAttribute));
    // the java member is normally the synthetic getter method created by class weaver
    if (!isEclipseLinkValueHolderWeavingMethod((Method) jpaAttribute.getJavaMember())) {
      throw new IllegalStateException(jpaAttribute.getName() + " belongs not to an weaving attribute");
    }
    assert getMethodRead() != null;
    assert getMethodWrite() != null;
    //    final Class<?> weaverdJPAClass = jpaAttribute.getDeclaringType().getJavaType();
    //    System.out.println("--- " + weaverdJPAClass.getName());
    //    for (final Method m : weaverdJPAClass.getDeclaredMethods()) {
    //      System.out.println("  # " + m.toGenericString());
    //    }
    synchronized (LOG) {
      if (!weavingDetectedAlreadyLogged) {
        weavingDetectedAlreadyLogged = true;
        LOG.info(
            "EclipseLink byte code enhancement (weaving) detected, use special attribute accessor for entity model");
      }
    }
  }

  private static Method findWriteMethod(final Attribute<?, ?> jpaAttribute) {
    final Class<?> clazz = jpaAttribute.getDeclaringType().getJavaType();
    // magic to determine the EclipseLink Weaver generated setter method
    final String methodName = PERSISTENCE_FIELDNAME_PREFIX + "set_" + jpaAttribute.getName();
    return AbstractProperty.findMethod(clazz, methodName, jpaAttribute.getJavaType());
  }

  private static Method findReadMethod(final Attribute<?, ?> jpaAttribute) {
    final Class<?> clazz = jpaAttribute.getDeclaringType().getJavaType();
    // magic to determine the EclipseLink Weaver generated getter method
    final String methodName = PERSISTENCE_FIELDNAME_PREFIX + "get_" + jpaAttribute.getName();
    return AbstractProperty.findMethod(clazz, methodName);
  }

  static boolean isEclipseLinkValueHolderWeavingMethod(final Method method) {
    if (!method.getName().startsWith(PERSISTENCE_FIELDNAME_PREFIX)) {
      return false;
    }
    if (!method.getName().endsWith(PERSISTENCE_FIELDNAME_POSTFIX)) {
      return false;
    }
    return true;
  }
}
