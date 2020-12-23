package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * @author Ralf Zozmann
 *
 */
abstract class AbstractStructuredTypeDTO<CsdlType extends CsdlStructuralType> extends AbstractStructuredType<CsdlType> {

  private final boolean isAbstract;
  private final boolean isOpenType;

  public AbstractStructuredTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName, final boolean isAbstract,
      final boolean isOpenType)
          throws ODataJPAModelException {
    super(nameBuilder, typeName);

    this.setExternalName(typeName);
    this.isAbstract = isAbstract;
    this.isOpenType = isOpenType;
  }

  @Override
  public final boolean isAbstract() {
    return isAbstract;
  }

  @Override
  final public boolean isOpenType() {
    return isOpenType;
  }

  @Override
  final protected List<IntermediateJoinColumn> determineJoinColumns(final JPAAttribute<?> property,
      final JPAAssociationPath association) {
    throw new UnsupportedOperationException();
  }

  @Override
  final protected String determineDBFieldName(final JPAMemberAttribute property,
      final JPAAttributePath jpaPath) {
    throw new UnsupportedOperationException();
  }

}
