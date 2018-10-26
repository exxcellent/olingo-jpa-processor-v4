package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.sql.Date;
import java.util.GregorianCalendar;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPATypeConvertor;

/**
 * Convert between OData attribute type {@link java.util.Calendar} and the JPA attribute type {@link java.sql.Date}.
 *
 * @author Ralf Zozmann
 *
 */
public class SqlDate2GregorianCalendarODataAttributeConverter implements ODataAttributeConverter<java.sql.Date, java.util.GregorianCalendar> {

	@Override
	public EdmPrimitiveTypeKind getODataType() {
		try {
			// delegate to the central mapping definition class
			return JPATypeConvertor.convertToEdmSimpleType(java.sql.Date.class);
		} catch (final ODataJPAModelException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GregorianCalendar convertToOData(final Date jpaValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Date convertToJPAEntity(final GregorianCalendar oDataValue) {
		if (oDataValue == null)
			return null;
		return new java.sql.Date(oDataValue.getTimeInMillis());
	}

}
