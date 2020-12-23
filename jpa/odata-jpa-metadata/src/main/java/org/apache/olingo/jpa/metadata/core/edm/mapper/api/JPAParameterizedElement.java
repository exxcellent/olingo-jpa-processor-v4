package org.apache.olingo.jpa.metadata.core.edm.mapper.api;

import javax.persistence.metamodel.PluralAttribute.CollectionType;

public interface JPAParameterizedElement extends JPADescribedElement {


  /**
   *
   * @return The type of collection if the attribute is a {@link #isCollection() collection} or <code>null</code>
   * otherwise. The <i>map</i> type is special: {@link #isCollection()} will return FALSE, because is not really handled
   * as collection in OData.
   */
  public CollectionType getCollectionType();

  public Integer getMaxLength();

  public Integer getPrecision();

  public Integer getScale();

  public boolean isNullable();

  /**
   *
   * @return TRUE if the member is an {@link java.util.Collection Collection&lt;...&gt;}.
   */
  public boolean isCollection();

}
