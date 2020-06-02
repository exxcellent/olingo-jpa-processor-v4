package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

public class LocalDateTime2SqlTimestampODataAttributeConverter
implements ODataAttributeConverter<java.time.LocalDateTime, java.sql.Timestamp> {


  @Override
  public java.sql.Timestamp convertToOData(final LocalDateTime jpaValue) {
    if (jpaValue == null) {
      return null;
    }
    final Calendar calendar = Calendar.getInstance();
    calendar.clear();
    calendar.set(jpaValue.getYear(), jpaValue.getMonthValue() - 1, jpaValue.getDayOfMonth(), jpaValue.getHour(),
        jpaValue.getMinute(), jpaValue.getSecond());
    return java.sql.Timestamp.from(calendar.toInstant());
  }

  @Override
  public LocalDateTime convertToJPA(final java.sql.Timestamp oDataValue) {
    if (oDataValue == null) {
      return null;
    }
    return oDataValue.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
  }


}
