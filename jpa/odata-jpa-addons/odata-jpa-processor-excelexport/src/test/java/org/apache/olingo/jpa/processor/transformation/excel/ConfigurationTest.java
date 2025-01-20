package org.apache.olingo.jpa.processor.transformation.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Map;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;

public class ConfigurationTest extends TestBase {

  @Test
  public void testDuplicateColumnIndex1() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);

    assertThrows(IllegalArgumentException.class, () -> configuration.assignColumnIndex(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), "C2", 2));
  }

  @Test
  public void testDuplicateColumnIndex2() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);

    assertThrows(IllegalArgumentException.class, () -> configuration.assignColumnOrder(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), "C2", "C3", "C4"));
  }

  @Test
  public void testColumnIndexForSuppressedColumn() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    configuration.addSuppressedColumns(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C2");

    assertThrows(IllegalArgumentException.class, () -> configuration.assignColumnOrder(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), "C1", "C2"));
  }

  @Test
  public void testColumnIndex() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C2", 3);

    final JPAEntityType<?> et = helper.getJPAEntityType("DatatypeConversionEntities");
    final Map<String, Integer> map = configuration.getCustomColumnIndexes(et);
    assertEquals(2, map.size());
  }

  @Test
  public void testInvalidFormatDate() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    assertThrows(IllegalArgumentException.class, () -> configuration.setFormatDate(""));
  }

  @Test
  public void testInvalidFormatDecimal() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    assertThrows(IllegalArgumentException.class, () -> configuration.setFormatDecimal(""));
  }

  @Test
  public void testInvalidFormatInteger() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    assertThrows(IllegalArgumentException.class, () -> configuration.setFormatInteger(""));
  }

  @Test
  public void testInvalidFormatTime() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    assertThrows(IllegalArgumentException.class, () -> configuration.setFormatTime(""));
  }

  @Test
  public void testInvalidFormatDateTime() throws IOException, ODataException {
    final Configuration configuration = new Configuration();
    assertThrows(IllegalArgumentException.class, () -> configuration.setFormatDateTime(""));
  }

  @Test
  public void testSuppressedColumns() throws IOException, ODataException {
    final Configuration configuration = new Configuration();

    assertThrows(IllegalArgumentException.class, () -> configuration.addSuppressedColumns(DatatypeConversionEntity.class
        .getAnnotation(Entity.class).name(), new String[0]));

    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);
    assertThrows(IllegalArgumentException.class, () -> configuration.addSuppressedColumns(DatatypeConversionEntity.class
        .getAnnotation(
            Entity.class).name(), "C1"));
  }
}
