package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.net.MalformedURLException;
import java.net.URL;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class JPAUrlConverter implements AttributeConverter<URL, String> {

	@Override
	public String convertToDatabaseColumn(final URL attribute) {
		if (attribute == null) {
			return null;
		}
		return attribute.toExternalForm();
	}

	@Override
	public URL convertToEntityAttribute(final String dbData) {
		if (dbData == null) {
			return null;
		}
		try {
			return new URL(dbData);
		} catch (final MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
}
