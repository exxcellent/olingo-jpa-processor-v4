package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

public class LocalDateTime2ZonedDateTimeODataAttributeConverter
implements ODataAttributeConverter<java.time.LocalDateTime, ZonedDateTime> {


  @Override
  public ZonedDateTime convertToOData(final LocalDateTime jpaValue) {
    if (jpaValue == null) {
      return null;
    }
    // handle as UTC to avoid time zone effects
    return ZonedDateTime.of(jpaValue, ZoneOffset.UTC);
  }

  @Override
  public LocalDateTime convertToJPA(final ZonedDateTime oDataValue) {
    if (oDataValue == null) {
      return null;
    }
    return oDataValue.toLocalDateTime();
  }


}
