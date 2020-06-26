package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class JPAUuidFragmentsListConverter implements AttributeConverter<List<String>, String> {

  @Override
  public String convertToDatabaseColumn(final List<String> listAttribute) {
    if (listAttribute == null || listAttribute.isEmpty()) {
      return null;
    }
    return String.join("-", listAttribute);
  }

  @Override
  public List<String> convertToEntityAttribute(final String dbData) {
    if (dbData == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(dbData.split("-"));
  }
}
