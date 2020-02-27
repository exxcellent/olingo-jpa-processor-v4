package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/*
 * http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/schemas/edmx.xsd
 * A Service Document can contain of multiple schemas, but only of
 * one Entity Container. This container is assigned to one of the
 * schemas.
 * @see http://services.odata.org/V4/Northwind/Northwind.svc/$metadata
 * @see org.apache.olingo.client.api.data.ServiceDocument
 */
public class IntermediateServiceDocument {
  private final Object lock = new Object();
  private final Map<String, AbstractJPASchema> schemaListInternalKey = new HashMap<>();
  private boolean dependendSchemaCreationRequired = false;
  private final IntermediateEntityContainer intermediateContainer;

  /**
   *
   * @param namespaceDefault
   *            The name space used by the entity container. The name space should
   *            identify the persistence unit containing all the entity sets.
   */
  public IntermediateServiceDocument(final String namespaceDefault) throws ODataJPAModelException {
    super();
    this.intermediateContainer = new IntermediateEntityContainer(new JPAEdmNameBuilder(namespaceDefault), this);
  }

  private void invokeEverySchema() {
    final List<AbstractJPASchema> existingSchemas = new ArrayList<>(schemaListInternalKey.values());
    for (final AbstractJPASchema schema : existingSchemas) {
      if (schema instanceof IntermediateMetamodelSchema) {
        // only schemas working on javax.persistence.metamodel.Metamodel can be source
        // of additional custom schemas
        try {
          schema.getEdmItem();
        } catch (final ODataJPAModelException e) {
          throw new IllegalStateException(e);
        }
      }
    }
  }

  private final void resolveSchemas() {
    synchronized (lock) {
      if (!dependendSchemaCreationRequired) {
        return;
      }
      // prevent recursive calls
      dependendSchemaCreationRequired = false;

      // we have to do something very tricky/dirty:
      // some custom schemas are created on demand while traversing the metamodel from
      // JPA
      // so we have trigger here the creation of all meta informations (including schema creation)
      invokeEverySchema();
      // recursive second strike to touch also new (on demand) created schemas
      invokeEverySchema();
    }
  }

  /**
   *
   * @return The only entity container of OData service.
   */
  public CsdlEntityContainer getEntityContainer() throws ODataJPAModelException {
    return intermediateContainer.getEdmItem();
  }

  public CsdlEntityContainerInfo getEntityContainerInfo() {
    return new CsdlEntityContainerInfo().setContainerName(intermediateContainer.getExternalFQN());
  }

  public List<CsdlSchema> getEdmSchemas() throws ODataJPAModelException {
    final List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
    synchronized (lock) {
      resolveSchemas();
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        // assign entity container to schema... only to the schema of same name space as
        // in entity container (simply to reduce complexity of meta data)
        final CsdlSchema cdslSchema = schema.getEdmItem();
        if (cdslSchema.getNamespace().equals(intermediateContainer.getExternalFQN().getNamespace())) {
          cdslSchema.setEntityContainer(intermediateContainer.getEdmItem());
        }
        schemas.add(cdslSchema);
      }
    }
    return schemas;
  }

  Collection<AbstractJPASchema> getJPASchemas() {
    synchronized (lock) {
      resolveSchemas();
      return schemaListInternalKey.values();
    }
  }

  public JPAEntityType getEntityType(final EdmType edmType) {
    synchronized (lock) {
      resolveSchemas();

      final AbstractJPASchema schema = schemaListInternalKey.get(edmType.getNamespace());
      if (schema != null) {
        return schema.getEntityType(edmType.getName());
      }
    }
    return null;
  }

  /**
   *
   * @return The entity type based on given external (OData) related full qualified name.
   */
  public JPAEntityType getEntityType(final FullQualifiedName typeName) {
    synchronized (lock) {
      resolveSchemas();
      final AbstractJPASchema schema = schemaListInternalKey.get(typeName.getNamespace());
      if (schema != null) {
        return schema.getEntityType(typeName.getName());
      }
    }
    return null;
  }

  public JPAEntityType getEntityType(final String edmEntitySetName) throws ODataJPAModelException {
    synchronized (lock) {
      // do not resolve the on-demand schemas here; we have to avoid recursion
      // problems and want to optimize performance
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        final JPAEntitySet es = schema.getEntitySet(edmEntitySetName);
        if (es != null) {
          return es.getEntityType();
        }
      }
    }
    return null;
  }

  public JPAFunction getFunction(final EdmFunction function) {
    synchronized (lock) {
      resolveSchemas();
      final AbstractJPASchema schema = schemaListInternalKey.get(function.getNamespace());
      if (schema != null) {
        return schema.getFunction(function.getName());
      }
    }
    return null;
  }

  public JPAAction getAction(final EdmAction action) {
    synchronized (lock) {
      resolveSchemas();
      final AbstractJPASchema schema = schemaListInternalKey.get(action.getNamespace());
      if (schema != null) {
        return schema.getAction(action.getName());
      }
    }
    return null;
  }

  /**
   * @see AbstractJPASchema#getStructuredType(Attribute)
   */
  IntermediateStructuredType<?> getStructuredType(final Attribute<?, ?> jpaAttribute) {
    synchronized (lock) {
      // do not resolve the on-demand schemas here; we have to avoid recursion
      // problems and want to optimize performance
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        // only the JPA meta model based schema can have the requested type of attribute
        if (!IntermediateMetamodelSchema.class.isInstance(schema)) {
          continue;
        }
        final IntermediateStructuredType<?> structuredType = IntermediateMetamodelSchema.class.cast(schema)
            .getStructuredType(jpaAttribute);
        if (structuredType != null) {
          return structuredType;
        }
      }
    }
    return null;
  }

  JPAEntityType getEntityType(final Class<?> targetClass) {
    JPAEntityType entityType;
    if (Object.class.equals(targetClass)) {
      return null;
    }
    synchronized (lock) {
      resolveSchemas();
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        entityType = schema.getEntityType(targetClass);
        if (entityType != null) {
          return entityType;
        }
      }
    }
    return null;
  }

  IntermediateEnumType getEnumType(final Class<?> targetClass) {
    IntermediateEnumType enumType;
    synchronized (lock) {
      resolveSchemas();
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        enumType = schema.getEnumType(targetClass);
        if (enumType != null) {
          return enumType;
        }
      }
    }
    return null;
  }

  // TODO remove method from public API
  public AbstractJPASchema createMetamodelSchema(final String namespace, final Metamodel jpaMetamodel)
      throws ODataJPAModelException {
    synchronized (lock) {
      if (schemaListInternalKey.containsKey(namespace)) {
        throw new ODataJPAModelException(MessageKeys.GENERAL);
      }
      final IntermediateMetamodelSchema schema = new IntermediateMetamodelSchema(this, namespace,
          jpaMetamodel);
      schemaListInternalKey.put(namespace, schema);
      dependendSchemaCreationRequired = true;
      return schema;
    }
  }

  private IntermediateCustomSchema createCustomSchema(final String namespace) throws ODataJPAModelException {
    if (schemaListInternalKey.containsKey(namespace)) {
      throw new ODataJPAModelException(MessageKeys.GENERAL);
    }
    final IntermediateCustomSchema schema = new IntermediateCustomSchema(this, namespace);
    schemaListInternalKey.put(namespace, schema);
    return schema;
  }

  IntermediateEnumType createEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException {
    synchronized (lock) {
      final String namespace = clazz.getPackage().getName();
      AbstractJPASchema schema = schemaListInternalKey.get(namespace);
      if(schema == null) {
        schema = createCustomSchema(namespace);
      }
      return schema.createEnumType(clazz);
    }
  }

  public IntermediateTypeDTO createDTOType(final Class<?> clazz) throws ODataJPAModelException {
    synchronized (lock) {
      if (clazz == null) {
        throw new ODataJPAModelException(MessageKeys.GENERAL);
      }
      final String namespace = clazz.getPackage().getName();
      AbstractJPASchema schema = schemaListInternalKey.get(namespace);
      if (schema == null) {
        schema = createCustomSchema(namespace);
      } else if (!IntermediateCustomSchema.class.isInstance(schema)) {
        // DTO's can be defined only in custom schemas
        throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM);
      }
      // this will affect the number of entity set's so we have to refresh the container
      intermediateContainer.reset();
      return ((IntermediateCustomSchema) schema).createDTOType(clazz);
    }
  }

  public JPAElement getEntitySet(final JPAEntityType entityType) throws ODataJPAModelException {
    synchronized (lock) {
      resolveSchemas();
      for (final AbstractJPASchema schema : schemaListInternalKey.values()) {
        final JPAEntitySet es = schema.getEntitySet(entityType.getEntitySetName());
        if (es != null) {
          return es;
        }
      }
    }
    return null;
  }

}
