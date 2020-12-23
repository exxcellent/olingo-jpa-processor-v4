package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;

public abstract class AbstractProperty<CsdlType extends CsdlAbstractEdmItem> extends
    IntermediateModelElement<CsdlType> {

  protected AbstractProperty(final JPAEdmNameBuilder nameBuilder, final String internalName) {
    super(nameBuilder, internalName);
  }

  abstract boolean isStream();

  /**
   * With the different JPA implementations we may have several scenarios:
   * <ul>
   * <li>The attribute is simply a field -&gt; the default case</li>
   * <li>The attribute is declared via (getter) method -&gt; the unsupported case</li>
   * <li>The attribute is declared by an field, but EclipseLink's weaving is covering the access with an method -&gt;
   * the case this method will work around</li>
   * </ul>
   * @param jpaAttribute The JPA attribute definition
   * @return The origin field or <code>null</code>
   */
  protected static final AnnotatedElement determineRealPropertyDeclarationElement(final Attribute<?, ?> jpaAttribute)
      throws UnsupportedOperationException {
    final Member member = jpaAttribute.getJavaMember();
    if (Field.class.isInstance(member)) {
      return (AnnotatedElement) member;
    } else if (Method.class.isInstance(member)) {
      return findField(jpaAttribute.getDeclaringType().getJavaType(), jpaAttribute.getName());
    } else {
      throw new UnsupportedOperationException("Unsupported kind of member: " + member);
    }
  }

  static Field findField(final Class<?> clazz, final String fieldName) {

    for (final Field field : clazz.getDeclaredFields()) {
      if (field.getName().equals(fieldName)) {
        return field;
      }
    }
    if (clazz.getSuperclass() == null) {
      return null;
    }
    return findField(clazz.getSuperclass(), fieldName);
  }

  static Method findMethod(final Class<?> clazz, final String methodName, final Class<?>... methodParameters) {
    for (final Method method : clazz.getDeclaredMethods()) {
      if (!method.getName().equals(methodName)) {
        continue;
      }
      if (Arrays.equals(method.getParameterTypes(), methodParameters)) {
        return method;
      }
    }
    if (clazz.getSuperclass() == null) {
      return null;
    }
    return findMethod(clazz.getSuperclass(), methodName, methodParameters);
  }
}
