package org.apache.olingo.jpa.processor.core.exception;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAMessageTextBuffer;
import org.apache.olingo.server.api.ODataApplicationException;

public abstract class ODataJPAProcessException extends ODataApplicationException {

	private static final long serialVersionUID = -3178033271311091314L;
	private static final String UNKNOWN_MESSAGE = "No message text found";
	private static Collection<Locale> locales;

	public static Enumeration<Locale> getLocales() {
		if (locales == null)
			return Collections.emptyEnumeration();
		return Collections.enumeration(locales);
	}

	public static void setLocales(final Enumeration<Locale> locales) {
		if (locales == null)
			ODataJPAProcessException.locales = null;
		else
			ODataJPAProcessException.locales = Collections.list(locales);
	}

	protected final String id;
	protected final ODataJPAMessageTextBuffer messageBuffer;
	protected final String[] parameter;

	public ODataJPAProcessException(final String id, final HttpStatusCode statusCode) {
		super("", statusCode.getStatusCode(), determinePreferredLocale());
		this.id = id;
		this.messageBuffer = new ODataJPAMessageTextBuffer(getBundleName());
		this.parameter = null;
	}

	public ODataJPAProcessException(final Throwable cause, final HttpStatusCode statusCode) {
		super("", statusCode.getStatusCode(), determinePreferredLocale(), cause);
		this.id = null;
		this.messageBuffer = null;
		this.parameter = null;
	}

	public ODataJPAProcessException(final String id, final HttpStatusCode statusCode, final Throwable cause) {
		super("", statusCode.getStatusCode(), determinePreferredLocale(), cause);
		this.id = id;
		this.messageBuffer = new ODataJPAMessageTextBuffer(getBundleName());
		this.parameter = null;
	}

	public ODataJPAProcessException(final String id, final HttpStatusCode statusCode, final Throwable cause,
	        final String[] params) {
		super("", statusCode.getStatusCode(), determinePreferredLocale(), cause);
		this.id = id;
		this.messageBuffer = new ODataJPAMessageTextBuffer(getBundleName());
		this.parameter = params;
	}

	public ODataJPAProcessException(final String id, final HttpStatusCode statusCode, final String[] params) {
		super("", statusCode.getStatusCode(), determinePreferredLocale());
		this.id = id;
		this.messageBuffer = new ODataJPAMessageTextBuffer(getBundleName());
		this.parameter = params;
	}

	/**
	 *
	 * @return The first locale of the list of requested {@link #locales}.
	 */
	private static Locale determinePreferredLocale() {
		if (locales == null || locales.isEmpty())
			return Locale.ENGLISH;
		return locales.iterator().next();
	}

	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}

	@Override
	public String getMessage() {
		if (messageBuffer != null) {
			messageBuffer.setLocales(getLocales());
			return messageBuffer.getText(this, id, parameter);
		} else if (getCause() != null) {
			return getCause().getLocalizedMessage();
		} else
			return UNKNOWN_MESSAGE;
	}

	protected abstract String getBundleName();
}
