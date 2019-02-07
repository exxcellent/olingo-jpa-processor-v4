package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.jpa.processor.core.database.JPA_HSQLDBDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.test.Constant;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.junit.Before;

public abstract class TestBase {

	public enum JPAProvider {
		EclipseLink, Hibernate, OpenJPA;
	}

	protected TestHelper helper;
	protected Map<String, List<String>> headers;
	protected final static JPAEdmNameBuilder nameBuilder = new JPAEdmNameBuilder(Constant.PUNIT_NAME);
	protected TestGenericJPAPersistenceAdapter persistenceAdapter;


	@Before
	public void setupTest() throws ODataJPAModelException {
		persistenceAdapter = new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME,
				new JPA_HSQLDBDatabaseProcessor(),
				DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
	}

	protected JPAProvider getJPAProvider() {
		if (persistenceAdapter == null) {
			throw new IllegalStateException("setup test before");
		}
		if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.hibernate")) {
			return JPAProvider.Hibernate;
		}
		if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.apache.openjpa")) {
			return JPAProvider.OpenJPA;
		}
		if (persistenceAdapter.getEMF().getClass().getName().startsWith("org.eclipse.persistence")) {
			return JPAProvider.EclipseLink;
		}
		throw new UnsupportedOperationException("Current JPA provider not known");
	}

	protected void createHeaders() {
		headers = new HashMap<String, List<String>>();
		final List<String> languageHeaders = new ArrayList<String>();
		languageHeaders.add("de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
		headers.put("accept-language", languageHeaders);
	}
}