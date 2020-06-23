package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * <p>
 * For details about Schema metadata see:
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397946"
 * >OData Version 4.0 Part 3 - 5 Schema </a>
 *
 * @author Oliver Grande
 *
 */
class IntermediateMetamodelSchema extends AbstractJPASchema {

  private final static Logger LOGGER = Logger.getLogger(IntermediateMetamodelSchema.class.getName());

  final private IntermediateServiceDocument serviceDocument;
  final private Metamodel jpaMetamodel;
  final private Map<String, IntermediateComplexType> mapInternalName2ComplexType;
  final private Map<String, IntermediateEntityType> mapInternalName2EntityType;
  final private Map<String, IntermediateFunction> mapeInternalName2Function;
  final private Map<String, IntermediateAction> mapInternalName2Action;
  final private Map<String, JPAEntitySet> mapExternalName2EntitySet;
  private CsdlSchema edmSchema = null;

  IntermediateMetamodelSchema(final IntermediateServiceDocument serviceDocument, final String namespace,
      final Metamodel jpaMetamodel) throws ODataJPAModelException {
    super(namespace);
    this.serviceDocument = serviceDocument;
    this.jpaMetamodel = jpaMetamodel;
    this.mapInternalName2ComplexType = buildComplexTypeList();
    this.mapInternalName2EntityType = buildEntityTypeList();
    this.mapeInternalName2Function = buildFunctionList();
    this.mapInternalName2Action = buildActionList();
    this.mapExternalName2EntitySet = buildEntitySetList(mapInternalName2EntityType.values());
  }

  @SuppressWarnings("unchecked")
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmSchema != null) {
      return;
    }
    edmSchema = new CsdlSchema();
    edmSchema.setNamespace(getNameBuilder().buildNamespace());
    edmSchema.setComplexTypes(
        (List<CsdlComplexType>) IntermediateModelElement.extractEdmModelElements(mapInternalName2ComplexType));
    edmSchema.setEntityTypes(
        (List<CsdlEntityType>) IntermediateModelElement.extractEdmModelElements(mapInternalName2EntityType));
    edmSchema.setFunctions(
        (List<CsdlFunction>) IntermediateModelElement.extractEdmModelElements(mapeInternalName2Function));
    edmSchema
    .setActions((List<CsdlAction>) IntermediateModelElement.extractEdmModelElements(mapInternalName2Action));

    // edm:Annotations
    // edm:Annotation
    // edm:EnumType --> Annotation @Enummerated (see IntermediateCustomSchema)
    // edm:Term
    // edm:TypeDefinition
    // MUST be the last thing that is done !!!!
    // REMARK: the entity container is set outside (in
    // IntermediateServiceDocument#getEdmSchemas()) for related schemas only
  }

  @Override
  public CsdlSchema getEdmItem() throws ODataJPAModelException {
    lazyBuildEdmItem();
    return edmSchema;
  }

  private static String buildStructuredTypeInternalName(final Class<?> clazz) {
    return clazz.getCanonicalName();
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
  JPAEntityType getEntityType(final Class<?> targetClass) {
    return mapInternalName2EntityType.get(buildStructuredTypeInternalName(targetClass));
  }

  @Override
  IntermediateComplexType getComplexType(final Class<?> targetClass) {
    return mapInternalName2ComplexType.get(buildStructuredTypeInternalName(targetClass));
  }

  @Override
  JPAEntityType getEntityType(final String externalName) {
    for (final String internalName : mapInternalName2EntityType.keySet()) {
      if (mapInternalName2EntityType.get(internalName).getExternalName().equals(externalName)) {
        return mapInternalName2EntityType.get(internalName);
      }
    }
    return null;
  }

  @Override
  List<JPAEntityType> getEntityTypes() {
    final List<JPAEntityType> entityTypes = new ArrayList<JPAEntityType>(
        mapInternalName2EntityType.size());
    entityTypes.addAll(mapInternalName2EntityType.values());
    return entityTypes;
  }

  @Override
  JPAFunction getFunction(final String externalName) {
    for (final String internalName : mapeInternalName2Function.keySet()) {
      if (mapeInternalName2Function.get(internalName).getExternalName().equals(externalName)) {
        if (!mapeInternalName2Function.get(internalName).ignore()) {
          return mapeInternalName2Function.get(internalName);
        }
      }
    }
    return null;
  }

  @Override
  List<JPAFunction> getFunctions() {
    final ArrayList<JPAFunction> functions = new ArrayList<JPAFunction>(mapeInternalName2Function.size());
    for (final String internalName : mapeInternalName2Function.keySet()) {
      functions.add(mapeInternalName2Function.get(internalName));
    }
    return functions;
  }

  @Override
  JPAAction getAction(final String externalName) {
    for (final Entry<String, IntermediateAction> entry : mapInternalName2Action.entrySet()) {
      if (!entry.getValue().getExternalName().equals(externalName)) {
        continue;
      }
      if (entry.getValue().ignore()) {
        LOGGER.log(Level.WARNING, "Attempted call to ignored action '"
            + entry.getValue().getJavaMethod().getName() + "'... Reject!");
        return null;
      }
      return entry.getValue();
    }
    return null;
  }

  @Override
  List<JPAAction> getActions() {
    return new ArrayList<JPAAction>(mapInternalName2Action.values());
  }

  private Map<String, IntermediateComplexType> buildComplexTypeList() throws ODataJPAModelException {
    final HashMap<String, IntermediateComplexType> ctList = new HashMap<String, IntermediateComplexType>();

    for (final EmbeddableType<?> embeddable : this.jpaMetamodel.getEmbeddables()) {
      final IntermediateComplexType ct = new IntermediateComplexType(getNameBuilder(), embeddable,
          serviceDocument);
      ctList.put(ct.getInternalName(), ct);
    }
    return ctList;
  }

  private Map<String, JPAEntitySet> buildEntitySetList(final Collection<IntermediateEntityType> entities)
      throws ODataJPAModelException {
    final HashMap<String, JPAEntitySet> esList = new HashMap<>();

    for (final IntermediateEntityType entity : entities) {
      if (esList.containsKey(entity.getEntitySetName())) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.UNIQUE_NAME_VIOLATION, entity
            .getEntitySetName());
      }
      final IntermediateEntitySet es = new IntermediateEntitySet(entity);
      esList.put(es.getExternalName(), es);
    }
    return esList;
  }

  private Map<String, IntermediateEntityType> buildEntityTypeList() throws ODataJPAModelException {
    final HashMap<String, IntermediateEntityType> etList = new HashMap<String, IntermediateEntityType>();

    for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {
      final IntermediateEntityType et = new IntermediateEntityType(getNameBuilder(), entity, serviceDocument);
      etList.put(et.getInternalName(), et);
    }
    return etList;
  }

  private Map<String, IntermediateFunction> buildFunctionList() throws ODataJPAModelException {
    final HashMap<String, IntermediateFunction> funcList = new HashMap<String, IntermediateFunction>();
    // 1. Option: Create Function from Entity Annotations
    final IntermediateFunctionFactory factory = new IntermediateFunctionFactory();
    for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {

      funcList.putAll(factory.create(getNameBuilder(), entity, serviceDocument));
    }
    return funcList;
  }

  private Map<String, IntermediateAction> buildActionList() throws ODataJPAModelException {
    final Map<String, IntermediateAction> actionList = new HashMap<String, IntermediateAction>();
    // 1. Option: Create Action from Entity Annotations
    final IntermediateActionFactory factory = new IntermediateActionFactory();
    for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {
      actionList.putAll(factory.create(getNameBuilder(), entity.getJavaType(), serviceDocument));
    }
    return actionList;
  }

  @Override
  IntermediateEnumType getEnumType(final Class<?> targetClass) {
    return null;
  }

  @Override
  IntermediateEnumType findOrCreateEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException {
    // not supported in JPA models
    throw new ODataJPAModelException(MessageKeys.INVALID_ENTITY_TYPE);
  }

  @Override
  JPAEntitySet getEntitySet(final String entitySetName) {
    return mapExternalName2EntitySet.get(entitySetName);
  }

  @Override
  List<JPAEntitySet> getEntitySets() {
    return new ArrayList<>(mapExternalName2EntitySet.values());
  }
}
