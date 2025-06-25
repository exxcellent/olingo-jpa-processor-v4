package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.util.FieldAccess;

class FieldAttributeAccessor implements JPAAttributeAccessor {

  private final Field field;

  public FieldAttributeAccessor(final Field field) {
    this.field = field;
  }

  @Override
  public Object getDefaultPropertyValue() throws ODataJPAModelException {
    // It is not possible to get the default value directly from the Field,
    // only from an instance field.get(Object obj).toString();
    try {
      // FIXME
      // final Constructor<?> constructor = jpaAttribute.getDeclaringType().getJavaType().getConstructor();
      final Constructor<?> constructor = field.getDeclaringClass().getConstructor();
      final Object pojo = constructor.newInstance();
      return getPropertyValue(pojo);
    } catch (final InstantiationException | NoSuchMethodException e) {
      // Class could not be instantiated e.g. abstract class like Business Partner=>
      // default could not be determined
      // and will be ignored
    } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.PROPERTY_DEFAULT_ERROR, e,
          field.getName());
    }
    return null;
  }

  @Override
  public void setPropertyValue(final Object jpaEntity, final Object jpaPropertyValue) throws ODataJPAModelException {
    // don't try to set null values to primitive fields
    if (jpaPropertyValue == null && field.getType().isPrimitive()) {
      return;
    }
    try {
      FieldAccess.writeFieldValue(jpaEntity, field, jpaPropertyValue);
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new ODataJPAModelException(e);
    }
  }

  @Override
  public Object getPropertyValue(final Object jpaEntity) throws ODataJPAModelException {
    try {
      return FieldAccess.readFieldValue(jpaEntity, field);
    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
      throw new ODataJPAModelException(e);
    }
  }


}
