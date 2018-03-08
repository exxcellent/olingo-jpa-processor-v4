package org.apache.olingo.jpa.processor.core.testmodel.converter;

import java.util.UUID;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class JPAUuidConverter implements AttributeConverter<UUID, String> {

	@Override
	public String convertToDatabaseColumn(final UUID attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.toString();
	}

	@Override
	public UUID convertToEntityAttribute(final String dbData) {
		if (dbData == null) {
			return null;
		}
		return UUID.fromString(dbData);
	}
}
