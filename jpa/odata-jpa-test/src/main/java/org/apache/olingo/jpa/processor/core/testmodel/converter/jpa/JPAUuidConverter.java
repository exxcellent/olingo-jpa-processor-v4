package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.util.UUID;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

// Do not change the 'autoApply = true', it's required to match a test scenario
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
