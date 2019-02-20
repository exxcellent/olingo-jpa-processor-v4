package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.util.Calendar;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

/**
 * Convert between OData attribute type {@link java.util.Calendar} and the JPA attribute type {@link java.util.Date}.
 *
 * @author Ralf Zozmann
 *
 */
public class LocalDate2UtilCalendarODataAttributeConverter
        implements ODataAttributeConverter<java.time.LocalDate, java.util.Calendar> {

	@Override
	public java.util.Calendar convertToOData(final java.time.LocalDate jpaValue) {
		if (jpaValue == null) {
			return null;
		}
		final Calendar calendar = Calendar.getInstance();
		calendar.clear();
		// assuming year/month/date information is not important
		calendar.set(jpaValue.getYear(), jpaValue.getMonthValue() - 1, jpaValue.getDayOfMonth());
		return calendar;
	}

	@Override
	public java.time.LocalDate convertToJPA(final java.util.Calendar oDataValue) {
		if (oDataValue == null) {
			return null;
		}
		return java.time.LocalDate.of(oDataValue.get(Calendar.YEAR), oDataValue.get(Calendar.MONTH) + 1,
		        oDataValue.get(Calendar.DAY_OF_MONTH));
	}

}
