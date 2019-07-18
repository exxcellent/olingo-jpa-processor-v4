package org.apache.olingo.jpa.metadata.core.edm.mapper.exception;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestODataJPAMessageTextBuffer {

	private static String BUNDLE_NAME = "test-i18n";
	private ODataJPAMessageTextBuffer mtb;

	@BeforeClass
	public static void prepare() {
		// force the JRE to use our default locale instead of system dependent one
		Locale.setDefault(ODataJPAMessageTextBuffer.DEFAULT_LOCALE);
	}

	@Before
	public void setup() {
		mtb = new ODataJPAMessageTextBuffer(BUNDLE_NAME);
	}

	@Test
	public void checkDefaultLocale() {
		assertEquals(ODataJPAMessageTextBuffer.DEFAULT_LOCALE.getLanguage(), mtb.getLocale().getLanguage());
	}

	@Test
	public void checkSetLocaleGerman() {
		mtb.setLocale(Locale.GERMANY);
		assertEquals("de", mtb.getLocale().getLanguage());
	}

	@Test
	public void checkSetLocaleReset() {
		// Set first to German
		checkSetLocaleGerman();
		// Then reset to default
		mtb.setLocale(null);
		assertEquals(ODataJPAMessageTextBuffer.DEFAULT_LOCALE.getLanguage(), mtb.getLocale().getLanguage());
	}

	@Test
	public void checkGetDefaultLocaleText() {
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("An English message", act);
	}

	@Test
	public void checkGetGermanText() {
		mtb.setLocale(Locale.GERMAN);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("Ein deutscher Text", act);
	}

	@Test
	public void checkGetOtherBundle() {
		mtb.setBundleName("test-i18n2");
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("Another English message", act);
	}

	// %1$s
	@Test
	public void checkGetTextWithParameter() {
		final String act = mtb.getText(this, "SECOND_MESSAGE", "Hugo", "Willi");
		assertEquals("Hugo looks for Willi", act);
	}

	@Test
	public void checkSetLocalesNull() {
		final Enumeration<Locale> locales = null;
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("An English message", act);
	}

	@Test
	public void checkSetLocalesRestDefaultWithNull() {
		// First set to German
		checkSetLocaleGerman();
		// Then reset default
		final Enumeration<Locale> locales = null;
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("An English message", act);
	}

	@Test
	public void checkSetLocalesRestDefaultWithEmpty() {
		// First set to German
		checkSetLocaleGerman();
		// Then reset default
		final Enumeration<Locale> locales = Collections.emptyEnumeration();
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("An English message", act);
	}

	@Test
	public void checkSetLocalesFirstMatches() {

		final ArrayList<Locale> localesList = new ArrayList<Locale>();
		localesList.add(Locale.GERMAN);
		localesList.add(Locale.CANADA_FRENCH);
		final Enumeration<Locale> locales = Collections.enumeration(localesList);
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("Ein deutscher Text", act);
	}

	@Test
	public void checkSetLocalesSecondMatches() {

		final ArrayList<Locale> localesList = new ArrayList<Locale>();
		localesList.add(Locale.CANADA_FRENCH);
		localesList.add(Locale.GERMAN);
		final Enumeration<Locale> locales = Collections.enumeration(localesList);
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("Ein deutscher Text", act);
	}

	@Test
	public void checkSetLocalesNonMatches() {

		final ArrayList<Locale> localesList = new ArrayList<Locale>();
		localesList.add(Locale.CANADA_FRENCH);
		localesList.add(Locale.SIMPLIFIED_CHINESE);
		final Enumeration<Locale> locales = Collections.enumeration(localesList);
		mtb.setLocales(locales);
		final String act = mtb.getText(this, "FIRST_MESSAGE");
		assertEquals("An English message", act);
	}
}
