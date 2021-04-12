package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class AbstractIntermediateComplexTypeDTO extends AbstractStructuredTypeDTO<CsdlComplexType> implements
JPAComplexType {

  private CsdlComplexType edmComplexType;

  public AbstractIntermediateComplexTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName,
      final boolean isAbstract,
      final boolean isOpenType, final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
    super(nameBuilder, typeName, isAbstract, isOpenType, serviceDocument);
  }

  @Override
  final CsdlComplexType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmComplexType;
  }

  @Override
  final protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (edmComplexType == null) {
      edmComplexType = new CsdlComplexType();

      edmComplexType.setName(this.getExternalName());
      edmComplexType.setProperties(getAttributes(true).stream().map(attribute -> attribute.getProperty()).collect(
          Collectors
          .toList()));
      edmComplexType.setNavigationProperties(getAssociations().stream().map(association -> association.getProperty())
          .collect(Collectors.toList()));
      edmComplexType.setBaseType(determineBaseType());
      edmComplexType.setAbstract(isAbstract());
      edmComplexType.setOpenType(isOpenType());

      enrichCsdlComplexType(edmComplexType);

      if (determineHasStream()) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_EMBEDDED_STREAM,
            getInternalName());
      }
    }
  }

  /**
   * Hook method to enrich the created CDSL complex type with more data.
   *
   * @param justCreatedCdslType The just created type.
   */
  abstract protected void enrichCsdlComplexType(CsdlComplexType justCreatedCdslType);

}
