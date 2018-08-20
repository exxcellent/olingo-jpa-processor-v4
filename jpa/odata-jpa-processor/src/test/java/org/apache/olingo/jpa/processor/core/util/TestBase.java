package org.apache.olingo.jpa.processor.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.jpa.processor.core.database.JPA_HSQLDB_DatabaseProcessor;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.junit.Before;

public abstract class TestBase {

	protected static final String PUNIT_NAME = org.apache.olingo.jpa.processor.core.test.Constant.PUNIT_NAME;
	@Deprecated
	protected EntityManagerFactory emf;
	protected TestHelper helper;
	protected Map<String, List<String>> headers;
	protected final static JPAEdmNameBuilder nameBuilder = new JPAEdmNameBuilder(PUNIT_NAME);
	protected TestGenericJPAPersistenceAdapter persistenceAdapter;

	@Before
	public void setupTest() throws ODataJPAModelException {
		persistenceAdapter = new TestGenericJPAPersistenceAdapter(PUNIT_NAME, new JPA_HSQLDB_DatabaseProcessor(),
				DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
		emf = persistenceAdapter.getEMF();
	}

	protected void createHeaders() {
		headers = new HashMap<String, List<String>>();
		final List<String> languageHeaders = new ArrayList<String>();
		languageHeaders.add("de-DE,de;q=0.8,en-US;q=0.6,en;q=0.4");
		headers.put("accept-language", languageHeaders);
	}
}