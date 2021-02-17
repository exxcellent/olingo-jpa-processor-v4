package org.apache.olingo.jpa.processor.core.mapping.converter;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

public class Character2StringODataAttributeConverter implements ODataAttributeConverter<Character, String> {
	@Override
	public String convertToOData(Character ch) {
		return ch != null ? String.valueOf(ch) : null;
	}

	@Override
	public Character convertToJPA(String str) {
		return str != null ? str.charAt(0) : null;
	}
}
