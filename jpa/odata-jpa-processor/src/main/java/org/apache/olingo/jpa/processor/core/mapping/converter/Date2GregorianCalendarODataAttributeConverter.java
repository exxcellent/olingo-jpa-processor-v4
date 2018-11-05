package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.util.GregorianCalendar;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPATypeConvertor;

/**
 * Convert between OData attribute type {@link java.util.Calendar} and the JPA attribute type {@link java.util.Date}.
 *
 * @author Ralf Zozmann
 *
 */
public class Date2GregorianCalendarODataAttributeConverter implements ODataAttributeConverter<java.util.Date, java.util.GregorianCalendar> {

	@Override
	public EdmPrimitiveTypeKind getODataType() {
		try {
			// delegate to the central mapping definition class
			return JPATypeConvertor.convertToEdmSimpleType(java.util.Date.class);
		} catch (final ODataJPAModelException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GregorianCalendar convertToOData(final java.util.Date jpaValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public java.util.Date convertToJPAEntity(final GregorianCalendar oDataValue) {
		if (oDataValue == null)
			return null;
		return oDataValue.getTime();
	}

}
