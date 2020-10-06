package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributeAccessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 *
 * @author Ralf Zozmann
 *
 */
public class MethodAttributeAccessor implements JPAAttributeAccessor {

  private final Method methodRead;
  private final Method methodWrite;

  public MethodAttributeAccessor(final Method methodRead, final Method methodWrite) {
    this.methodRead = methodRead;
    this.methodWrite = methodWrite;
  }

  protected final Method getMethodRead() {
    return methodRead;
  }

  protected final Method getMethodWrite() {
    return methodWrite;
  }

  @Override
  public void setPropertyValue(final Object jpaEntity, final Object jpaPropertyValue) throws ODataJPAModelException {
    try {
      methodWrite.invoke(jpaEntity, jpaPropertyValue);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ODataJPAModelException(e);
    }
  }

  @Override
  public Object getDefaultPropertyValue() throws ODataJPAModelException {
    return null;
  }

  @Override
  public Object getPropertyValue(final Object jpaEntity) throws ODataJPAModelException {
    try {
      return methodRead.invoke(jpaEntity);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new ODataJPAModelException(e);
    }
  }

}
