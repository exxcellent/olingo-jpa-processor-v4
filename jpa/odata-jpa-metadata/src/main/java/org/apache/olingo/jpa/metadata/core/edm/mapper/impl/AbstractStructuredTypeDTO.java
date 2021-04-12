package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.Transient;

import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
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

  private final IntermediateServiceDocument serviceDocument;
  private final boolean isAbstract;
  private final boolean isOpenType;

  /**
   *
   * @param typeName The (full qualified) internal name.
   */
  public AbstractStructuredTypeDTO(final JPAEdmNameBuilder nameBuilder, final String typeName, final boolean isAbstract,
      final boolean isOpenType, final IntermediateServiceDocument serviceDocument)
          throws ODataJPAModelException {
    super(nameBuilder, typeName);
    this.serviceDocument = serviceDocument;
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

  final protected IntermediateServiceDocument getServiceDocument() {
    return serviceDocument;
  }

  @Override
  final protected List<IntermediateJoinColumn> determineJoinColumns(final JPAAttribute<?> property,
      final JPAAssociationPath association) {
    throw new UnsupportedOperationException();
  }

  @Override
  final protected String determineDBFieldName(final JPAMemberAttribute property,
      final JPAAttributePath jpaPath) {
    // simply the default (will have no effect for DTO)
    return jpaPath.getDBFieldName();
  }

  /**
   *
   * @return The collection of fields used as source for property meta creation.
   */
  abstract protected Collection<Field> getPropertyFields();

  @Override
  final protected void buildPropertyList() throws ODataJPAModelException {

    for (final Field field : getPropertyFields()) {
      if (field.isAnnotationPresent(Transient.class)) {
        continue;
      }
      if (field.isAnnotationPresent(Inject.class)) {
        continue;
      } else if (field.isAnnotationPresent(EdmIgnore.class)) {
        continue;
      } else if (TypeMapping.isTargetingDTO(field)) {
        final IntermediateNavigationDTOProperty property = new IntermediateNavigationDTOProperty(getNameBuilder(),
            field,
            serviceDocument);
        addNavigationProperty(property);
        continue;
      } else if (field.isSynthetic()) {
        // JaCoCo will create synthetic member '$jacocoData' while class file instrumentation for coverage report
        // so we ignore synthetic members always...
        LOG.log(Level.FINE, "Synthetic member '" + field.getDeclaringClass().getSimpleName() + "#" + field.getName()
        + "' is ignored");
        continue;
      } else {
        // assume to be primitive or complex
        final IntermediatePropertyDTOField property = new IntermediatePropertyDTOField(getNameBuilder(), field,
            serviceDocument);
        addSimpleProperty(property);
        continue;
      }
    }
  }

}
