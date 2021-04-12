package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

class IntermediateComplexTypeDTO extends AbstractIntermediateComplexTypeDTO {

  private final Class<?> cType;

  public IntermediateComplexTypeDTO(final JPAEdmNameBuilder nameBuilder, final Class<?> type,
      final IntermediateServiceDocument serviceDocument)
          throws ODataJPAModelException {
    super(determineComplexTypeNameBuilder(nameBuilder, type), type.getName(), false, false, serviceDocument);
    this.cType = type;
    this.setExternalName(getNameBuilder().buildComplexTypeName(type));
  }

  @Override
  public Class<?> getTypeClass() {
    return cType;
  }

  @Override
  protected JPAStructuredType getBaseType() throws ODataJPAModelException {
    final Class<?> baseType = cType.getSuperclass();
    if (baseType == null) {
      return null;
    }
    return getServiceDocument().getComplexType(baseType);
  }

  @Override
  protected void enrichCsdlComplexType(final CsdlComplexType justCreatedCdslType) {
    // do nothing
  }

  @Override
  protected Collection<Field> getPropertyFields() {
    return Arrays.asList(cType.getDeclaredFields());
  }
}