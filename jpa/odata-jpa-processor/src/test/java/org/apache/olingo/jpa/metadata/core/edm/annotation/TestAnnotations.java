package org.apache.olingo.jpa.metadata.core.edm.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestAnnotations extends TestBase {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testDefaultConverterPresence() throws NoSuchFieldException, SecurityException, InstantiationException,
  IllegalAccessException {
    final EdmAttributeConversion anno = DatatypeConversionEntity.class.getDeclaredField("aTime1").getAnnotation(
        EdmAttributeConversion.class);
    assertNotNull(anno);
    assertEquals(EdmPrimitiveTypeKind.TimeOfDay, anno.odataType());
    assertEquals(EdmAttributeConversion.DEFAULT.class, anno.converter());

    thrown.expect(UnsupportedOperationException.class);
    anno.converter().newInstance().convertToJPA(null);

  }

}
