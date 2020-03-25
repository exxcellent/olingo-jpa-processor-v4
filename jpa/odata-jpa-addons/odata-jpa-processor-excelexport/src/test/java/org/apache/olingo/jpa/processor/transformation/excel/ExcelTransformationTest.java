package org.apache.olingo.jpa.processor.transformation.excel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.ModifiableJPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.AbstractTest;
import org.apache.olingo.jpa.test.util.AbstractTest.EntityManagerProperty;
import org.apache.olingo.jpa.test.util.AbstractTest.JPAProvider;
import org.apache.olingo.jpa.test.util.Constant;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.junit.Before;
import org.junit.Test;

public class ExcelTransformationTest extends TestBase {

  @Before
  public void recreateDatabase() {
    DataSourceHelper.forceFreshCreatedDatabase();
  }

  /**
   * Create own adapter instance with disabled SQL logging
   */
  @Override
  protected TestGenericJPAPersistenceAdapter createPersistenceAdapter() {
    return new TestGenericJPAPersistenceAdapter(Constant.PUNIT_NAME, buildTestEntityManagerFactoryProperties(
        DataSourceHelper.DatabaseType.H2), TestGenericJPAPersistenceAdapter
        .determineDatabaseProcessor(DataSourceHelper.DatabaseType.H2));
  }

  public static Map<String, Object> buildTestEntityManagerFactoryProperties(final DataSourceHelper.DatabaseType dbType,
      final EntityManagerProperty... additionalProperties) {
    final Map<String, Object> map = AbstractTest.buildEntityManagerFactoryProperties(dbType);
    map.put("eclipselink.logging.level.sql", "INFO");
    return map;
  }

  @Test
  public void testSuccessfulFullExport() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.assignSheetName(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "Demo");
    configuration.setCreateHeaderRow(true);
    configuration.setFormatDate("DD.MM.yy");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, configuration);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final byte[] data = helper.getBinaryResult();

    assertTrue(data.length > 1000);

    final FileOutputStream file = new FileOutputStream("target/test-full.xlsx");
    file.write(data);
    file.flush();
    file.close();

    final TestInspector validator = new TestInspector(configuration, data);
    assertEquals(17, validator.determineNumberOfColumns("Demo"));
    assertTrue(validator.determineNumberOfRows("Demo") > 2);// 2+header
    final DecimalFormat df = new DecimalFormat("#.00000#", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    assertEquals("98989898.34678", df.format(validator.determineCellValueAsNumber("Demo", 2, "ADecimal")));
  }

  @Test
  public void testSuccessfulFewerColumnsExport() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.setCreateHeaderRow(true);
    // suppress auto selected ID column
    configuration.addSuppressedColumns(Organization.class.getAnnotation(Entity.class).name(), "ID");
    configuration.assignColumnName(Organization.class.getAnnotation(Entity.class).name(), "Name1", "Organization name");
    configuration.assignColumnName(Organization.class.getAnnotation(Entity.class).name(), "Address/Country",
        "Location - Country");
    // force table data start to column 10
    configuration.assignColumnIndex(Organization.class.getAnnotation(Entity.class).name(), "Country", 10);

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("Name1", "Country",
        "Address");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, configuration);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final byte[] data = helper.getBinaryResult();

    assertTrue(data.length > 1000);

    //    final FileOutputStream file = new FileOutputStream("target/test-selected.xlsx");
    //    file.write(data);
    //    file.flush();
    //    file.close();

    final TestInspector validator = new TestInspector(configuration, data);
    assertEquals(10, validator.determineColumnIndex("Organization", "Country"));
    assertTrue(validator.determineColumnIndex("Organization", "Address/CityName") > 10);
    assertTrue(validator.determineColumnIndex("Organization", "Organization name") > 10);
    assertFalse(validator.hasColumnOfName("Organization", "Name1"));// renamed
    assertFalse(validator.hasColumnOfName("Organization", "Name2"));
    assertFalse(validator.hasColumnOfName("Organization", "ID"));
    assertEquals(9, validator.determineNumberOfColumns("Organization"));
  }

  @Test
  public void testColumnOrdering() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.setCreateHeaderRow(true);
    configuration.assignColumnOrder(Organization.class.getAnnotation(Entity.class).name(), "Country", "Name1", "Name2");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, configuration);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final byte[] data = helper.getBinaryResult();

    assertTrue(data.length > 1000);

    //    final FileOutputStream file = new FileOutputStream("target/test-columnordering.xlsx");
    //    file.write(data);
    //    file.flush();
    //    file.close();

    final TestInspector validator = new TestInspector(configuration, data);
    assertEquals(0, validator.determineColumnIndex("Organization", "Country"));
    assertEquals(2, validator.determineColumnIndex("Organization", "Name2"));
    assertTrue(validator.determineColumnIndex("Organization", "ID") > 2);
  }

  @Test
  public void testExportUsingDefaultConfiguration() throws IOException, ODataException {
    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, null);
    helper.execute(HttpStatusCode.OK.getStatusCode());
  }

  @Test
  public void testIllegalExpandExport() throws IOException, ODataException {

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").expand("Locations");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, null);
    helper.execute(HttpStatusCode.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void testLoad() throws IOException, ODataException {

    assumeTrue("We do not execute thit long term test with all JPA providers...",
        getJPAProvider() == JPAProvider.EclipseLink);

    final long beforeData = System.currentTimeMillis();

    //    final int numberdata = 1048570;// excel row number limit
    final int numberdata = 250000;
    createData(numberdata);

    final long beforeQuery = System.currentTimeMillis();

    final Configuration configuration = new Configuration();
    configuration.assignSheetName(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "Demo");

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("DatatypeConversionEntities");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, configuration);
    helper.execute(HttpStatusCode.OK.getStatusCode());

    final long afterQuery = System.currentTimeMillis();

    final byte[] data = helper.getBinaryResult();

    final long afterPOI = System.currentTimeMillis();

    assertTrue(data.length > 1000);

    final FileOutputStream file = new FileOutputStream("target/test-load.xlsx");
    file.write(data);
    file.flush();
    file.close();

    final long afterFileWrite = System.currentTimeMillis();

    System.out.println("Duration: data creation(" + numberdata + ")=" + (beforeQuery - beforeData) / 1000
        + "sec, query=" + (afterQuery - beforeQuery) / 1000 + "sec, streaming=" + (afterPOI - afterQuery)
        + "ms, file write=" + (afterFileWrite - afterPOI) + "ms");
  }

  private void createData(final int number) {
    final EntityManager em = persistenceAdapter.getEMF().createEntityManager();
    final EntityTransaction t = em.getTransaction();
    t.begin();

    for (int i = 0; i < number; i++) {
      final String sql =
          "insert into \"OLINGO\".\"org.apache.olingo.jpa::DatatypeConversionEntity\" values( " + Integer.toString(i
              + 10)
          + ", '0610-01-01', null, null, '09:21:00','2010-01-01 09:21:00', '2016-01-20 09:21:23', -12.34, 'http://www.anywhere.org/image.jpg', 123, 1900, 'CE', 0, 'Two', '"
          + UUID.randomUUID() + "', 1, FALSE);";
      final Query q = em.createNativeQuery(sql);
      q.executeUpdate();
    }
    t.commit();
  }

  private ServerCallSimulator prepareServerCallHelper(final URIBuilder uriBuilder, final Configuration configuration)
      throws IOException, ODataException {
    final ContentType contentTypeExcel = ContentType.create(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {

      @Override
      protected JPAODataServletHandler createServletHandler() throws ODataException {
        final JPAODataServletHandler handler = super.createServletHandler();
        // register Excel as output format
        handler.activateCustomResponseTransformation(
            QueryEntityResult2ExcelODataResponseContentTransformation.DEFAULT_DECLARATION,
            QueryEntityResult2ExcelODataResponseContentTransformation.class,
            QueryEntityResult2ExcelODataResponseContentTransformation.CONTENTTYPE_EXCEL,
            RepresentationType.COLLECTION_ENTITY);

        return handler;
      }

      @Override
      protected void prepareRequestContext(final ModifiableJPAODataRequestContext requestContext) {
        super.prepareRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(Configuration.class, configuration);
      }

    };
    // request excel format as response
    helper.setRequestedResponseContentType(contentTypeExcel.toContentTypeString());

    return helper;
  }
}
