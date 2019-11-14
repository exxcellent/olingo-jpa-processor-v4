package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlStructuralType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.generator.api.client.generatorclassloader.LogWrapper;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public class IntermediateModelGenerator {

  private final IntermediateServiceDocument isd;
  private final LogWrapper log;
  private final File generationBaseDirectory;

  public IntermediateModelGenerator(final IntermediateServiceDocument isd, final LogWrapper log,
      final File generationDirectory) {
    this.isd = isd;
    this.log = log;
    this.generationBaseDirectory = generationDirectory;
  }

  private void generate4Type(final AbstractJPASchema schema, final JPAStructuredType et,
      final boolean generateProtocolCode)
          throws ODataJPAModelException, IOException {
    log.debug("Generate client side type code for " + et.getInternalName() + "...");
    CsdlStructuralType csdlType;
    final List<JPASimpleAttribute> simpleAttributes = et.getAttributes();
    final List<JPAAssociationAttribute> navigationAttributes = et.getAssociations();
    String streamProperty = "";
    boolean isEntity = false;
    if (IntermediateEntityType.class.isInstance(et)) {
      csdlType = IntermediateEntityType.class.cast(et).getEdmItem();
      if (IntermediateEntityType.class.cast(et).hasStream()) {
        streamProperty = IntermediateEntityType.class.cast(et).getStreamAttributePath().getLeaf().getInternalName();
      }
      isEntity = true;
    } else if (IntermediateTypeDTO.class.isInstance(et)) {
      csdlType = IntermediateTypeDTO.class.cast(et).getEdmItem();
      isEntity = true;
    } else if (IntermediateComplexType.class.isInstance(et)) {
      csdlType = IntermediateComplexType.class.cast(et).getEdmItem();
    } else {
      throw new UnsupportedOperationException("Type " + et.getClass().getName() + " isn't supported");
    }

    // META start
    final TypeMetaAPIWriter metaWriter = new TypeMetaAPIWriter(generationBaseDirectory, et);
    metaWriter.writeMetaStart();
    // navigation properties
    for (final CsdlNavigationProperty prop : csdlType.getNavigationProperties()) {
      metaWriter.writeMetaTypeProperty(prop);
    }
    // simple properties
    for (final CsdlProperty prop : csdlType.getProperties()) {
      metaWriter.writeMetaTypeProperty(prop);
    }
    metaWriter.writeMetaEnd();
    // META end

    // DTO start
    final TypeDtoAPIWriter dtoWriter = new TypeDtoAPIWriter(generationBaseDirectory, et.getTypeClass().getPackage()
        .getName(), et.getTypeClass().getSimpleName());
    dtoWriter.writeDtoStart();
    // navigation properties
    for (final JPAAssociationAttribute prop : navigationAttributes) {
      dtoWriter.writeDtoTypeProperty(prop);
    }
    // simple properties
    for (final JPASimpleAttribute prop : simpleAttributes) {
      if (streamProperty.equals(prop.getInternalName())) {
        log.debug("Suppress stream property " + et.getExternalName() + "+" + prop.getInternalName() + " in API");
        continue;
      }
      dtoWriter.writeDtoTypeProperty(prop);
    }
    dtoWriter.writeDtoEnd();
    // DTO end

    // ACCESS start
    if (isEntity && generateProtocolCode) {
      final AccessAPIWriter accessWriter = new AccessAPIWriter(generationBaseDirectory, schema, et);
      accessWriter.writeProtocolCodeStart();
      accessWriter.writeProtocolCode();
      accessWriter.writeProtocolCodeEnd();
    }
    // ACCESS end
  }

  public void generateTypeAPI(final boolean generateProtocolCode) throws IOException, ODataException {
    for (final Map.Entry<AbstractJPASchema, List<JPAEntityType>> entry : getEntityTypes().entrySet()) {
      for (final JPAEntityType et : entry.getValue()) {
        if (et.ignore()) {
          continue;
        }
        generate4Type(entry.getKey(), et, generateProtocolCode);
      }
    }
    for (final Map.Entry<AbstractJPASchema, List<IntermediateComplexType>> entry : getComplexTypes().entrySet()) {
      for (final IntermediateComplexType ct : entry.getValue()) {
        if (ct.ignore()) {
          continue;
        }
        generate4Type(entry.getKey(), ct, generateProtocolCode);
      }
    }

  }

  public void generateEnumAPI() throws IOException, ODataException {
    for (final IntermediateEnumType et : getEnumTypes()) {
      if (et.ignore()) {
        continue;
      }
      if (et.getInternalName().startsWith("java.")) {
        log.debug("Suppress enum data generation for " + et.getInternalName()
        + ", because is available as part of system runtime");
        continue;
      }
      log.debug("Generate client side enum code for " + et.getInternalName() + "...");
      final EnumAPIWriter writer = new EnumAPIWriter(generationBaseDirectory, et.getExternalFQN().getNamespace(), et
          .getExternalFQN().getName());
      writer.writeStart();
      for (final CsdlEnumMember literal : et.getEdmItem().getMembers()) {
        writer.writeLiteral(literal);
      }
      writer.writeEnd();
    }
  }

  private List<IntermediateEnumType> getEnumTypes() {
    final List<IntermediateEnumType> enums = new LinkedList<>();
    final Collection<AbstractJPASchema> schemas = isd.getJPASchemas();
    for (final AbstractJPASchema s : schemas) {
      enums.addAll(s.getEnumTypes());
    }
    return enums;
  }

  private Map<AbstractJPASchema, List<JPAEntityType>> getEntityTypes() {
    final Map<AbstractJPASchema, List<JPAEntityType>> map = new HashMap<>();
    final Collection<AbstractJPASchema> schemas = isd.getJPASchemas();
    for (final AbstractJPASchema s : schemas) {
      final List<JPAEntityType> types = new LinkedList<>();
      types.addAll(s.getEntityTypes());
      map.put(s, types);
    }
    return map;
  }

  private Map<AbstractJPASchema, List<IntermediateComplexType>> getComplexTypes() {
    final Map<AbstractJPASchema, List<IntermediateComplexType>> map = new HashMap<>();
    final Collection<AbstractJPASchema> schemas = isd.getJPASchemas();
    for (final AbstractJPASchema s : schemas) {
      final List<IntermediateComplexType> types = new LinkedList<>();
      types.addAll(s.getComplexTypes());
      map.put(s, types);
    }
    return map;
  }
}
