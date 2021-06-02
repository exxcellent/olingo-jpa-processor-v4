package org.apache.olingo.jpa.servlet.springboot.bean.example;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler;
import org.apache.olingo.jpa.processor.core.database.JPA_DERBYDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.mapping.AbstractJPAAdapter;
import org.apache.olingo.jpa.processor.core.mapping.ResourceLocalPersistenceAdapter;
import org.apache.olingo.jpa.test.util.AbstractTest;
import org.apache.olingo.jpa.test.util.DataSourceHelper;

/**
 * Example call: http://localhost:8080/odata/$metadata
 *
 * @author Ralf Zozmann
 *
 */
@WebServlet(name = "odata-servlet", loadOnStartup = 1, urlPatterns = { "/odata/*" })
public class ODataSpringBootServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private JPAODataServletHandler requestHandler = null;

  @Override
  public void init() throws ServletException {
    super.init();
    // use in memory Derby database
    try {
      final Map<String, Object> emProperties = AbstractTest.buildEntityManagerFactoryProperties(
          DataSourceHelper.DatabaseType.DERBY);
      final AbstractJPAAdapter mappingAdapter = new ResourceLocalPersistenceAdapter(
          org.apache.olingo.jpa.test.util.Constant.PUNIT_NAME, emProperties, new JPA_DERBYDatabaseProcessor());

      requestHandler = new JPAODataServletHandler(mappingAdapter);
    } catch (final ODataException e) {
      throw new ServletException("Initialization of request handler failed", e);
    }

    log("oData endpoint prepared");
  }

  @Override
  protected void service(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {
    requestHandler.process(req, resp);
  }

}
