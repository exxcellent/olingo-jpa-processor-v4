package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import javax.persistence.metamodel.Attribute.PersistentAttributeType;

import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * @deprecated This class exists only for {@link JPAAssociationPathImpl}.
 */
@Deprecated
abstract class AbstractNavigationProperty extends AbstractProperty<CsdlNavigationProperty> implements
JPAAssociationAttribute {

  public AbstractNavigationProperty(final JPAEdmNameBuilder nameBuilder, final String internalName) {
    super(nameBuilder, internalName);
  }

  abstract List<IntermediateJoinColumn> getSourceJoinColumns() throws ODataJPAModelException;

  abstract List<IntermediateJoinColumn> getTargetJoinColumns() throws ODataJPAModelException;

  abstract public boolean doesUseJoinTable();

  abstract PersistentAttributeType getJoinCardinality() throws ODataJPAModelException;
}
