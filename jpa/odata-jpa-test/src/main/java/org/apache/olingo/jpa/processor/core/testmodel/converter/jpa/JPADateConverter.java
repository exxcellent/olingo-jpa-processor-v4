package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

//This converter has to be mentioned at all columns it is applicable
@Converter(autoApply = false)
public class JPADateConverter implements AttributeConverter<LocalDate, Date> {

  public Date convertToDatabaseColumn(LocalDate locDate) {
    return (locDate == null ? null : Date.valueOf(locDate));
  }

  public LocalDate convertToEntityAttribute(Date sqlDate) {
    return (sqlDate == null ? null : sqlDate.toLocalDate());
  }

}
