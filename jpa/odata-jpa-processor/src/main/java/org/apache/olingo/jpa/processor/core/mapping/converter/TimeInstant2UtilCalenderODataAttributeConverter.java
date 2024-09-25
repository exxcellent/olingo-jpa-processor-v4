package org.apache.olingo.jpa.processor.core.mapping.converter;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

import java.util.Calendar;
import java.util.Date;

/**
 * Convert between OData attribute type {@link java.sql.Date} and the JPA attribute type {@link java.time.Instant}.
 *
 * @author Nikolas Orlowitsch
 *
 */
public class TimeInstant2UtilCalenderODataAttributeConverter
implements ODataAttributeConverter<java.time.Instant, java.util.Calendar> {

  @Override
  public java.util.Calendar convertToOData(final java.time.Instant jpaValue) {
    if (jpaValue == null) {
      return null;
    }
    Date date = Date.from(jpaValue);
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return calendar;
  }

  @Override
  public java.time.Instant convertToJPA(final java.util.Calendar oDataValue) {
    if (oDataValue == null) {
      return null;
    }
    return oDataValue.toInstant();
  }
}
