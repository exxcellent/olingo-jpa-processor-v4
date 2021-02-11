package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class AbstractStructuredType<CsdlType extends CsdlStructuralType> extends IntermediateModelElement<CsdlType>
implements JPAStructuredType {

  protected final static Logger LOG = Logger.getLogger(AbstractStructuredType.class.getName());

  private final Map<String, JPAMemberAttribute> declaredPropertiesList;
  private final Map<String, JPAAssociationAttribute> declaredNaviPropertiesList;
  private final Map<String, JPAPathImpl> simpleAttributePathMap;
  private final Map<String, JPAPathImpl> complexAttributePathMap;
  private final Map<String, JPAAssociationPathImpl> associationPathMap;
  private InitializationState initStateType = InitializationState.NotInitialized;

  protected AbstractStructuredType(final JPAEdmNameBuilder nameBuilder, final String qualifiedInternalTypeName)
      throws ODataJPAModelException {
    super(nameBuilder, qualifiedInternalTypeName);
    this.declaredPropertiesList = new HashMap<String, JPAMemberAttribute>();
    this.simpleAttributePathMap = new HashMap<String, JPAPathImpl>();
    this.complexAttributePathMap = new HashMap<String, JPAPathImpl>();
    this.declaredNaviPropertiesList = new HashMap<String, JPAAssociationAttribute>();
    this.associationPathMap = new HashMap<String, JPAAssociationPathImpl>();
  }

  /**
   * @see #declaredPropertiesList
   */
  final protected void addSimpleProperty(final JPAMemberAttribute property) {
    declaredPropertiesList.put(property.getInternalName(), property);
  }

  /**
   * @see #declaredNaviPropertiesList
   */
  final protected void addNavigationProperty(final JPAAssociationAttribute property) {
    declaredNaviPropertiesList.put(property.getInternalName(), property);
  }

  @Override
  final public JPAAssociationAttribute getAssociationByPath(final JPAAssociationPath path)
      throws ODataJPAModelException {
    for (final JPAAssociationAttribute attribute : this.getAssociations()) {
      if (attribute.getExternalName().equals(path.getAlias())) {
        return attribute;
      }
    }
    return null;
  }

  final JPAAssociationAttribute getAssociationByAttributeName(final String internalName) throws ODataJPAModelException {
    for (final JPAAssociationAttribute attribute : this.getAssociations()) {
      if (attribute.getInternalName().equals(internalName)) {
        return attribute;
      }
    }
    return null;
  }

  @Override
  final public JPAAssociationPath getAssociationPath(final String externalName) throws ODataJPAModelException {
    lazyBuildCompleteAssociationPathMap();
    return associationPathMap.get(externalName);
  }

  @Override
  final public List<JPAAssociationPath> getAssociationPathList() throws ODataJPAModelException {
    lazyBuildCompleteAssociationPathMap();
    final List<JPAAssociationPath> associationList = new ArrayList<JPAAssociationPath>();

    for (final String externalName : associationPathMap.keySet()) {
      associationList.add(associationPathMap.get(externalName));
    }
    return associationList;
  }

  @Override
  final public JPAMemberAttribute getAttribute(final String internalName) throws ODataJPAModelException {
    initializeType();
    JPAMemberAttribute result = declaredPropertiesList.get(internalName);
    if (result == null && getBaseType() != null) {
      result = getBaseType().getAttribute(internalName);
    }
    return result;
  }

  @Override
  final public List<JPAMemberAttribute> getAttributes(final boolean exploded) throws ODataJPAModelException {
    return getAllAttributes(exploded).stream().filter(a -> !a.ignore()).collect(Collectors.toList());
  }

  @Override
  final public List<JPAMemberAttribute> getAllAttributes(final boolean exploded) throws ODataJPAModelException {
    initializeType();
    final List<JPAMemberAttribute> result = new LinkedList<JPAMemberAttribute>();
    for (final JPAMemberAttribute attribute : declaredPropertiesList.values()) {
      if (exploded && attribute.isKey() && attribute.isComplex()) {
        result.addAll(attribute.getStructuredType().getAllAttributes(exploded));
      } else {
        result.add(attribute);
      }
    }
    if (getBaseType() != null) {
      result.addAll(getBaseType().getAllAttributes(exploded));
    }
    return result;
  }

  @Override
  final public List<JPAMemberAttribute> getKeyAttributes(final boolean exploded) throws ODataJPAModelException {
    final List<JPAMemberAttribute> keyList = new LinkedList<JPAMemberAttribute>();
    for (final JPAMemberAttribute attribute : getAttributes(false)) {
      if (!attribute.isKey()) {
        continue;
      }
      if (exploded && attribute.isComplex()) {
        // take ALL attributes, because in @Embeddable are no keys (@Id)
        keyList.addAll(attribute.getStructuredType().getAttributes(false));
      } else {
        keyList.add(attribute);
      }
    }
    return keyList;
  }

  /**
   * Creates the key of an entity. In case the POJO is declared with an embedded
   * ID the key fields get resolved, so that they occur as separate properties
   * within the metadata document
   *
   * @throws ODataJPAModelException
   */
  final protected List<CsdlPropertyRef> extractEdmKeyElements()
      throws ODataJPAModelException {
    // TODO setAlias
    final List<CsdlPropertyRef> keyList = new ArrayList<CsdlPropertyRef>();
    for (final JPAMemberAttribute attribute : getKeyAttributes(true)) {
      final CsdlPropertyRef key = new CsdlPropertyRef();
      key.setName(attribute.getExternalName());
      keyList.add(key);
    }
    return returnNullIfEmpty(keyList);
  }

  @Override
  final public JPAAssociationPath getDeclaredAssociation(final String externalName) throws ODataJPAModelException {
    lazyBuildCompleteAssociationPathMap();
    for (final String internalName : declaredNaviPropertiesList.keySet()) {
      if (externalName.equals(declaredNaviPropertiesList.get(internalName).getExternalName())) {
        return associationPathMap.get(externalName);
      }
    }
    final JPAStructuredType baseType = getBaseType();
    if (baseType != null) {
      return baseType.getDeclaredAssociation(externalName);
    }
    return null;
  }

  @Override
  final public JPASelector getPath(final String externalName) throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    JPASelector targetPath = simpleAttributePathMap.get(externalName);
    if (targetPath != null) {
      return targetPath;
    }
    targetPath = complexAttributePathMap.get(externalName);
    if (targetPath != null) {
      return targetPath;
    }
    return targetPath = associationPathMap.get(externalName);
  }

  @Override
  final public List<JPASelector> getPathList() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    final List<JPASelector> pathList = new ArrayList<JPASelector>();
    for (final Entry<String, JPAPathImpl> entry : simpleAttributePathMap.entrySet()) {
      if (entry.getValue().ignore()) {
        continue;
      }
      pathList.add(entry.getValue());
    }
    return pathList;
  }

  @Override
  final public List<JPASelector> getSearchablePath() throws ODataJPAModelException {
    final List<JPASelector> allPath = getPathList();
    final List<JPASelector> searchablePath = new ArrayList<JPASelector>(allPath.size());
    for (final JPASelector p : allPath) {
      if (p.getLeaf().isSearchable()) {
        searchablePath.add(p);
      }
    }
    return searchablePath;
  }

  protected abstract void buildPropertyList() throws ODataJPAModelException;

  /**
   * Internal class/type method to initialize state of type.
   */
  protected final void initializeType() throws ODataJPAModelException {
    switch (initStateType) {
    case Initialized:
      return;
    case InProgress:
      throw new IllegalStateException("Initialization already in progress, recursion problem!");
    default:
      break;
    }

    try {
      initStateType = InitializationState.InProgress;
      buildPropertyList();
    } finally {
      initStateType = InitializationState.Initialized;
    }

  }

  final protected FullQualifiedName determineBaseType() throws ODataJPAModelException {

    final JPAStructuredType baseEntity = getBaseType();
    if (baseEntity != null && !baseEntity.isAbstract() && isAbstract()) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INHERITANCE_NOT_ALLOWED,
          this.getInternalName(), baseEntity.getInternalName());
    }
    return baseEntity != null ? baseEntity.getExternalFQN() : null;
  }

  abstract protected JPAStructuredType getBaseType() throws ODataJPAModelException;

  @Override
  final public List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException {
    initializeType();
    final List<JPAAssociationAttribute> jpaAttributes = new LinkedList<JPAAssociationAttribute>();
    for (final String internalName : declaredNaviPropertiesList.keySet()) {
      final JPAAssociationAttribute property = declaredNaviPropertiesList.get(internalName);
      if (!property.ignore()) {
        jpaAttributes.add(property);
      }
    }
    final JPAStructuredType baseType = getBaseType();
    if (baseType != null) {
      jpaAttributes.addAll(baseType.getAssociations());
    }
    return jpaAttributes;
  }

  /**
   * This does not resolve associations! It's only for simple attributes.
   *
   * @param dbFieldName
   *            The path to find based on db field name.
   * @return The path or <code>null</code>
   * @throws ODataJPAModelException
   * @Deprecated Multiple attribute may use the same DB field, so the DB column
   *             name is not always unique. Use {@link #getPath(String)} if you a
   *             attribute in context.
   */
  @Deprecated
  final JPAAttributePath getPathByDBField(final String dbFieldName)
      throws ODataJPAModelException {
    lazyBuildCompletePathMap();

    // find any db field names
    JPAPathImpl found = null;
    for (final JPAPathImpl path : simpleAttributePathMap.values()) {
      if (path.getDBFieldName().equals(dbFieldName)) {
        if (found != null) {
          LOG.log(Level.WARNING, "Ambiguous DB column name '" + dbFieldName + "' used to find attribute");
          return null;
        }
        found = path;
      }
    }
    return found;
  }

  /**
   * Internal method to access also {@link IntermediateProperty#ignore() ignored}
   * properties.
   */
  final JPAMemberAttribute getPropertyByDBField(final String dbFieldName) throws ODataJPAModelException {
    initializeType();
    for (final String internalName : declaredPropertiesList.keySet()) {
      final JPAMemberAttribute property = declaredPropertiesList.get(internalName);
      if (property.getDBFieldName().equals(dbFieldName)) {
        return property;
      }
    }
    if (getBaseType() != null) {
      // TODO: base class must be a JPA type, so we can cast... but has a bad smell
      return ((AbstractStructuredType<?>) getBaseType()).getPropertyByDBField(dbFieldName);
    }
    return null;
  }

  final Map<String, JPAPathImpl> getSimpleAttributePathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return simpleAttributePathMap;
  }

  final Map<String, JPAPathImpl> getComplexAttributePathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return complexAttributePathMap;
  }

  final Map<String, JPAAssociationPathImpl> getAssociationPathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return associationPathMap;
  }

  private void lazyBuildCompleteAssociationPathMap() throws ODataJPAModelException {
    JPAAssociationPathImpl associationPath;
    lazyBuildCompletePathMap();
    // TODO check if ignore has to be handled
    if (associationPathMap.size() == 0) {
      for (final JPAAssociationAttribute navProperty : getAssociations()) {
        associationPath = new JPAAssociationPathImpl((AbstractNavigationProperty) navProperty, this);
        associationPathMap.put(associationPath.getAlias(), associationPath);
      }

      for (final String key : this.complexAttributePathMap.keySet()) {
        final JPAAttributePath attributePath = this.complexAttributePathMap.get(key);
        if (attributePath.getPathElements().size() == 1) {
          // Only direct attributes
          final JPAAttribute<?> property = attributePath.getLeaf();
          final AbstractStructuredType<?> nestedComplexType = (AbstractStructuredType<?>) property
              .getStructuredType();
          // the 'nested complex type' is in the DB handled by same table (of me), so we
          // have to build association paths
          // for queries as if all of the 'nested complex type' properties are defined by
          // this structured type
          for (final JPAAssociationPath association : nestedComplexType.getAssociationPathList()) {
            associationPath = new JPAAssociationPathImpl(getNameBuilder(), property, association, this,
                determineJoinColumns(property, association));
            associationPathMap.put(associationPath.getAlias(), associationPath);
          }
        }
      }
    }
  }

  abstract protected List<IntermediateJoinColumn> determineJoinColumns(final JPAAttribute<?> property,
      final JPAAssociationPath association);

  abstract protected String determineDBFieldName(final JPAMemberAttribute property, final JPAAttributePath jpaPath);

  private void lazyBuildCompletePathMap() throws ODataJPAModelException {
    initializeType();

    if (!simpleAttributePathMap.isEmpty()) {
      return;
    }
    ArrayList<JPAAttribute<?>> pathList;
    String externalName;
    for (final JPAMemberAttribute property : declaredPropertiesList.values()) {
      if (property.isComplex()) {
        // for @EmbeddedId also
        complexAttributePathMap.put(property.getExternalName(),
            new JPAPathImpl(property.getExternalName(), null, property));
        final Map<String, JPAPathImpl> nestedComplexAttributePathMap = ((AbstractStructuredType<?>) property
            .getStructuredType()).getComplexAttributePathMap();
        for (final Entry<String, JPAPathImpl> entry : nestedComplexAttributePathMap.entrySet()) {
          externalName = entry.getKey();
          pathList = new ArrayList<JPAAttribute<?>>(entry.getValue().getPathElements());
          pathList.add(0, property);
          complexAttributePathMap.put(getNameBuilder().buildPath(property.getExternalName(), externalName),
              new JPAPathImpl(getNameBuilder().buildPath(property.getExternalName(),
                  externalName), null, pathList));
        }

        // add the (simple) properties of complex type as path to this type
        final Map<String, JPAPathImpl> nestedSimpleAttributePathMap = ((AbstractStructuredType<?>) property
            .getStructuredType()).getSimpleAttributePathMap();
        JPAPathImpl newPath;
        for (final Entry<String, JPAPathImpl> entry : nestedSimpleAttributePathMap.entrySet()) {
          externalName = entry.getKey();
          pathList = new ArrayList<JPAAttribute<?>>(entry.getValue().getPathElements());
          pathList.add(0, property);
          if (property.isKey()) {
            newPath = new JPAPathImpl(externalName, determineDBFieldName(property, entry.getValue()),
                pathList);
          } else {
            newPath = new JPAPathImpl(getNameBuilder().buildPath(property.getExternalName(), externalName),
                determineDBFieldName(property, entry.getValue()), pathList);
          }
          simpleAttributePathMap.put(newPath.getAlias(), newPath);
        }
      } else {
        simpleAttributePathMap.put(property.getExternalName(), new JPAPathImpl(property.getExternalName(), property
            .getDBFieldName(), property));
      }
    }
    // TODO: base class must be a JPA type, so we can cast... but has a bad smell
    final AbstractStructuredType<?> baseType = (AbstractStructuredType<?>) getBaseType();
    if (baseType != null) {
      simpleAttributePathMap.putAll(baseType.getSimpleAttributePathMap());
      complexAttributePathMap.putAll(baseType.getComplexAttributePathMap());
    }

  }

  final protected boolean determineHasStream() throws ODataJPAModelException {
    return getStreamProperty() == null ? false : true;
  }

  final protected JPAMemberAttribute getStreamProperty() throws ODataJPAModelException {
    int count = 0;
    JPAMemberAttribute result = null;
    for (final String internalName : declaredPropertiesList.keySet()) {
      if (((AbstractProperty<?>) declaredPropertiesList.get(internalName)).isStream()) {
        count += 1;
        result = declaredPropertiesList.get(internalName);
      }
    }
    if (this.getBaseType() != null) {
      // TODO: base class must be a JPA type, so we can cast... but has a bad smell
      final JPAMemberAttribute superResult = ((AbstractStructuredType<?>) getBaseType())
          .getStreamProperty();
      if (superResult != null) {
        count += 1;
        result = superResult;
      }
    }
    if (count > 1) {
      // Only one stream property per entity is allowed. For %1$s %2$s have been found
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TO_MANY_STREAMS, getInternalName(), Integer
          .toString(count));
    }
    return result;
  }
}