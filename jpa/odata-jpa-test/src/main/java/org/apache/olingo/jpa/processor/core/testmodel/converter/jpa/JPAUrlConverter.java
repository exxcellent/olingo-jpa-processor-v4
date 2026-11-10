package org.apache.olingo.jpa.processor.core.testmodel.converter.jpa;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
			return new URI(dbData).toURL();
		} catch (final MalformedURLException | URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}
}
