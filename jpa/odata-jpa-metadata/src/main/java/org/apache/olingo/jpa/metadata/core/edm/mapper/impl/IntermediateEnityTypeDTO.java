package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * A DTO is mapped as OData entity!
 *
 * @author Ralf Zozmann
 *
 */
class IntermediateEnityTypeDTO extends AbstractStructuredTypeDTO<CsdlEntityType> implements JPAEntityType {

  private final Class<?> dtoType;
  private final String entitySetName;
  private CsdlEntityType edmEntityType = null;

  public IntermediateEnityTypeDTO(final JPAEdmNameBuilder nameBuilder, final Class<?> dtoType,
      final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
    super(determineDTONameBuilder(nameBuilder, dtoType), dtoType.getName(), determineAbstract(dtoType), false,
        serviceDocument);

    // DTO must have marker annotation
    final ODataDTO annotation = dtoType.getAnnotation(ODataDTO.class);
    if (annotation == null) {
      throw new ODataJPAModelException(MessageKeys.TYPE_NOT_SUPPORTED, dtoType.getName(), null);
    }

    this.dtoType = dtoType;
    this.setExternalName(getNameBuilder().buildDTOTypeName(dtoType));
    entitySetName = determineEntitySetName(dtoType);
  }

  private String determineEntitySetName(final Class<?> entityClass) {
    final ODataDTO dtoAnnotation = entityClass.getAnnotation(ODataDTO.class);
    if (dtoAnnotation == null || dtoAnnotation.edmEntitySetName() == null || dtoAnnotation.edmEntitySetName()
        .isEmpty()) {
      // default naming
      return getNameBuilder().buildEntitySetName(getExternalName());
    }
    // manual naming
    return dtoAnnotation.edmEntitySetName();
  }

  private static JPAEdmNameBuilder determineDTONameBuilder(final JPAEdmNameBuilder nameBuilderDefault,
      final Class<?> entityClass) {
    final ODataDTO dtoAnnotation = entityClass.getAnnotation(ODataDTO.class);
    if (dtoAnnotation == null || dtoAnnotation.attributeNaming() == null) {
      // nothing to change
      return nameBuilderDefault;
    }
    // prepare a custom name builder
    return new JPAEdmNameBuilder(nameBuilderDefault.getNamespace(), dtoAnnotation.attributeNaming());
  }

  @Override
  public String getEntitySetName() {
    return entitySetName;
  }

  private static boolean determineAbstract(final Class<?> dtoType) {
    final int modifiers = dtoType.getModifiers();
    return Modifier.isAbstract(modifiers);
  }

  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (edmEntityType != null) {
      return;
    }
    edmEntityType = new CsdlEntityType();
    edmEntityType.setName(getExternalName());
    edmEntityType.setProperties(getAttributes(true).stream().map(attribute -> attribute.getProperty()).collect(
        Collectors.toList()));
    edmEntityType.setNavigationProperties(getAssociations().stream().map(association -> association.getProperty())
        .collect(Collectors.toList()));
    edmEntityType.setKey(extractEdmKeyElements());
    edmEntityType.setAbstract(isAbstract());
    edmEntityType.setBaseType(determineBaseType());
    edmEntityType.setHasStream(determineHasStream());
    edmEntityType.setOpenType(isOpenType());
  }

  @Override
  protected AbstractStructuredType<?> getBaseType() throws ODataJPAModelException {
    final Class<?> baseType = dtoType.getSuperclass();
    if (baseType == null) {
      return null;
    }
    return (AbstractStructuredType<?>) getServiceDocument().getEntityType(baseType);
  }

  @Override
  public Class<?> getTypeClass() {
    return dtoType;
  }


  @Override
  public final DataAccessConditioner<?> getDataAccessConditioner() {
    return null;
  }

  @Override
  public final String getContentType() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final JPASelector getContentTypeAttributePath() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final List<JPAAttributePath> getKeyPath() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Class<?> getKeyType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final JPAAttributePath getStreamAttributePath() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean hasEtag() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean hasStream() throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final List<JPASelector> searchChildPath(final JPASelector selectItemPath) throws ODataJPAModelException {
    throw new UnsupportedOperationException();
  }

  @Override
  public final CsdlEntityType getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmEntityType;
  }

  @Override
  protected Collection<Field> getPropertyFields() {
    return Arrays.asList(dtoType.getDeclaredFields());
  }
}
