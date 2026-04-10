package org.apache.olingo.jpa.test.util;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.jdbc.DriverDataSource;


import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class DataSourceHelper {

  public enum DatabaseType {
    /**
     * In memory
     */
    H2,
    /**
     * In memory
     */
    HSQLDB,
    /**
     * In memory, not persistent variant!
     */
    DERBY,
    /**
     * @deprecated Currently not useable
     */
    @Deprecated
    REMOTE;
  }

  public static final int DB_H2 = 1;
  public static final int DB_HSQLDB = 2;
  public static final int DB_REMOTE = 3;
  public static final int DB_DERBY = 4;

  private static final String DB_SCHEMA = "OLINGO";

  private static final String H2_DRIVER_CLASS_NAME = "org.h2.Driver";

  private static final String HSQLDB_URL = "jdbc:hsqldb:mem:com.sample";
  private static final String HSQLDB_DRIVER_CLASS_NAME = "org.hsqldb.jdbc.JDBCDriver";

  private static final String DERBY_URL =
      "jdbc:derby:memory:target/testdb;create=true;traceFile=derby_trace.log;trace_level=0xFFFFFFFF";
  private static final String DERBY_DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";

  private static final String REMOTE_URL = "jdbc:$DBNAME$:$Host$:$Port$";

  private static int dbCounter = 0;

  /**
   * The next database connection will open a fresh database without content...
   * that means a migration + content fill is triggered
   */
  public static void forceFreshCreatedDatabase() {
    dbCounter++;
  }

  private static String build_H2_Url() {
    return "jdbc:h2:mem:test" + Integer.toString(dbCounter) + ";DB_CLOSE_DELAY=-1";
  }

  public static DataSource createDataSource(final DatabaseType database) {
    DriverDataSource ds = null;
    switch (database) {
    case H2:
      ds = new DriverDataSource(Thread.currentThread().getContextClassLoader(), H2_DRIVER_CLASS_NAME, build_H2_Url(),
          null, null);
      break;

    case HSQLDB:
      // HSQLDB does call LogManager.reset() and this will destroy our logging configuration
      System.setProperty("hsqldb.reconfig_logging", "false");
      ds = new DriverDataSource(Thread.currentThread().getContextClassLoader(), HSQLDB_DRIVER_CLASS_NAME, HSQLDB_URL,
          null, null);
      break;
    case DERBY:
      ds = new DriverDataSource(Thread.currentThread().getContextClassLoader(), DERBY_DRIVER_CLASS_NAME, DERBY_URL,
          null, null);
      break;

    case REMOTE:
      final String env = System.getenv().get("REMOTE_DB_LOGON");
      final ObjectMapper mapper = new ObjectMapper();
      ObjectNode hanaInfo;
      try {
        hanaInfo = (ObjectNode) mapper.readTree(env);
      } catch (final JacksonException e) {
        return null;
      }
      String url = REMOTE_URL;
      url = url.replace("$Host$", hanaInfo.get("hostname").asString());
      url = url.replace("$Port$", hanaInfo.get("port").asString());
      url = url.replace("$DBNAME$", hanaInfo.get("dbname").asString());
      final String driver = hanaInfo.get("driver").asString();
      ds = new DriverDataSource(Thread.currentThread().getContextClassLoader(), driver, url, hanaInfo.get("username")
          .asString(), hanaInfo.get(
              "password").asString());
      return ds;
    default:
      return null;
    }

    initializeDatabase(ds);
    return ds;
  }

  public static void initializeDatabase(final DataSource ds) {
    final Flyway flyway = Flyway.configure().dataSource(ds).schemas(DB_SCHEMA).baselineOnMigrate(true).cleanDisabled(
        false).load();
    flyway.clean();
    flyway.migrate();
  }
}
