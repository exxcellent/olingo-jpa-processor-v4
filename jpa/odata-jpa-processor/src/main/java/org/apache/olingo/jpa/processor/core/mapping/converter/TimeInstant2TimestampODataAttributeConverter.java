package org.apache.olingo.jpa.processor.core.mapping.converter;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

import java.sql.Timestamp;

/**
 * Convert between OData attribute type {@link java.sql.Date} and the JPA attribute type {@link java.time.Instant}.
 *
 * @author Nikolas Orlowitsch
 *
 */
public class TimeInstant2TimestampODataAttributeConverter
implements ODataAttributeConverter<java.time.Instant, java.sql.Timestamp> {

  @Override
  public java.sql.Timestamp convertToOData(final java.time.Instant jpaValue) {
    if (jpaValue == null) {
      return null;
    }
    return Timestamp.from(jpaValue);
  }

  @Override
  public java.time.Instant convertToJPA(final java.sql.Timestamp oDataValue) {
    if (oDataValue == null) {
      return null;
    }
    return oDataValue.toInstant();
  }
}
