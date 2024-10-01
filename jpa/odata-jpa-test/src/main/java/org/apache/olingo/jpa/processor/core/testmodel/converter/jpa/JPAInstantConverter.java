package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

//This converter has to be mentioned at all columns it is applicable
@Converter(autoApply = false)
public class JPAInstantConverter implements AttributeConverter<Instant, Timestamp> {

  @Override
  public Timestamp convertToDatabaseColumn(final Instant attribute) {
    if (attribute == null) {
      return null;
    }
    final LocalDateTime ldt = LocalDateTime.ofInstant(attribute, ZoneOffset.UTC);
    return Timestamp.valueOf(ldt);
  }

  @Override
  public Instant convertToEntityAttribute(final Timestamp dbData) {
    if (dbData == null) {
      return null;
    }
    return dbData.toInstant();
  }
}
