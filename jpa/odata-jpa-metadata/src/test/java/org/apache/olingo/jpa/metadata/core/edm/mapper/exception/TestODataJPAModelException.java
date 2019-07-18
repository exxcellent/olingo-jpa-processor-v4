package org.apache.olingo.jpa.metadata.core.edm.mapper.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class TestODataJPAModelException {

	private static String BUNDLE_NAME = "test-i18n";

	@Before
	public void prepareTest() {
		// reset
		TestException.setLocales(null);
	}

	@Test
	public void checkTextWithUnsupportedLocale() {
		try {
			TestException.setLocales(Collections.enumeration(Collections.singletonList(Locale.JAPANESE)));
			RaiseExeption();
		} catch (final ODataJPAException e) {
			assertEquals("An English message", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void checkTextInDefaultLocale() {
		final Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.JAPANESE);
		try {
			RaiseExeption();
		} catch (final ODataJPAException e) {
			assertEquals("An English message", e.getMessage());
			return;
		} finally {
			Locale.setDefault(defaultLocale);
		}
		fail();
	}

	@Test
	public void checkTextInGerman() {
		try {
			final ArrayList<Locale> localesList = new ArrayList<Locale>();
			localesList.add(Locale.GERMAN);
			final Enumeration<Locale> locales = Collections.enumeration(localesList);
			TestException.setLocales(locales);
			RaiseExeption();
		} catch (final ODataJPAException e) {
			assertEquals("Ein deutscher Text", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void checkTextInDefaultLocaleWithParameter() {
		try {
			RaiseExeptionParam();
		} catch (final ODataJPAException e) {
			assertEquals("Willi looks for Hugo", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void checkTextOnlyCause() {
		try {
			RaiseExeptionCause();
		} catch (final ODataJPAException e) {
			assertEquals("Test text from cause", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void checkTextIdAndCause() {
		final Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
		try {
			RaiseExeptionIDCause();
		} catch (final ODataJPAException e) {
			assertEquals("An English message", e.getMessage());
			return;
		} finally {
			Locale.setDefault(defaultLocale);
		}
		fail();
	}

	@Test
	public void checkTextIdAndCauseAndParameter() {
		try {
			RaiseExeptionIDCause("Willi", "Hugo");
		} catch (final ODataJPAException e) {
			assertEquals("Willi looks for Hugo", e.getMessage());
			return;
		}
		fail();
	}

	@Test
	public void checkTextNullId() {
		try {
			RaiseEmptyIDExeption();
		} catch (final ODataJPAException e) {
			assertEquals("No message text found", e.getMessage());
			return;
		}
		fail();
	}

	private void RaiseExeptionIDCause(final String... params) throws TestException {
		try {
			raiseNullPointer();
		} catch (final NullPointerException e) {
			if (params.length == 0)
				throw new TestException("FIRST_MESSAGE", e);
			else
				throw new TestException("SECOND_MESSAGE", e, params);
		}
	}

	private void RaiseExeptionCause() throws ODataJPAException {
		try {
			raiseNullPointer();
		} catch (final NullPointerException e) {
			throw new TestException(e);
		}
	}

	private void raiseNullPointer() throws NullPointerException {
		throw new NullPointerException("Test text from cause");
	}

	private void RaiseExeptionParam() throws ODataJPAException {
		throw new TestException("SECOND_MESSAGE", "Willi", "Hugo");
	}

	private void RaiseExeption() throws ODataJPAException {
		throw new TestException("FIRST_MESSAGE");
	}

	private void RaiseEmptyIDExeption() throws ODataJPAException {
		throw new TestException("");
	}

	private class TestException extends ODataJPAException {

		private static final long serialVersionUID = 1L;

		public TestException(final String id) {
			super(id);
		}

		public TestException(final String id, final String... params) {
			super(id, params);
		}

		public TestException(final Throwable e) {
			super(e);
		}

		public TestException(final String id, final Throwable e) {
			super(id, e);
		}

		public TestException(final String id, final Throwable e, final String[] params) {
			super(id, e, params);
		}

		@Override
		protected String getBundleName() {
			return BUNDLE_NAME;
		}
	}
}
