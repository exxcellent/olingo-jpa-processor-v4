package org.apache.olingo.jpa.processor.transformation.excel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Map;

import javax.persistence.Entity;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class ConfigurationTest extends TestBase {

  @Test
  public void testDuplicateColumnIndex() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);

    final ThrowingRunnable throwingRunnable = () -> configuration.assignColumnIndex(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), "C2", 2);
    assertThrows(IllegalArgumentException.class, throwingRunnable);
  }

  @Test
  public void testColumnIndex() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C2", 3);

    final JPAEntityType et = helper.getJPAEntityType("DatatypeConversionEntities");
    final Map<String, Integer> map = configuration.getCustomColumnIndexes(et);
    assertEquals(2, map.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormatDate() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.setFormatDate("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormatDecimal() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.setFormatDecimal("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormatInteger() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.setFormatInteger("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormatTime() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.setFormatTime("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormatDateTime() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.setFormatDateTime("");
  }

  @Test
  public void testSuppressedColumns() throws IOException, ODataException {
    final Configuration configuration = new Configuration();

    ThrowingRunnable throwingRunnable = () -> configuration.addSuppressedColumns(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), new String[0]);
    assertThrows(IllegalArgumentException.class, throwingRunnable);

    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);
    throwingRunnable = () -> configuration.addSuppressedColumns(DatatypeConversionEntity.class.getAnnotation(
        Entity.class).name(), "C1");
    assertThrows(IllegalArgumentException.class, throwingRunnable);
  }
}
