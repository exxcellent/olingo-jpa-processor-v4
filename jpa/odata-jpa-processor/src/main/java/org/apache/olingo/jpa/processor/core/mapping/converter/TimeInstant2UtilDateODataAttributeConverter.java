package org.apache.olingo.jpa.processor.core.mapping.converter;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Convert between OData attribute type {@link java.sql.Date} and the JPA attribute type {@link java.time.Instant}.
 *
 * @author Nikolas Orlowitsch
 *
 */
public class TimeInstant2UtilDateODataAttributeConverter
implements ODataAttributeConverter<java.time.Instant, java.sql.Date> {

  @Override
  public java.sql.Date convertToOData(final java.time.Instant jpaValue) {
    if (jpaValue == null) {
      return null;
    }
    return new java.sql.Date(jpaValue.toEpochMilli());
  }

  @Override
  public java.time.Instant convertToJPA(final java.sql.Date oDataValue) {
    if (oDataValue == null) {
      return null;
    }
    return oDataValue.toInstant();
  }
}
