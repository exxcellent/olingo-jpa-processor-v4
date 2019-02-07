package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Ignore;
import org.junit.Test;

public class TestJPAFunction extends TestBase {
	// protected static final String PUNIT_NAME = "org.apache.olingo.jpa";
	// protected static EntityManagerFactory emf;
	// protected static TestGenericJPAPersistenceAdapter persistenceAdapter;
	//
	// protected TestHelper helper;
	// protected Map<String, List<String>> headers;
	// protected static JPAEdmNameBuilder nameBuilder;
	//
	// @Before
	// public void setup() {
	// persistenceAdapter = new TestGenericJPAPersistenceAdapter(PUNIT_NAME, new
	// JPA_HSQLDBDatabaseProcessor(),
	// DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
	// emf = persistenceAdapter.getEMF();
	//
	// }

	@Ignore // TODO check is path is in general allowed
	@Test
	public void testNavigationAfterFunctionNotAllowed() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Siblings(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')/Parent");
		helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
	}

	// FIXME
	@Ignore("Curently not working")
	@Test
	public void testFunctionGenerateQueryString() throws IOException, ODataException, SQLException {

		createSiblingsFunction();

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Siblings(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		assertTrue(helper.getValues().size() > 0);
	}

	/**
	 * Create function on-demand, not in the *.sql file, because Derby doesn't
	 * support it currently (wrong datatypes?)
	 */
	private void createSiblingsFunction() {
		final StringBuffer sqlString = new StringBuffer();

		sqlString.append("create function \"OLINGO\".\"org.apache.olingo.jpa::Siblings\""); // \"OLINGO\".
		sqlString.append("( CodePublisher VARCHAR(10), CodeID VARCHAR(10), DivisionCode VARCHAR(10))");
		sqlString.append(
				" RETURNS TABLE (\"CodePublisher\" VARCHAR(10), \"CodeID\" VARCHAR(10), \"DivisionCode\" VARCHAR(10),");
		sqlString.append(
				"\"CountryISOCode\"  VARCHAR(4), \"ParentCodeID\"  VARCHAR(10), \"ParentDivisionCode\"  VARCHAR(10),");
		sqlString.append("\"AlternativeCode\"  NVARCHAR(10),  \"Area\"  INTEGER, \"Population\"  BIGINT )");
		sqlString.append(" READS SQL DATA");
		sqlString.append(" RETURN TABLE (SELECT ");
		sqlString.append(
				"a.\"CodePublisher\", a.\"CodeID\", a.\"DivisionCode\", a.\"CountryISOCode\",a.\"ParentCodeID\"");
		sqlString.append(",a.\"ParentDivisionCode\", a.\"AlternativeCode\",a.\"Area\", a.\"Population\"");
		sqlString.append(" FROM \"OLINGO\".\"org.apache.olingo.jpa::AdministrativeDivision\" as a);");

		final EntityManager em = persistenceAdapter.createEntityManager();
		persistenceAdapter.beginTransaction(em);
		final javax.persistence.Query q = em.createNativeQuery(sqlString.toString());
		q.executeUpdate();

		persistenceAdapter.commitTransaction(em);
	}
}
