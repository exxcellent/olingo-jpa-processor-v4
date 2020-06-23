package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmAction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunction;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmFunctions;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class TestHelper {
  final private Metamodel jpaMetamodel;
  @Deprecated
  final public IntermediateServiceDocument serviceDocument;
  private final JPAEdmProvider edmProvider;

  public TestHelper(final Metamodel metamodel, final String namespace) throws ODataJPAModelException {
    this.jpaMetamodel = metamodel;
    try {
      edmProvider = new JPAEdmProvider(namespace, jpaMetamodel);
    } catch (final ODataException e) {
      throw new ODataJPAModelException(e);
    }
    this.serviceDocument = edmProvider.getServiceDocument();
    serviceDocument.getEdmSchemas();// init model
  }

  public IntermediateServiceDocument getServiceDocument() {
    return serviceDocument;
  }

  public JPAEdmProvider getEdmProvider() {
    return edmProvider;
  }

  public EntityType<?> getEntityType(final String typeName) {
    for (final EntityType<?> entityType : jpaMetamodel.getEntities()) {
      if (entityType.getJavaType().getSimpleName().equals(typeName)) {
        return entityType;
      }
    }
    return null;
  }

  public JPAEntityType getJPAEntityType(final String entitySetName) throws ODataJPAModelException {
    return serviceDocument.getEntityType(entitySetName);
  }

  public JPAAssociationPath getJPAAssociationPath(final String entitySetName, final String attributeExtName)
      throws ODataJPAModelException {
    final JPAEntityType jpaEntity = serviceDocument.getEntityType(entitySetName);
    return jpaEntity.getAssociationPath(attributeExtName);
  }

  public JPAAssociationAttribute getJPAAssociation(final String entitySetName, final String attributeIntName)
      throws ODataJPAModelException {
    final JPAEntityType jpaEntity = serviceDocument.getEntityType(entitySetName);
    for (final JPAAssociationAttribute attribute : jpaEntity.getAssociations()) {
      if (attribute.getInternalName().equals(attributeIntName)) {
        return attribute;
      }
    }
    return null;
  }

  public JPAAttribute<?> getJPAAttribute(final String entitySetName, final String attributeIntName)
      throws ODataJPAModelException {
    final JPAEntityType jpaEntity = serviceDocument.getEntityType(entitySetName);
    return jpaEntity.getAttribute(attributeIntName);
  }

  public EdmFunction getStoredProcedure(final EntityType<?> jpaEntityType, final String string) {
    if (jpaEntityType.getJavaType() instanceof AnnotatedElement) {
      final EdmFunctions jpaStoredProcedureList = ((AnnotatedElement) jpaEntityType.getJavaType())
          .getAnnotation(EdmFunctions.class);
      if (jpaStoredProcedureList != null) {
        for (final EdmFunction jpaStoredProcedure : jpaStoredProcedureList.value()) {
          if (jpaStoredProcedure.name().equals(string)) {
            return jpaStoredProcedure;
          }
        }
      }
    }
    return null;
  }

  public Attribute<?, ?> getAttribute(final ManagedType<?> et, final String attributeName) {
    for (final SingularAttribute<?, ?> attribute : et.getSingularAttributes()) {
      if (attribute.getName().equals(attributeName)) {
        return attribute;
      }
    }
    return null;
  }

  public EmbeddableType<?> getEmbeddedableType(final String typeName) {
    for (final EmbeddableType<?> embeddableType : jpaMetamodel.getEmbeddables()) {
      if (embeddableType.getJavaType().getSimpleName().equals(typeName)) {
        return embeddableType;
      }
    }
    return null;
  }

  public Attribute<?, ?> getDeclaredAttribute(final ManagedType<?> et, final String attributeName) {
    for (final Attribute<?, ?> attribute : et.getDeclaredAttributes()) {
      if (attribute.getName().equals(attributeName)) {
        return attribute;
      }
    }
    return null;
  }

  /**
   * Find the requested java method for given action name.
   *
   * @param jpaEntityType The entity (with java type in background) to inspect.
   * @param actionName The {@link EdmAction#name() action name} to find the assigned java method for.
   * @return The found method or <code>null</code>
   */
  public Method getActionMethod(final EntityType<?> jpaEntityType, final String actionName) {
    for (final Method method : jpaEntityType.getJavaType().getMethods()) {
      final EdmAction action = method.getAnnotation(EdmAction.class);
      if (action == null) {
        continue;
      }
      if (action.name().equals(actionName)) {
        return method;
      }
    }
    return null;
  }

  public Object findAttribute(final List<? extends JPAAttribute<?>> attributes, final String searchItem) {
    for (final JPAAttribute<?> attribute : attributes) {
      if (attribute.getExternalName().equals(searchItem)) {
        return attribute;
      }
    }
    return null;
  }

}