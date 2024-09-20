package org.apache.olingo.jpa.metadata.core.edm.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.processor.core.testmodel.DatatypeConversionEntity;
import org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner.AbstractGenericBusinessPartner;
import org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner.GenericBusinessPartner;
import org.apache.olingo.jpa.processor.core.testmodel.dataaccessconditioner.GenericBusinessPartnerDataAccessConditioner;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestGenericJPAPersistenceAdapter;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.jupiter.api.Test;

public class TestAnnotations extends TestBase {

  @Test
  public void testDefaultConverterPresence() throws NoSuchFieldException, SecurityException, InstantiationException,
  IllegalAccessException {
    final EdmAttributeConversion anno = DatatypeConversionEntity.class.getDeclaredField("aTime1").getAnnotation(
        EdmAttributeConversion.class);
    assertNotNull(anno);
    assertEquals(EdmPrimitiveTypeKind.TimeOfDay, anno.odataType());
    assertEquals(EdmAttributeConversion.DEFAULT.class, anno.converter());

    assertThrows(UnsupportedOperationException.class, () -> anno.converter().newInstance().convertToJPA(null));

  }

  @Test
  public void testDataAccessConditionerPresence() throws ODataException {

    final String namespace = "DataAccessConditioner";
    final TestGenericJPAPersistenceAdapter otherPersistenceAdapter = new TestGenericJPAPersistenceAdapter(namespace,
        DataSourceHelper.DatabaseType.H2);
    final JPAEdmProvider otherEdmProvider = new JPAEdmProvider(namespace, otherPersistenceAdapter.getMetamodel());

    final JPAEntityType type = otherEdmProvider.getServiceDocument().getEntityType("GenericBusinessPartners");
    final DataAccessConditioner<?> dac = type.getDataAccessConditioner();
    assertEquals(GenericBusinessPartnerDataAccessConditioner.class, dac.getClass());
    AbstractGenericBusinessPartner.class.getAnnotation(ODataEntity.class);
    final ODataEntity anno = GenericBusinessPartner.class.getAnnotation(ODataEntity.class);
    assertNotNull(anno, "@Inherited annotation must not be null");
    assertEquals(GenericBusinessPartnerDataAccessConditioner.class, anno.handlerDataAccessConditioner());
  }

}
