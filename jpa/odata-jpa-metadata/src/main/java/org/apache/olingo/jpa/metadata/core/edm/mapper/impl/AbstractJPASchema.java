package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntitySet;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

public abstract class AbstractJPASchema implements JPAElement {

  private final JPAEdmNameBuilder nameBuilder;

  public AbstractJPASchema(final String namespace) {
    nameBuilder = new JPAEdmNameBuilder(namespace);
  }

  protected final JPAEdmNameBuilder getNameBuilder() {
    return nameBuilder;
  }

  @Override
  public FullQualifiedName getExternalFQN() {
    return nameBuilder.buildFQN(getExternalName());
  }

  @Override
  public String getExternalName() {
    return nameBuilder.buildNamespace();
  }

  @Override
  public String getInternalName() {
    return getExternalName();
  }

  abstract IntermediateEnumType getEnumType(final Class<?> targetClass);

  abstract JPAEntityType getEntityType(final Class<?> targetClass);

  abstract JPAEntityType getEntityType(final String externalName);

  abstract JPAEntitySet getEntitySet(String entitySetName);

  abstract JPAFunction getFunction(final String externalName);

  abstract List<JPAFunction> getFunctions();

  abstract JPAAction getAction(final String externalName);

  abstract List<JPAAction> getActions();

  public abstract CsdlSchema getEdmItem() throws ODataJPAModelException;

  abstract IntermediateEnumType createEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException;

  abstract List<JPAEntityType> getEntityTypes();

  abstract List<IntermediateComplexType> getComplexTypes();

  abstract List<IntermediateEnumType> getEnumTypes();

  abstract List<JPAEntitySet> getEntitySets();
  
  abstract IntermediateComplexType getComplexType(final Class<?> targetClass);
}
