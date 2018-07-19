package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.olingo.commons.api.ex.ODataException;
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
	// JPA_HSQLDB_DatabaseProcessor(),
	// DataSourceHelper.createDataSource(DataSourceHelper.DB_HSQLDB));
	// emf = persistenceAdapter.getEMF();
	//
	// }

	@Ignore // TODO check is path is in general allowed
	@Test
	public void testNavigationAfterFunctionNotAllowed() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Siblings(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')/Parent");
		helper.assertStatus(501);
	}

	@Test
	public void testFunctionGenerateQueryString() throws IOException, ODataException, SQLException {

		createSiblingsFunction();
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Siblings(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')");
		helper.assertStatus(200);
		assertTrue(helper.getValues().size() > 0);
	}

	private void createSiblingsFunction() {
		final StringBuffer sqlString = new StringBuffer();

		final EntityManager em = persistenceAdapter.createEntityManager();
		final EntityTransaction t = em.getTransaction();

		sqlString.append("create function \"OLINGO\".\"org.apache.olingo.jpa::Siblings\""); // \"OLINGO\".
		sqlString.append("( CodePublisher nvarchar(10), CodeID nvarchar(10), DivisionCode nvarchar(10))");
		sqlString.append(
				"RETURNS TABLE (\"CodePublisher\" nvarchar(10), \"CodeID\" nvarchar(10), \"DivisionCode\" nvarchar(10),");
		sqlString.append(
				"\"CountryISOCode\"  NVARCHAR(4), \"ParentCodeID\"  NVARCHAR(10), \"ParentDivisionCode\"  NVARCHAR(10),");
		sqlString.append("\"AlternativeCode\"  NVARCHAR(10),  \"Area\"  DECIMAL(34,0), \"Population\"  BIGINT )");
		sqlString.append("READS SQL  DATA RETURN TABLE (SELECT ");
		sqlString.append("a.\"CodePublisher\", a.\"CodeID\", a.\"DivisionCode\", a.\"CountryISOCode\",a.\"ParentCodeID\"");
		sqlString.append(",a.\"ParentDivisionCode\", a.\"AlternativeCode\",a.\"Area\", a.\"Population\"");
		sqlString.append("FROM \"OLINGO\".\"org.apache.olingo.jpa::AdministrativeDivision\" as a);");

		t.begin();
		final javax.persistence.Query q = em.createNativeQuery(sqlString.toString());
		q.executeUpdate();
		t.commit();
	}
}
