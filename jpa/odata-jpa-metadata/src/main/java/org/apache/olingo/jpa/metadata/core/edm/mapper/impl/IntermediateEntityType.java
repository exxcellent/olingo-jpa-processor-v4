package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.Type;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.entity.DataAccessConditioner;
import org.apache.olingo.jpa.metadata.core.edm.entity.ODataEntity;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397974"
 * >OData Version 4.0 Part 3 - 8 Entity Type</a>
 *
 * @author Oliver Grande
 *
 */
class IntermediateEntityType extends IntermediateStructuredType<CsdlEntityType> implements JPAEntityType {

  private CsdlEntityType edmEntityType;
  private boolean hasEtag = false;
  private final DataAccessConditioner<?> dac;
  private final String entitySetName;

  IntermediateEntityType(final JPAEdmNameBuilder nameBuilder, final EntityType<?> et,
      final IntermediateServiceDocument serviceDocument)
          throws ODataJPAModelException {
    super(determineEntityNameBuilder(nameBuilder, et.getJavaType()), et, serviceDocument);
    this.setExternalName(getNameBuilder().buildEntityTypeName(et));
    final EdmIgnore jpaIgnore = ((AnnotatedElement) this.jpaManagedType.getJavaType()).getAnnotation(EdmIgnore.class);
    if (jpaIgnore != null) {
      this.setIgnore(true);
    }
    dac = buildDataAccessConditionerInstance(this.jpaManagedType.getJavaType());
    entitySetName = determineEntitySetName(this.jpaManagedType.getJavaType());
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
      // search also super classes for the annotation...
      final Class<?> superClass = entityClass.getSuperclass();
      if (superClass == null || superClass == Object.class) {
        return null;
      }
      return buildDataAccessConditionerInstance(superClass);
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
    final IntermediateProperty stream = getStreamProperty();
    return stream.getContentType();
  }

  @Override
  public List<JPAAttributePath> getKeyPath() throws ODataJPAModelException {
    initializeType();

    final List<JPAAttributePath> result = new ArrayList<JPAAttributePath>();
    for (final String internalName : this.declaredPropertiesList.keySet()) {
      final JPAAttribute<?> attribute = this.declaredPropertiesList.get(internalName);
      if (attribute instanceof IntermediateEmbeddedIdProperty) {
        result.add(complexAttributePathMap.get(attribute.getExternalName()));
      } else if (attribute.isKey()) {
        result.add(simpleAttributePathMap.get(attribute.getExternalName()));
      }
    }
    final JPAStructuredType baseType = getBaseType();
    if (baseType != null) {
      result.addAll(((IntermediateEntityType) baseType).getKeyPath());
    }
    return result;
  }

  @Override
  public Class<?> getKeyType() {
    if (jpaManagedType instanceof IdentifiableType<?>) {
      final Type<?> idType = ((IdentifiableType<?>) jpaManagedType).getIdType();
      // Hibernate doesn't really support @IdClass declarations with multiple key
      // attributes
      if (idType == null) {
        final IdClass idClassAnnotation = jpaManagedType.getJavaType().getAnnotation(IdClass.class);
        if (idClassAnnotation != null) {
          if (jpaManagedType.getClass().getName().startsWith("org.hibernate")) {
            LOG.log(Level.WARNING, "invalid metamodel of Hibernate detected for " + getInternalName()
            + ", no idType or invalid... use workaround");
          }
          return idClassAnnotation.value();
        }
        // TODO @EmbeddedId also?
        throw new IllegalStateException("no key/pk/id class defined");
      }
      return idType.getJavaType();
    }
    throw new IllegalStateException("no key/pk/id class defined");
  }

  @Override
  public JPAAttributePath getStreamAttributePath() throws ODataJPAModelException {
    return (JPAAttributePath) getPath(getStreamProperty().getExternalName());
  }

  @Override
  public JPASelector getContentTypeAttributePath() throws ODataJPAModelException {
    final String propertyInternalName = getStreamProperty().getContentTypeProperty();
    if (propertyInternalName == null || propertyInternalName.isEmpty()) {
      return null;
    }
    // Ensure that Ignore is ignored
    return getPathByDBField(getProperty(propertyInternalName).getDBFieldName());
  }

  @Override
  public String getTableName() {
    final AnnotatedElement a = jpaManagedType.getJavaType();
    Table t = null;

    if (a != null) {
      t = a.getAnnotation(Table.class);
    }

    return (t == null) ? jpaManagedType.getJavaType().getName().toUpperCase(Locale.ENGLISH)
        : t.name();
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
  public List<JPAAttributePath> searchChildPath(final JPASelector selectItemPath) {
    final List<JPAAttributePath> result = new ArrayList<JPAAttributePath>();
    for (final String pathName : this.simpleAttributePathMap.keySet()) {
      final JPAAttributePath p = simpleAttributePathMap.get(pathName);
      if (!p.ignore() && p.getAlias().startsWith(selectItemPath.getAlias())) {
        result.add(p);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  protected <T> List<?> extractEdmProperties(final Map<String, ? extends IntermediateModelElement> mappingBuffer)
      throws ODataJPAModelException {
    final List<T> extractionTarget = new LinkedList<T>();
    for (final String externalName : mappingBuffer.keySet()) {
      if (!((IntermediateModelElement) mappingBuffer.get(externalName)).ignore()
          // Skip Streams
          && !(mappingBuffer.get(externalName) instanceof IntermediateProperty &&
              ((IntermediateProperty) mappingBuffer.get(externalName)).isStream())) {
        if (mappingBuffer.get(externalName) instanceof IntermediateEmbeddedIdProperty) {
          extractionTarget.addAll((Collection<? extends T>) resolveEmbeddedId(
              (IntermediateEmbeddedIdProperty) mappingBuffer.get(externalName)));
        } else {
          extractionTarget.add((T) ((IntermediateModelElement) mappingBuffer.get(externalName)).getEdmItem());
        }
      }
    }
    return extractionTarget;
    // return returnNullIfEmpty(extractionTarget);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    initializeType();
    if (edmEntityType == null) {
      edmEntityType = new CsdlEntityType();
      edmEntityType.setName(getExternalName());
      edmEntityType.setProperties((List<CsdlProperty>) extractEdmProperties(declaredPropertiesList));
      edmEntityType.setNavigationProperties(
          (List<CsdlNavigationProperty>) extractEdmProperties(
              declaredNaviPropertiesList));
      edmEntityType.setKey(extractEdmKeyElements(declaredPropertiesList));
      edmEntityType.setAbstract(isAbstract());
      edmEntityType.setBaseType(determineBaseType());
      edmEntityType.setHasStream(determineHasStream());
      determineHasEtag();
      // TODO determine OpenType
    }
  }

  private void determineHasEtag() {
    for (final String internalName : this.declaredPropertiesList.keySet()) {
      if (declaredPropertiesList.get(internalName).isEtag()) {
        hasEtag = true;
      }
    }
  }

  /**
   * Creates the key of an entity. In case the POJO is declared with an embedded
   * ID the key fields get resolved, so that they occur as separate properties
   * within the metadata document
   *
   * @param propertyList
   * @return
   * @throws ODataJPAModelException
   */
  List<CsdlPropertyRef> extractEdmKeyElements(final Map<String, IntermediateProperty> propertyList)
      throws ODataJPAModelException {
    // TODO setAlias
    final List<CsdlPropertyRef> keyList = new ArrayList<CsdlPropertyRef>();
    for (final String internalName : propertyList.keySet()) {
      if (propertyList.get(internalName).isKey()) {
        if (propertyList.get(internalName).isComplex()) {
          final List<JPASimpleAttribute> idAttributes = ((IntermediateComplexType) propertyList
              .get(internalName)
              .getStructuredType())
              .getAttributes();
          for (final JPAAttribute<?> idAttribute : idAttributes) {
            final CsdlPropertyRef key = new CsdlPropertyRef();
            key.setName(idAttribute.getExternalName());
            keyList.add(key);
          }
        } else {
          final CsdlPropertyRef key = new CsdlPropertyRef();
          key.setName(propertyList.get(internalName).getExternalName());
          keyList.add(key);
        }
      }
    }
    return returnNullIfEmpty(keyList);
  }

  @SuppressWarnings("unchecked")
  @Override
  public CsdlEntityType getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmEntityType;
  }

  @Override
  CsdlEntityType getEdmStructuralType() throws ODataJPAModelException {
    return getEdmItem();
  }

  private <T> List<?> resolveEmbeddedId(final IntermediateEmbeddedIdProperty embeddedId) throws ODataJPAModelException {
    return ((IntermediateComplexType) embeddedId.getStructuredType()).getEdmItem().getProperties();
  }
}
