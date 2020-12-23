package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
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
  final private Map<String, IntermediateComplexTypeJPA> mapExternalName2ComplexType;
  final private Map<String, IntermediateEntityTypeJPA> mapExternalName2EntityType;
  final private Map<String, IntermediateFunction> mapeExternalName2Function;
  final private List<IntermediateAction> collectionActions;
  final private Map<String, JPAEntitySet> mapExternalName2EntitySet;
  private CsdlSchema edmSchema = null;

  IntermediateMetamodelSchema(final IntermediateServiceDocument serviceDocument, final String namespace,
      final Metamodel jpaMetamodel) throws ODataJPAModelException {
    super(namespace);
    this.serviceDocument = serviceDocument;
    this.jpaMetamodel = jpaMetamodel;
    this.mapExternalName2ComplexType = buildComplexTypeList();
    this.mapExternalName2EntityType = buildEntityTypeList();
    this.mapeExternalName2Function = buildFunctionList();
    this.collectionActions = buildActionList();
    this.mapExternalName2EntitySet = buildEntitySetList(mapExternalName2EntityType.values());
  }

  @SuppressWarnings("unchecked")
  protected void lazyBuildEdmItem() throws ODataJPAModelException {
    if (edmSchema != null) {
      return;
    }
    edmSchema = new CsdlSchema();
    edmSchema.setNamespace(getNameBuilder().buildNamespace());
    edmSchema.setComplexTypes(
        (List<CsdlComplexType>) IntermediateModelElement.extractEdmModelElements(mapExternalName2ComplexType));
    edmSchema.setEntityTypes(
        (List<CsdlEntityType>) IntermediateModelElement.extractEdmModelElements(mapExternalName2EntityType));
    edmSchema.setFunctions(
        (List<CsdlFunction>) IntermediateModelElement.extractEdmModelElements(mapeExternalName2Function));
    edmSchema.setActions(collectionActions.stream().map(entry -> entry.getEdmItem()).collect(Collectors.toList()));

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

  @Override
  IntermediateComplexTypeJPA getComplexType(final Class<?> targetClass) {
    return mapExternalName2ComplexType.get(getNameBuilder().buildComplexTypeName(targetClass));
  }

  @Override
  JPAComplexType getComplexType(final String externalName) {
    return mapExternalName2ComplexType.get(externalName);
  }

  @Override
  List<JPAComplexType> getComplexTypes() {
    return new ArrayList<>(mapExternalName2ComplexType.values());
  }

  @Override
  JPAEntityType getEntityType(final Class<?> targetClass) {
    // an JPA entity can have a complete different name...
    for (final IntermediateEntityTypeJPA jpaEntity : mapExternalName2EntityType.values()) {
      if (jpaEntity.getManagedType().getJavaType().equals(targetClass)) {
        return jpaEntity;
      }
    }
    return null;
  }

  @Override
  JPAEntityType getEntityType(final String externalName) {
    return mapExternalName2EntityType.get(externalName);
  }

  @Override
  List<JPAEntityType> getEntityTypes() {
    return new ArrayList<JPAEntityType>(mapExternalName2EntityType.values());
  }

  @Override
  JPAFunction getFunction(final String externalName) {
    return mapeExternalName2Function.get(externalName);
  }

  @Override
  List<JPAFunction> getFunctions() {
    return new ArrayList<JPAFunction>(mapeExternalName2Function.values());
  }

  @Override
  JPAAction getAction(final EdmAction edmAction) {
    for (final IntermediateAction jpaAction : collectionActions) {
      if (!jpaAction.getExternalName().equals(edmAction.getName())) {
        continue;
      }
      if (jpaAction.isBound() && !jpaAction.getEdmItem().getParameters().get(0).getTypeFQN().equals(edmAction
          .getBindingParameterTypeFqn())) {
        // for bound actions the 'entity' parameter (parameter[0]) must the same type as from request call
        continue;
      }
      if (jpaAction.ignore()) {
        LOGGER.log(Level.WARNING, "Attempted call to ignored action '" + jpaAction.getJavaMethod().getName()
            + "'... Reject!");
        return null;
      }
      return jpaAction;
    }
    return null;
  }

  @Override
  List<JPAAction> getActions() {
    return Collections.unmodifiableList(collectionActions);
  }

  private Map<String, IntermediateComplexTypeJPA> buildComplexTypeList() throws ODataJPAModelException {
    final HashMap<String, IntermediateComplexTypeJPA> ctList = new HashMap<String, IntermediateComplexTypeJPA>();

    for (final EmbeddableType<?> embeddable : this.jpaMetamodel.getEmbeddables()) {
      final IntermediateComplexTypeJPA ct = new IntermediateComplexTypeJPA(getNameBuilder(), embeddable,
          serviceDocument);
      ctList.put(ct.getExternalName(), ct);
    }
    return ctList;
  }

  private Map<String, JPAEntitySet> buildEntitySetList(final Collection<IntermediateEntityTypeJPA> entities)
      throws ODataJPAModelException {
    final HashMap<String, JPAEntitySet> esList = new HashMap<>();

    for (final IntermediateEntityTypeJPA entity : entities) {
      if (esList.containsKey(entity.getEntitySetName())) {
        throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.UNIQUE_NAME_VIOLATION, entity
            .getEntitySetName());
      }
      final IntermediateEntitySet es = new IntermediateEntitySet(entity);
      esList.put(es.getExternalName(), es);
    }
    return esList;
  }

  private Map<String, IntermediateEntityTypeJPA> buildEntityTypeList() throws ODataJPAModelException {
    final HashMap<String, IntermediateEntityTypeJPA> etMap = new HashMap<String, IntermediateEntityTypeJPA>();

    for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {
      final IntermediateEntityTypeJPA et = new IntermediateEntityTypeJPA(getNameBuilder(), entity, serviceDocument);
      etMap.put(et.getExternalName(), et);
    }
    return etMap;
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

  private List<IntermediateAction> buildActionList() throws ODataJPAModelException {
    final Map<String, IntermediateAction> actionList = new HashMap<String, IntermediateAction>();
    // 1. Option: Create Action from Entity Annotations
    final IntermediateActionFactory factory = new IntermediateActionFactory();
    for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {
      final Map<? extends String, ? extends IntermediateAction> moreActions = factory.create(getNameBuilder(), entity
          .getJavaType(), serviceDocument);
      for (final Entry<? extends String, ? extends IntermediateAction> entry : moreActions.entrySet()) {
        if (actionList.containsKey(entry.getKey())) {
          throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.UNIQUE_NAME_VIOLATION, entry.getKey());
        }
        actionList.put(entry.getKey(), entry.getValue());
      }
    }
    return new ArrayList<>(actionList.values());
  }

  @Override
  List<IntermediateEnumType> getEnumTypes() {
    return Collections.emptyList();
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
