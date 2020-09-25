package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Attribute.PersistentAttributeType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class IntermediateStructuredType<CsdlType extends CsdlStructuralType> extends IntermediateModelElement
implements JPAStructuredType {

  protected final static Logger LOG = Logger.getLogger(IntermediateStructuredType.class.getName());

  protected final Map<String, IntermediateProperty> declaredPropertiesList;
  protected final Map<String, IntermediateNavigationProperty> declaredNaviPropertiesList;
  protected final Map<String, JPAPathImpl> simpleAttributePathMap;
  protected final Map<String, JPAPathImpl> complexAttributePathMap;
  protected final Map<String, JPAAssociationPathImpl> associationPathMap;
  protected final ManagedType<?> jpaManagedType;
  protected final IntermediateServiceDocument serviceDocument;
  private InitializationState initStateType = InitializationState.NotInitialized;

  protected IntermediateStructuredType(final JPAEdmNameBuilder nameBuilder, final ManagedType<?> jpaManagedType,
      final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
    super(nameBuilder, jpaManagedType.getJavaType().getCanonicalName());
    this.declaredPropertiesList = new HashMap<String, IntermediateProperty>();
    this.simpleAttributePathMap = new HashMap<String, JPAPathImpl>();
    this.complexAttributePathMap = new HashMap<String, JPAPathImpl>();
    this.declaredNaviPropertiesList = new HashMap<String, IntermediateNavigationProperty>();
    this.associationPathMap = new HashMap<String, JPAAssociationPathImpl>();
    this.jpaManagedType = jpaManagedType;
    this.serviceDocument = serviceDocument;

  }

  abstract CsdlType getEdmStructuralType() throws ODataJPAModelException;

  ManagedType<?> getJpaManagedType() {
    return jpaManagedType;
  }

  @Override
  public JPAAssociationAttribute getAssociationByPath(final JPAAssociationPath path) throws ODataJPAModelException {
    for (final JPAAssociationAttribute attribute : this.getAssociations()) {
      if (attribute.getExternalName().equals(path.getAlias())) {
        return attribute;
      }
    }
    return null;
  }

  JPAAssociationAttribute getAssociationByAttributeName(final String internalName) throws ODataJPAModelException {
    for (final JPAAssociationAttribute attribute : this.getAssociations()) {
      if (attribute.getInternalName().equals(internalName)) {
        return attribute;
      }
    }
    return null;
  }

  @Override
  public JPAAssociationPath getAssociationPath(final String externalName) throws ODataJPAModelException {
    lazyBuildCompleteAssociationPathMap();
    return associationPathMap.get(externalName);
  }

  @Override
  public List<JPAAssociationPath> getAssociationPathList() throws ODataJPAModelException {
    lazyBuildCompleteAssociationPathMap();
    final List<JPAAssociationPath> associationList = new ArrayList<JPAAssociationPath>();

    for (final String externalName : associationPathMap.keySet()) {
      associationList.add(associationPathMap.get(externalName));
    }
    return associationList;
  }

  @Override
  public JPAMemberAttribute getAttribute(final String internalName) throws ODataJPAModelException {
    initializeType();
    JPAMemberAttribute result = declaredPropertiesList.get(internalName);
    if (result == null && getBaseType() != null) {
      result = getBaseType().getAttribute(internalName);
    } else if (result != null && ((IntermediateModelElement) result).ignore()) {
      return null;
    }
    return result;
  }

  @Override
  public List<JPAMemberAttribute> getAttributes() throws ODataJPAModelException {
    initializeType();
    final List<JPAMemberAttribute> result = new ArrayList<JPAMemberAttribute>();
    for (final String propertyKey : declaredPropertiesList.keySet()) {
      final IntermediateProperty attribute = declaredPropertiesList.get(propertyKey);
      if (!attribute.ignore()) {
        result.add(attribute);
      }
    }
    if (getBaseType() != null) {
      result.addAll(getBaseType().getAttributes());
    }
    return result;
  }

  @Override
  public List<JPAMemberAttribute> getKeyAttributes(final boolean exploded) throws ODataJPAModelException {
    final List<JPAMemberAttribute> keyList = new LinkedList<JPAMemberAttribute>();
    for (final JPAMemberAttribute attribute : getAttributes()) {
      if (!attribute.isKey()) {
        continue;
      }
      if (exploded && attribute.isComplex()) {
        // take ALL attributes, because in @Embeddable are no keys (@Id)
        keyList.addAll(attribute.getStructuredType().getAttributes());
      } else {
        keyList.add(attribute);
      }
    }
    return keyList;
  }

  @Override
  public JPAAssociationPath getDeclaredAssociation(final String externalName) throws ODataJPAModelException {
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
  public JPASelector getPath(final String externalName) throws ODataJPAModelException {
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
  public List<JPASelector> getPathList() throws ODataJPAModelException {
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
  public List<JPASelector> getSearchablePath() throws ODataJPAModelException {
    final List<JPASelector> allPath = getPathList();
    final List<JPASelector> searchablePath = new ArrayList<JPASelector>(allPath.size());
    for (final JPASelector p : allPath) {
      if (p.getLeaf().isSearchable()) {
        searchablePath.add(p);
      }
    }
    return searchablePath;
  }

  @Override
  public Class<?> getTypeClass() {
    return this.jpaManagedType.getJavaType();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Collection<Attribute<?, ?>> determineJPAAttributes() {
    if (isMappedSuperclass(jpaManagedType.getJavaType().getSuperclass())) {
      // include attributes from mapped superclass -> required for Hibernate,
      // EclipseLink doesn't need it
      return (Collection) jpaManagedType.getAttributes();
    }
    // attributes without superclass
    return (Collection) jpaManagedType.getDeclaredAttributes();
  }

  /**
   * Internal class/type method to initialize state of type.
   */
  protected void initializeType() throws ODataJPAModelException {
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

      IntermediateProperty property;
      IntermediateNavigationProperty navProp;
      final JPAEdmNameBuilder nameBuilder = getNameBuilder();
      for (final Attribute<?, ?> jpaAttribute : determineJPAAttributes()) {
        // also attributes marked with @EdmIgnore are collected, to be present for some
        // association related functionality
        final PersistentAttributeType attributeType = jpaAttribute.getPersistentAttributeType();
        switch (attributeType) {
        case BASIC:
        case EMBEDDED:
          if (jpaAttribute instanceof SingularAttribute<?, ?>
          && ((SingularAttribute<?, ?>) jpaAttribute).isId()
          && attributeType == PersistentAttributeType.EMBEDDED) {
            property = new IntermediateEmbeddedIdProperty(nameBuilder, jpaAttribute, serviceDocument);
            declaredPropertiesList.put(property.getInternalName(), property);
          } else {
            property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
            declaredPropertiesList.put(property.getInternalName(), property);
          }
          break;
        case ONE_TO_MANY:
        case ONE_TO_ONE:
        case MANY_TO_MANY:
        case MANY_TO_ONE:
          navProp = new IntermediateNavigationProperty(nameBuilder, this, jpaAttribute, serviceDocument);
          declaredNaviPropertiesList.put(navProp.getInternalName(), navProp);
          break;
        case ELEMENT_COLLECTION:
          if (TypeMapping.isEmbeddableTypeCollection(jpaAttribute)) {
            property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
            declaredPropertiesList.put(property.getInternalName(), property);
            break;
          } else if (jpaAttribute instanceof PluralAttribute) {
            // primitive type collection
            property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
            declaredPropertiesList.put(property.getInternalName(), property);
            break;
          }
          // fall through
        default:
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
              attributeType.name(), jpaAttribute.getDeclaringType().getJavaType().getName());
        }
      }
    } finally {
      initStateType = InitializationState.Initialized;
    }

  }

  protected FullQualifiedName determineBaseType() throws ODataJPAModelException {

    final JPAStructuredType baseEntity = getBaseType();
    if (baseEntity != null && !baseEntity.isAbstract() && isAbstract()) {
      throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.INHERITANCE_NOT_ALLOWED,
          this.getInternalName(), baseEntity.getInternalName());
    }
    return baseEntity != null ? getNameBuilder().buildFQN(baseEntity.getExternalName()) : null;
  }

  @Override
  public boolean isAbstract() {
    final int modifiers = jpaManagedType.getJavaType().getModifiers();
    return Modifier.isAbstract(modifiers);
  }

  protected JPAStructuredType getBaseType() throws ODataJPAModelException {
    final Class<?> baseType = jpaManagedType.getJavaType().getSuperclass();
    if (baseType == null) {
      return null;
    }
    if (isMappedSuperclass(baseType)) {
      return null;
    }
    return serviceDocument.getEntityType(baseType);
  }

  private static boolean isMappedSuperclass(final Class<?> clazz) {
    if (clazz == null) {
      return false;
    }
    return clazz.getAnnotation(MappedSuperclass.class) != null;
  }

  @Override
  public List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException {
    initializeType();
    final List<JPAAssociationAttribute> jpaAttributes = new LinkedList<JPAAssociationAttribute>();
    for (final String internalName : declaredNaviPropertiesList.keySet()) {
      final IntermediateNavigationProperty property = declaredNaviPropertiesList.get(internalName);
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
  JPAAttributePath getPathByDBField(final String dbFieldName)
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
  IntermediateProperty getPropertyByDBField(final String dbFieldName) throws ODataJPAModelException {
    initializeType();
    for (final String internalName : declaredPropertiesList.keySet()) {
      final IntermediateProperty property = declaredPropertiesList.get(internalName);
      if (property.getDBFieldName().equals(dbFieldName)) {
        return property;
      }
    }
    if (getBaseType() != null) {
      // TODO: base class must be a JPA type, so we can cast... but has a bad smell
      return ((IntermediateStructuredType<?>) getBaseType()).getPropertyByDBField(dbFieldName);
    }
    return null;
  }

  /**
   * Returns an property regardless if it should be ignored or not
   *
   * @param internalName
   * @return
   * @throws ODataJPAModelException
   */
  IntermediateProperty getProperty(final String internalName) throws ODataJPAModelException {
    initializeType();
    IntermediateProperty result = declaredPropertiesList.get(internalName);
    if (result == null && getBaseType() != null) {
      // TODO: base class must be a JPA type, so we can cast... but has a bad smell
      result = ((IntermediateStructuredType<?>) getBaseType()).getProperty(internalName);
    }
    return result;
  }

  /**
   *
   * @param relationshipAttributeName
   *            Normally the name of the attribute
   *            identified by 'mappedBy'.
   */
  Attribute<?, ?> findJPARelationshipAttribute(final String relationshipAttributeName) {

    for (final Attribute<?, ?> jpaAttribute : determineJPAAttributes()) {
      if (relationshipAttributeName.equals(jpaAttribute.getName())) {
        return jpaAttribute;
      }
    }
    return null;
  }

  /**
   *
   * Find a attribute having the relationship annotation targeting the requested relationship target/entity type and
   * having a 'mappedBy' annotation value of given name.
   *
   * @return The attribute defining the mapping for the given 'mappedBy' value on the relationship owning side (pointed
   * by 'mappedBy' on referencing side) or <code>null</code>.
   */
  Attribute<?, ?> findCorrespondingMappedByImplementingAttribute(final Class<?> relationshipTargetType,
      final String mappedByIdentifier) {
    Class<?> targetClass = null;

    for (final Attribute<?, ?> jpaAttribute : determineJPAAttributes()) {
      if (jpaAttribute.getPersistentAttributeType() != null
          && jpaAttribute.getJavaMember() instanceof AnnotatedElement
          && !mappedByIdentifier.equals(jpaAttribute.getName())) {
        if (jpaAttribute.isCollection()) {
          targetClass = ((PluralAttribute<?, ?, ?>) jpaAttribute).getElementType().getJavaType();
        } else {
          targetClass = jpaAttribute.getJavaType();
        }
        if (!targetClass.equals(relationshipTargetType)) {
          continue;
        }
        final OneToMany o2m = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(OneToMany.class);
        if (o2m != null && o2m.mappedBy() != null && o2m.mappedBy().equals(mappedByIdentifier)) {
          return jpaAttribute;
        }
        final OneToOne o2o = ((AnnotatedElement) jpaAttribute.getJavaMember()).getAnnotation(OneToOne.class);
        if (o2o != null && o2o.mappedBy() != null && o2o.mappedBy().equals(mappedByIdentifier)) {
          return jpaAttribute;
        }
        final ManyToMany m2m = ((AnnotatedElement) jpaAttribute.getJavaMember())
            .getAnnotation(ManyToMany.class);
        if (m2m != null && m2m.mappedBy() != null && m2m.mappedBy().equals(mappedByIdentifier)) {
          return jpaAttribute;
        }
      }
    }

    return null;
  }

  Map<String, JPAPathImpl> getSimpleAttributePathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return simpleAttributePathMap;
  }

  Map<String, JPAPathImpl> getComplexAttributePathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return complexAttributePathMap;
  }

  Map<String, JPAAssociationPathImpl> getAssociationPathMap() throws ODataJPAModelException {
    lazyBuildCompletePathMap();
    return associationPathMap;
  }

  private String determineDBFieldName(final IntermediateProperty property, final JPAAttributePath jpaPath) {
    final Attribute<?, ?> jpaAttribute = jpaManagedType.getAttribute(property.getInternalName());
    if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
      final AnnotatedElement a = (AnnotatedElement) jpaAttribute.getJavaMember();
      final AttributeOverrides overwriteList = a.getAnnotation(AttributeOverrides.class);
      if (overwriteList != null) {
        for (final AttributeOverride overwrite : overwriteList.value()) {
          if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
            return overwrite.column().name();
          }
        }
      } else {
        final AttributeOverride overwrite = a.getAnnotation(AttributeOverride.class);
        if (overwrite != null) {
          if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
            return overwrite.column().name();
          }
        }
      }
    }
    return jpaPath.getDBFieldName();
  }

  private List<IntermediateJoinColumn> determineJoinColumns(final IntermediateProperty property,
      final JPAAssociationPath association) {
    final List<IntermediateJoinColumn> result = new ArrayList<IntermediateJoinColumn>();

    final Attribute<?, ?> jpaAttribute = jpaManagedType.getAttribute(property.getInternalName());
    if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
      final AnnotatedElement a = (AnnotatedElement) jpaAttribute.getJavaMember();
      if (jpaAttribute.getJavaMember() instanceof AnnotatedElement) {
        final AssociationOverrides overwriteList = a.getAnnotation(AssociationOverrides.class);
        if (overwriteList != null) {
          for (final AssociationOverride overwrite : overwriteList.value()) {
            if (overwrite.name().equals(association.getLeaf().getInternalName())) {
              for (final JoinColumn column : overwrite.joinColumns()) {
                result.add(new IntermediateJoinColumn(column));
              }
            }
          }
        } else {
          final AssociationOverride overwrite = a.getAnnotation(AssociationOverride.class);
          if (overwrite != null) {
            if (overwrite.name().equals(association.getLeaf().getInternalName())) {
              for (final JoinColumn column : overwrite.joinColumns()) {
                result.add(new IntermediateJoinColumn(column));
              }
            }
          }
        }
      }
    }
    return result;
  }

  private void lazyBuildCompleteAssociationPathMap() throws ODataJPAModelException {
    JPAAssociationPathImpl associationPath;
    lazyBuildCompletePathMap();
    // TODO check if ignore has to be handled
    if (associationPathMap.size() == 0) {
      for (final JPAAssociationAttribute navProperty : getAssociations()) {
        associationPath = new JPAAssociationPathImpl((IntermediateNavigationProperty) navProperty, this);
        associationPathMap.put(associationPath.getAlias(), associationPath);
      }

      for (final String key : this.complexAttributePathMap.keySet()) {
        final JPAAttributePath attributePath = this.complexAttributePathMap.get(key);
        if (attributePath.getPathElements().size() == 1) {
          // Only direct attributes
          final IntermediateProperty property = (IntermediateProperty) attributePath.getLeaf();
          final IntermediateStructuredType<?> nestedComplexType = (IntermediateStructuredType<?>) property
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

  private void lazyBuildCompletePathMap() throws ODataJPAModelException {
    initializeType();

    if (!simpleAttributePathMap.isEmpty()) {
      return;
    }
    ArrayList<JPAAttribute<?>> pathList;
    String externalName;
    for (final IntermediateProperty property : declaredPropertiesList.values()) {
      if (property.isComplex()) {
        complexAttributePathMap.put(property.getExternalName(),
            new JPAPathImpl(property.getExternalName(), null, property));
        final Map<String, JPAPathImpl> nestedComplexAttributePathMap = ((IntermediateStructuredType<?>) property
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
        final Map<String, JPAPathImpl> nestedSimpleAttributePathMap = ((IntermediateStructuredType<?>) property
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
    final IntermediateStructuredType<?> baseType = (IntermediateStructuredType<?>) getBaseType();
    if (baseType != null) {
      simpleAttributePathMap.putAll(baseType.getSimpleAttributePathMap());
      complexAttributePathMap.putAll(baseType.getComplexAttributePathMap());
    }

  }

  protected boolean determineHasStream() throws ODataJPAModelException {
    return getStreamProperty() == null ? false : true;
  }

  protected IntermediateProperty getStreamProperty() throws ODataJPAModelException {
    int count = 0;
    IntermediateProperty result = null;
    for (final String internalName : declaredPropertiesList.keySet()) {
      if (declaredPropertiesList.get(internalName).isStream()) {
        count += 1;
        result = declaredPropertiesList.get(internalName);
      }
    }
    if (this.getBaseType() != null) {
      // TODO: base class must be a JPA type, so we can cast... but has a bad smell
      final IntermediateProperty superResult = ((IntermediateStructuredType<?>) getBaseType())
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