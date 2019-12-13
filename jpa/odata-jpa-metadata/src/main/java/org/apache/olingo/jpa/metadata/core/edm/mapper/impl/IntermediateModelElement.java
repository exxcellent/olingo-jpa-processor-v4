package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAElement;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

abstract class IntermediateModelElement implements JPAElement {

  protected static enum InitializationState {
    NotInitialized, InProgress, Initialized;
  }

  private final JPAEdmNameBuilder nameBuilder;
  private final String internalName;

  private boolean toBeIgnored = false;
  private String externalName;

  public IntermediateModelElement(final JPAEdmNameBuilder nameBuilder, final String internalName) {
    super();
    this.nameBuilder = nameBuilder;
    this.internalName = internalName;
  }

  protected JPAEdmNameBuilder getNameBuilder() {
    return nameBuilder;
  }

  @Override
  public String getExternalName() {
    return externalName;
  }

  @Override
  public FullQualifiedName getExternalFQN() {
    return nameBuilder.buildFQN(getExternalName());
  }

  @Override
  public String getInternalName() {
    return internalName;
  }

  public boolean ignore() {
    return toBeIgnored;
  }

  public void setExternalName(final String externalName) {
    this.externalName = externalName;
  }

  public void setIgnore(final boolean ignore) {
    this.toBeIgnored = ignore;
  }

  protected abstract void lazyBuildEdmItem() throws ODataJPAModelException;

  @SuppressWarnings("unchecked")
  protected static <T> List<?> extractEdmModelElements(
      final Map<String, ? extends IntermediateModelElement> mappingBuffer) throws ODataJPAModelException {
    final List<T> extractionTarget = new ArrayList<T>(mappingBuffer.size());
    for (final String externalName : mappingBuffer.keySet()) {
      if (!((IntermediateModelElement) mappingBuffer.get(externalName)).toBeIgnored) {
        final IntermediateModelElement element = mappingBuffer.get(externalName);
        final CsdlAbstractEdmItem edmElement = element.getEdmItem();
        if (!element.ignore()) {
          extractionTarget.add((T) edmElement);
        }
      }
    }
    return extractionTarget;
    // return returnNullIfEmpty(extractionTarget);
  }

  protected IntermediateModelElement findModelElementByEdmItem(final String edmEntityItemName,
      final Map<String, ?> buffer) throws ODataJPAModelException {
    for (final String internalName : buffer.keySet()) {
      final IntermediateModelElement modelElement = (IntermediateModelElement) buffer.get(internalName);
      if (edmEntityItemName.equals(modelElement.getExternalName())) {
        return modelElement;
      }
    }
    return null;

  }

  protected static <T> List<T> returnNullIfEmpty(final List<T> list) {
    return list == null || list.isEmpty() ? null : list;
  }

  abstract <CDSLType extends CsdlAbstractEdmItem> CDSLType getEdmItem() throws ODataJPAModelException;
}
