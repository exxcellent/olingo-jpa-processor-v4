package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.util.Calendar;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Convert between OData attribute type {@link java.util.Calendar} and the JPA attribute type {@link java.sql.Date}.
 *
 * @author Ralf Zozmann
 *
 */
public class SqlDate2UtilCalendarODataAttributeConverter
implements ODataAttributeConverter<java.sql.Date, java.util.Calendar> {

	@Override
	public java.util.Calendar convertToOData(final java.sql.Date jpaValue) {
		if (jpaValue == null) {
			return null;
		}
		// create new instance
		final Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.setTime(jpaValue);
		return calendar;
	}

	@Override
	public java.sql.Date convertToJPA(final java.util.Calendar oDataValue) {
		if (oDataValue == null) {
			return null;
		}
		return new java.sql.Date(oDataValue.getTimeInMillis());
	}

}
