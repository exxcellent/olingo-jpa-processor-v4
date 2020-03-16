package org.apache.olingo.jpa.processor.transformation.excel;

import static org.junit.Assert.assertTrue;

import java.io.FileOutputStream;
import java.io.IOException;

import javax.persistence.Entity;

import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.JPAODataGlobalContext;
import org.apache.olingo.jpa.processor.JPAODataRequestContext;
import org.apache.olingo.jpa.processor.core.api.JPAODataServletHandler;
import org.apache.olingo.jpa.processor.core.query.result.QueryEntityResult;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.transformation.TransformationContextRequirement;
import org.apache.olingo.jpa.processor.transformation.TransformationDeclaration;
import org.apache.olingo.jpa.processor.transformation.impl.ODataResponseContent;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.serializer.RepresentationType;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.junit.Test;

public class ExcelTransformationTest extends TestBase {

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
  }

  @Test
  public void testSuccessfulFewerColumnsExport() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.setCreateHeaderRow(true);

    final URIBuilder uriBuilder = newUriBuilder().appendEntitySetSegment("Organizations").select("Name1", "Country",
        "Address");
    final ServerCallSimulator helper = prepareServerCallHelper(uriBuilder, configuration);
    helper.execute(HttpStatusCode.OK.getStatusCode());
    final byte[] data = helper.getBinaryResult();

    assertTrue(data.length > 1000);

    final FileOutputStream file = new FileOutputStream("target/test-selected.xlsx");
    file.write(data);
    file.flush();
    file.close();
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

  private ServerCallSimulator prepareServerCallHelper(final URIBuilder uriBuilder, final Configuration configuration)
      throws IOException, ODataException {
    final ContentType contentTypeExcel = ContentType.create(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, uriBuilder) {

      @Override
      protected JPAODataServletHandler createServletHandler() throws ODataException {
        final JPAODataServletHandler handler = super.createServletHandler();
        // register Excel as output format
        final TransformationDeclaration<QueryEntityResult, ODataResponseContent> declaration =
            new TransformationDeclaration<>(
                QueryEntityResult.class, ODataResponseContent.class, new TransformationContextRequirement(
                    JPAODataGlobalContext.class), new TransformationContextRequirement(
                        JPAODataRequestContext.class), new TransformationContextRequirement(
                            UriInfoResource.class), new TransformationContextRequirement(
                                ODataRequest.class), new TransformationContextRequirement(
                                    RepresentationType.class, RepresentationType.COLLECTION_ENTITY),
                new TransformationContextRequirement(
                    ContentType.class, contentTypeExcel));

        handler.activateCustomResponseTransformation(declaration,
            QueryEntityResult2ExcelODataResponseContentTransformation.class, contentTypeExcel,
            RepresentationType.COLLECTION_ENTITY);

        return handler;
      }

      @Override
      protected void prepareRequestContext(final JPAODataRequestContext requestContext) {
        super.prepareRequestContext(requestContext);
        requestContext.getDependencyInjector().registerDependencyMapping(Configuration.class, configuration);
      }

    };
    // request excel format as response
    helper.setRequestedResponseContentType(contentTypeExcel.toContentTypeString());

    return helper;
  }
}
