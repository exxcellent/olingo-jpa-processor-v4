package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import java.lang.annotation.Annotation;

import javax.persistence.metamodel.PluralAttribute.CollectionType;

public interface JPATypedElement {

  /**
   *
   * @return The direct type of simple attributes (or parameter or return value)
   * or the element type if the attribute is a collection.
   *
   * @see #getCollectionType()
   */
  public Class<?> getType();

  /**
   *
   * @return The type of collection if the attribute is a {@link #isCollection() collection} or <code>null</code>
   * otherwise.
   */
  public CollectionType getCollectionType();

  public Integer getMaxLength();

  public Integer getPrecision();

  public Integer getScale();

  public boolean isNullable();

  public boolean isCollection();

  /**
   * Wrapper to get annotation from the underlying property representation (field
   * , method,...).
   *
   * @param annotationClass The requested annotation class
   * @return The annotation or <code>null</code>.
   * @see java.lang.reflect.Field#getAnnotation(Class)
   */
  @Deprecated
  public <T extends Annotation> T getAnnotation(final Class<T> annotationClass);

}
