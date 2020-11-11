package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * Special schema to provide types not coming from JPA meta model... like
 * enumerations.
 */
class IntermediateCustomSchema extends AbstractJPASchema {

  private CsdlSchema edmSchema = null;
  final private Map<String, IntermediateEnumType> enumTypes = new HashMap<>();
  final private Map<String, IntermediateTypeDTO> dtoTypes = new HashMap<>();
  final private Map<String, IntermediateAction> actions = new HashMap<>();
  final private Map<String, JPAEntitySet> entitySets = new HashMap<>();
  final private IntermediateServiceDocument serviceDocument;

  IntermediateCustomSchema(final IntermediateServiceDocument serviceDocument, final String namespace)
      throws ODataJPAModelException {
    super(namespace);
    this.serviceDocument = serviceDocument;
  }

  @Override
  JPAEntityType getEntityType(final Class<?> targetClass) {
    return dtoTypes.get(getNameBuilder().buildDTOTypeName(targetClass));
  }

  @Override
  IntermediateComplexType getComplexType(final Class<?> targetClass) {
    // currently not supported
    return null;
  }

  @Override
  IntermediateEnumType getEnumType(final Class<?> targetClass) {
    return enumTypes.get(targetClass.getSimpleName());
  }

  @Override
  List<IntermediateComplexType> getComplexTypes() {
    return Collections.emptyList();
  }

  IntermediateTypeDTO getDTOType(final Class<?> targetClass) {
    return dtoTypes.get(getNameBuilder().buildDTOTypeName(targetClass));
  }
  
  protected final void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmSchema != null) {
      return;
    }
    // resolve schema changing types (maybe recursive add new types/enums... to schema)
    buildEntityTypeList();
    buildActionList();

    // take it...
    edmSchema = new CsdlSchema();
    edmSchema.setNamespace(getNameBuilder().buildNamespace());
    edmSchema.setEnumTypes(buildEnumTypeList());
    edmSchema.setEntityTypes(buildEntityTypeList());
    edmSchema.setActions(buildActionList());
  }

  private List<CsdlEntityType> buildEntityTypeList() throws RuntimeException {
    // TODO: entities (=empty) + dto's (as entities)
    return dtoTypes.entrySet().stream().map(x -> {
      try {
        return x.getValue().getEdmItem();
      } catch (final ODataJPAModelException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
  }

  private List<CsdlEnumType> buildEnumTypeList() throws RuntimeException {
    return enumTypes.entrySet().stream().map(x -> {
      try {
        return x.getValue().getEdmItem();
      } catch (final ODataJPAModelException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
  }

  private List<CsdlAction> buildActionList() throws RuntimeException {
    return actions.entrySet().stream().map(x -> {
      try {
        return x.getValue().getEdmItem();
      } catch (final ODataJPAModelException e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.toList());
  }

  @Override
  IntermediateEnumType findOrCreateEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException {
    final String namespace = clazz.getPackage().getName();
    if (!namespace.equalsIgnoreCase(getInternalName())) {
      throw new ODataJPAModelException(MessageKeys.GENERAL);
    }
    IntermediateEnumType enumType = getEnumType(clazz);
    if (enumType == null) {
      enumType = new IntermediateEnumType(getNameBuilder(), clazz, serviceDocument);
      enumTypes.put(clazz.getSimpleName(), enumType);
      // force rebuild
      edmSchema = null;
    }
    return enumType;
  }

  IntermediateTypeDTO createDTOType(final Class<?> clazz) throws ODataJPAModelException {
    final String namespace = clazz.getPackage().getName();
    if (!namespace.equalsIgnoreCase(getInternalName())) {
      throw new ODataJPAModelException(MessageKeys.GENERAL);
    }

    IntermediateTypeDTO dtoType = getDTOType(clazz);
    if (dtoType == null) {
      dtoType = new IntermediateTypeDTO(getNameBuilder(), clazz, serviceDocument);
      dtoTypes.put(dtoType.getExternalName(), dtoType);
      // build actions for DTO
      final IntermediateActionFactory factory = new IntermediateActionFactory();
      actions.putAll(factory.create(getNameBuilder(), dtoType.getTypeClass(), serviceDocument));
      // build entity set
      final IntermediateEntitySet es = new IntermediateEntitySet(dtoType);
      entitySets.put(es.getExternalName(), es);
      // force rebuild
      edmSchema = null;
    }
    return dtoType;
  }

  @Override
  public CsdlSchema getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmSchema;
  }

  @Override
  JPAAction getAction(final EdmAction edmAction) {
    for (final Entry<String, IntermediateAction> entry : actions.entrySet()) {
      if (!entry.getValue().getExternalName().equals(edmAction.getName())) {
        continue;
      }
      try {
        if (entry.getValue().isBound() && !entry.getValue().getEdmItem().getParameters().get(0).getTypeFQN().equals(
            edmAction
                .getBindingParameterTypeFqn())) {
          // for bound actions the 'entity' parameter (parameter[0]) must the same type as from request call
          continue;
        }
      } catch (final ODataJPAModelException e) {
        throw new IllegalStateException(e);
      }

      if (!entry.getValue().ignore()) {
        return entry.getValue();
      }
    }
    return null;
  }

  @Override
  List<JPAAction> getActions() {
    return new ArrayList<JPAAction>(actions.values());
  }

  @Override
  JPAEntityType getEntityType(final String externalName) {
    return dtoTypes.get(externalName);
  }

  @Override
  JPAStructuredType getStructuredType(final Class<?> typeClass) {
    final JPAEntityType eType = getEntityType(typeClass);
    if (eType != null) {
      return eType;
    }
    return getComplexType(typeClass);
  }

  @Override
  JPAFunction getFunction(final String externalName) {
    // currently not supported
    return null;
  }

  @Override
  List<JPAFunction> getFunctions() {
    // currently not supported
    return Collections.emptyList();
  }

  @Override
  List<JPAEntityType> getEntityTypes() {
    return new ArrayList<JPAEntityType>(dtoTypes.values());
  }

  @Override
  JPAEntitySet getEntitySet(final String entitySetName) {
    return entitySets.get(entitySetName);
  }

  @Override
  List<JPAEntitySet> getEntitySets() {
    return new ArrayList<>(entitySets.values());
  }

  @Override
  List<IntermediateEnumType> getEnumTypes() {
    return new ArrayList<IntermediateEnumType>(enumTypes.values());
  }

}
