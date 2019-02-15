package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.util.Calendar;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Convert between OData attribute type {@link java.util.Calendar} and the JPA attribute type {@link java.util.Date}.
 *
 * @author Ralf Zozmann
 *
 */
public class UtilDate2UtilCalendarODataAttributeConverter
implements ODataAttributeConverter<java.util.Date, java.util.Calendar> {

	@Override
	public Calendar convertToOData(final java.util.Date jpaValue) {
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
	public java.util.Date convertToJPA(final Calendar oDataValue) {
		if (oDataValue == null) {
			return null;
		}
		return oDataValue.getTime();
	}

}
