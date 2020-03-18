package org.apache.olingo.jpa.processor.core.query;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPATypedElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAConversionException;

/**
 * Helper class to convert attribute values between OData and JPA without entity as context.
 *
 * @author Ralf Zozmann
 *
 */
public final class ValueConverter extends AbstractConverter {

  public ValueConverter() {
    super();
  }

  @Override
  public Object convertJPA2ODataPrimitiveValue(final JPATypedElement attribute, final Object jpaValue)
      throws ODataJPAConversionException, ODataJPAModelException {
    if (Enum.class.isAssignableFrom(attribute.getType())) {
      // enums are handled 'as is'
      return jpaValue;
    }
    return super.convertJPA2ODataPrimitiveValue(attribute, jpaValue);
  }

}
