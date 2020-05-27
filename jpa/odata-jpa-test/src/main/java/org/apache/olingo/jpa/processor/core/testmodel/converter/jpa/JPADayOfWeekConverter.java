package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.time.DayOfWeek;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class JPADayOfWeekConverter implements AttributeConverter<DayOfWeek, Integer> {

  @Override
  public Integer convertToDatabaseColumn(final DayOfWeek attribute) {
    if (attribute == null) {
      return null;
    }
    return Integer.valueOf(attribute.getValue());
  }

  @Override
  public DayOfWeek convertToEntityAttribute(final Integer dbData) {
    if (dbData == null) {
      return null;
    }
    return DayOfWeek.of(dbData.intValue());
  }
}
