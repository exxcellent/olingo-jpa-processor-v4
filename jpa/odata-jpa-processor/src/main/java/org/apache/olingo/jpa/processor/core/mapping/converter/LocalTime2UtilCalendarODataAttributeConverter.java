package org.apache.olingo.jpa.processor.core.mapping.converter;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;

import org.apache.olingo.jpa.metadata.core.edm.converter.ODataAttributeConverter;

public class LocalTime2UtilCalendarODataAttributeConverter
implements ODataAttributeConverter<java.time.LocalTime, java.util.Calendar> {

	@Override
	public java.util.Calendar convertToOData(final LocalTime jpaValue) {
		if (jpaValue == null) {
			return null;
		}
		final Calendar calendar = Calendar.getInstance();
		calendar.clear();
		// assuming year/month/date information is not important
		calendar.set(0, 0, 0, jpaValue.getHour(), jpaValue.getMinute(), jpaValue.getSecond());
		return calendar;
	}

	@Override
	public LocalTime convertToJPA(final java.util.Calendar oDataValue) {
		if (oDataValue == null) {
			return null;
		}
		return oDataValue.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
	}
}
