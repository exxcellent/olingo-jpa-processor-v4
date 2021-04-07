package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAComplexType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * Special schema to provide types not coming from JPA meta model... like
 * enumerations.
 */
class IntermediateCustomSchema extends AbstractJPASchema {

  final private static Logger LOGGER = Logger.getLogger(IntermediateCustomSchema.class.getName());

  private static boolean mapWarningAlreadyLogged = false;

  final private Map<String, IntermediateEnumType> enumTypes = new TreeMap<>();
  final private Map<String, IntermediateEnityTypeDTO> dtoTypes = new TreeMap<>();
  final private Map<String, AbstractIntermediateComplexTypeDTO> complexTypes = new TreeMap<>();
  final private Map<String, IntermediateAction> actions = new TreeMap<>();
  final private Map<String, JPAEntitySet> entitySets = new TreeMap<>();
  final private IntermediateServiceDocument serviceDocument;
  private CsdlSchema edmSchema = null;
  private int dtCount = 0;

  IntermediateCustomSchema(final IntermediateServiceDocument serviceDocument, final String namespace)
      throws ODataJPAModelException {
    super(namespace);
    this.serviceDocument = serviceDocument;
  }

  @Override
  JPAComplexType getComplexType(final Class<?> targetClass) {
    return getComplexType(getNameBuilder().buildComplexTypeName(targetClass));
  }

  @Override
  JPAComplexType getComplexType(final String externalName) {
    return complexTypes.get(externalName);
  }

  @Override
  IntermediateEnumType getEnumType(final Class<?> targetClass) {
    return enumTypes.get(targetClass.getSimpleName());
  }

  @Override
  List<JPAComplexType> getComplexTypes() {
    return new ArrayList<JPAComplexType>(complexTypes.values());
  }

  IntermediateEnityTypeDTO getDTOType(final Class<?> targetClass) {
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
    edmSchema.setComplexTypes(buildComplexTypeList());
    edmSchema.setEntityTypes(buildEntityTypeList());
    edmSchema.setActions(buildActionList());
  }

  private List<CsdlEntityType> buildEntityTypeList() throws RuntimeException {
    // TODO: entities (=empty) + dto's (as entities)
    return dtoTypes.entrySet().stream().map(x -> x.getValue().getEdmItem()).collect(Collectors.toList());
  }

  private List<CsdlComplexType> buildComplexTypeList() throws RuntimeException {
    return complexTypes.entrySet().stream().map(x -> x.getValue().getEdmItem()).collect(Collectors.toList());
  }

  private List<CsdlEnumType> buildEnumTypeList() throws RuntimeException {
    return enumTypes.entrySet().stream().map(x -> x.getValue().getEdmItem()).collect(Collectors.toList());
  }

  private List<CsdlAction> buildActionList() throws RuntimeException {
    return actions.entrySet().stream().map(x -> x.getValue().getEdmItem()).collect(Collectors.toList());
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
      enumTypes.put(enumType.getExternalName(), enumType);
      // force rebuild
      edmSchema = null;
    }
    return enumType;
  }

  /**
   * A call to this method will always create an new complex type. The name of type is generated, based on map type, but
   * with suffix.
   * The map may be part of an schema (namespace), but as representation for {@link java.util.Map} the related namespace
   * should be used.
   */
  AbstractIntermediateComplexTypeDTO createDynamicMapType(final Class<?> mapKeyType, final Class<?> mapValueType,
      final boolean valueIsCollection) throws ODataJPAModelException {
    final String simpleName = Map.class.getSimpleName() + "{" + Integer.toString(++dtCount) + "}";
    AbstractIntermediateComplexTypeDTO mapType = complexTypes.get(simpleName);
    if (mapType != null)
    {
      throw new ODataJPAModelException(MessageKeys.RUNTIME_PROBLEM);
    }
    // define a Map DTO type... the map will have no attributes (mostly EdmUntyped), because all attributes are dynamic.
    mapType = new IntermediateMapComplexTypeDTO(getNameBuilder(), simpleName, mapKeyType, mapValueType,
        valueIsCollection);
    if (!mapWarningAlreadyLogged) {
      mapWarningAlreadyLogged = true;
      LOGGER.info("The type " + Map.class.getCanonicalName()
          + " was created as complex open type. Open types are not supported by Olingo's (de)serializer, so a custom (de)serializer by OData-JPA-Adapter must be used. There is only JSON supported!");
    }
    complexTypes.put(mapType.getExternalName(), mapType);
    // force rebuild
    edmSchema = null;
    return mapType;
  }

  IntermediateEnityTypeDTO findOrCreateDTOType(final Class<?> clazz) throws ODataJPAModelException {
    final String namespace = clazz.getPackage().getName();
    if (!namespace.equalsIgnoreCase(getInternalName())) {
      throw new ODataJPAModelException(MessageKeys.GENERAL);
    }

    IntermediateEnityTypeDTO dtoType = getDTOType(clazz);
    if (dtoType == null) {
      dtoType = new IntermediateEnityTypeDTO(getNameBuilder(), clazz, serviceDocument);
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
  public CsdlSchema getEdmItem() throws ODataRuntimeException {
    try {
      lazyBuildEdmItem();
    } catch (final ODataJPAModelException e) {
      throw new ODataRuntimeException(e);
    }
    return edmSchema;
  }

  @Override
  JPAAction getAction(final EdmAction edmAction) {
    for (final Entry<String, IntermediateAction> entry : actions.entrySet()) {
      if (!entry.getValue().getExternalName().equals(edmAction.getName())) {
        continue;
      }
      if (entry.getValue().isBound() && !entry.getValue().getEdmItem().getParameters().get(0).getTypeFQN().equals(
          edmAction
          .getBindingParameterTypeFqn())) {
        // for bound actions the 'entity' parameter (parameter[0]) must the same type as from request call
        continue;
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
  JPAEntityType getEntityType(final Class<?> targetClass) {
    return getEntityType(getNameBuilder().buildDTOTypeName(targetClass));
  }

  @Override
  JPAEntityType getEntityType(final String externalName) {
    return dtoTypes.get(externalName);
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
