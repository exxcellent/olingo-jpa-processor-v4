package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.metamodel.EntityType;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397974"
 * >OData Version 4.0 Part 3 - 8 Entity Type</a>
 *
 * @author Oliver Grande
 *
 */
class IntermediateEntityTypeJPA extends AbstractStructuredTypeJPA<EntityType<?>, CsdlEntityType> implements
JPAEntityType {

  private CsdlEntityType csdlEntityType;
  private boolean hasEtag = false;
  private final DataAccessConditioner<?> dac;
  private final String entitySetName;

  IntermediateEntityTypeJPA(final JPAEdmNameBuilder nameBuilder, final EntityType<?> et,
      final IntermediateServiceDocument serviceDocument)
          throws ODataJPAModelException {
    super(determineEntityNameBuilder(nameBuilder, et.getJavaType()), et, serviceDocument);
    this.setExternalName(getNameBuilder().buildEntityTypeName(et));
    final EdmIgnore jpaIgnore = ((AnnotatedElement) et.getJavaType()).getAnnotation(EdmIgnore.class);
    if (jpaIgnore != null) {
      this.setIgnore(true);
    }
    dac = buildDataAccessConditionerInstance(et.getJavaType());
    entitySetName = determineEntitySetName(et.getJavaType());
  }

  private String determineEntitySetName(final Class<?> entityClass) {
    final ODataEntity entityAnnotation = entityClass.getAnnotation(ODataEntity.class);
    if (entityAnnotation == null || entityAnnotation.edmEntitySetName() == null || entityAnnotation.edmEntitySetName()
        .isEmpty()) {
      // default naming
      return getNameBuilder().buildEntitySetName(getExternalName());
    }
    // manual naming
    return entityAnnotation.edmEntitySetName();
  }

  private static JPAEdmNameBuilder determineEntityNameBuilder(final JPAEdmNameBuilder nameBuilderDefault,
      final Class<?> entityClass) {
    final ODataEntity entityAnnotation = entityClass.getAnnotation(ODataEntity.class);
    if (entityAnnotation == null || entityAnnotation.attributeNaming() == null) {
      // nothing to change
      return nameBuilderDefault;
    }
    // prepare a custom name builder
    return new JPAEdmNameBuilder(nameBuilderDefault.getNamespace(), entityAnnotation.attributeNaming());
  }

  private DataAccessConditioner<?> buildDataAccessConditionerInstance(final Class<?> entityClass) throws ODataJPAModelException {
    final ODataEntity entityAnnotation = entityClass.getAnnotation(ODataEntity.class);
    if (entityAnnotation == null) {
      return null;
    }
    final Class<? extends DataAccessConditioner<?>> handlerClass = entityAnnotation.handlerDataAccessConditioner();
    if (handlerClass == null || ODataEntity.DEFAULT.class.equals(handlerClass)) {
      return null;
    }
    try {
      return handlerClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INNER_EXCEPTION, e);
    }
  }

  @Override
  public String getEntitySetName() {
    return entitySetName;
  }

  @Override
  public DataAccessConditioner<?> getDataAccessConditioner() {
    return dac;
  }

  @Override
  public String getContentType() throws ODataJPAModelException {
    final IntermediateProperty stream = (IntermediateProperty) getStreamProperty();
    return stream.getContentType();
  }

  @Override
  public JPAAttributePath getStreamAttributePath() throws ODataJPAModelException {
    return (JPAAttributePath) getPath(getStreamProperty().getExternalName());
  }

  @Override
  public JPASelector getContentTypeAttributePath() throws ODataJPAModelException {
    final String propertyInternalName = ((IntermediateProperty) getStreamProperty()).getContentTypeProperty();
    if (propertyInternalName == null || propertyInternalName.isEmpty()) {
      return null;
    }
    // Ensure that @Ignore is ignored, because we have to find/select that path independent from business requirements
    return getPath(getAttribute(propertyInternalName).getExternalName());
  }

  @Override
  public boolean hasStream() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return this.determineHasStream();
  }

  @Override
  public boolean hasEtag() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return hasEtag;
  };

  @Override
  public List<JPASelector> searchChildPath(final JPASelector selectItemPath) throws ODataJPAModelException {
    final List<JPASelector> result = new ArrayList<JPASelector>();

    for (final JPASelector p : getPathList()) {
      if (p.getAlias().startsWith(selectItemPath.getAlias())) {
        result.add(p);
      }
    }
    return result;
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (csdlEntityType == null) {
      determineHasEtag();
      csdlEntityType = new CsdlEntityType();
      csdlEntityType.setName(getExternalName());
      // Skip Streams ... why?
      csdlEntityType.setProperties(getAttributes(true).stream().filter(attribute -> !IntermediateProperty.class
          .isInstance(
              attribute) || !IntermediateProperty.class.cast(attribute).isStream()).map(attribute -> attribute
                  .getProperty()).collect(Collectors
                      .toList()));
      csdlEntityType.setNavigationProperties(getAssociations().stream().map(association -> association.getProperty())
          .collect(Collectors.toList()));
      csdlEntityType.setKey(extractEdmKeyElements());
      csdlEntityType.setAbstract(isAbstract());
      csdlEntityType.setBaseType(determineBaseType());
      csdlEntityType.setHasStream(determineHasStream());
      csdlEntityType.setOpenType(isOpenType());
    }
  }

  private void determineHasEtag() throws ODataJPAModelException {
    for (final JPAMemberAttribute attribute : getAllAttributes(true)) {
      if (attribute.isEtag()) {
        hasEtag = true;
      }
    }
  }

  @Override
  public CsdlEntityType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return csdlEntityType;
  }

}
