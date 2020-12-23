package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class AbstractIntermediateComplexTypeDTO extends AbstractStructuredTypeDTO<CsdlComplexType> implements
    JPAComplexType {

  public AbstractIntermediateComplexTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName,
      final boolean isAbstract,
      final boolean isOpenType) throws ODataJPAModelException {
    super(nameBuilder, typeName, isAbstract, isOpenType);
  }

}
