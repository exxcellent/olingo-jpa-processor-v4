package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAMemberAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class AbstractStructuredTypeJPA<JPAType extends ManagedType<?>, CsdlType extends CsdlStructuralType> extends
AbstractStructuredType<CsdlType> {

  private final JPAType jpaManagedType;
  private final IntermediateServiceDocument serviceDocument;

  protected AbstractStructuredTypeJPA(final JPAEdmNameBuilder nameBuilder, final JPAType jpaManagedType,
      final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
    super(nameBuilder, jpaManagedType.getJavaType().getCanonicalName());
    this.jpaManagedType = jpaManagedType;
    this.serviceDocument = serviceDocument;

  }

  final protected JPAType getManagedType() {
    return jpaManagedType;
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

  @Override
  protected void buildPropertyList() throws ODataJPAModelException {
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
        if (jpaAttribute instanceof SingularAttribute<?, ?> && ((SingularAttribute<?, ?>) jpaAttribute).isId()
            && attributeType == PersistentAttributeType.EMBEDDED) {
          property = new IntermediateEmbeddedIdProperty(nameBuilder, jpaAttribute, serviceDocument);
        } else {
          property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
        }
        addSimpleProperty(property);
        break;
      case ONE_TO_MANY:
      case ONE_TO_ONE:
      case MANY_TO_MANY:
      case MANY_TO_ONE:
        navProp = new IntermediateNavigationProperty(nameBuilder, this, jpaAttribute, serviceDocument);
        addNavigationProperty(navProp);
        break;
      case ELEMENT_COLLECTION:
        if (TypeMapping.isEmbeddableTypeCollection(jpaAttribute)) {
          property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
          addSimpleProperty(property);
          break;
        } else if (jpaAttribute instanceof PluralAttribute) {
          // primitive type collection
          property = new IntermediateProperty(nameBuilder, jpaAttribute, serviceDocument);
          addSimpleProperty(property);
          break;
        }
        // fall through
      default:
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.NOT_SUPPORTED_ATTRIBUTE_TYPE,
            attributeType.name(), jpaAttribute.getDeclaringType().getJavaType().getName());
      }
    }
  }

  @Override
  final public boolean isAbstract() {
    final int modifiers = jpaManagedType.getJavaType().getModifiers();
    return Modifier.isAbstract(modifiers);
  }

  @Override
  final public boolean isOpenType() {
    // always false for JPA
    return false;
  }

  @Override
  final protected AbstractStructuredType<?> getBaseType() throws ODataJPAModelException {
    final Class<?> baseType = jpaManagedType.getJavaType().getSuperclass();
    if (baseType == null) {
      return null;
    }
    if (isMappedSuperclass(baseType)) {
      return null;
    }
    return (AbstractStructuredType<?>) serviceDocument.getEntityType(baseType);
  }

  private static boolean isMappedSuperclass(final Class<?> clazz) {
    if (clazz == null) {
      return false;
    }
    return clazz.getAnnotation(MappedSuperclass.class) != null;
  }

  /**
   *
   * @param relationshipAttributeName
   *            Normally the name of the attribute
   *            identified by 'mappedBy'.
   */
  final Attribute<?, ?> findJPARelationshipAttribute(final String relationshipAttributeName) {
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
  final Attribute<?, ?> findCorrespondingMappedByImplementingAttribute(final Class<?> relationshipTargetType,
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

  @Override
  protected final String determineDBFieldName(final JPAMemberAttribute property, final JPAAttributePath jpaPath) {
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

  @Override
  protected List<IntermediateJoinColumn> determineJoinColumns(final JPAAttribute<?> property,
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

}