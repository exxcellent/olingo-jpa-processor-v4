package org.apache.olingo.jpa.processor.transformation.excel;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import javax.persistence.Entity;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigurationTest extends TestBase {

  @Rule
  public ExpectedException thrown = ExpectedException.none(); // initially, expect no exception

  @Test
  public void testDuplicateColumnIndex() throws IOException, ODataException {

    final Configuration configuration = new Configuration();
    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C1", 2);

    thrown.expect(IllegalArgumentException.class);

    configuration.assignColumnIndex(DatatypeConversionEntity.class.getAnnotation(Entity.class).name(), "C2", 2);
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

}
