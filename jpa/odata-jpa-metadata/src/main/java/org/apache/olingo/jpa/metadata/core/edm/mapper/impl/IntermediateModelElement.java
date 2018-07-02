package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmItem;
import org.apache.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateModelItemAccess;

abstract class IntermediateModelElement implements IntermediateModelItemAccess {

	@Deprecated
	protected static JPAEdmMetadataPostProcessor postProcessor = new DefaultEdmPostProcessor();
	protected final JPAEdmNameBuilder nameBuilder;
	protected final String internalName;

	private boolean toBeIgnored;
	private String externalName;

	@Deprecated
	static void setPostProcessor(final JPAEdmMetadataPostProcessor pP) {
		if(pP == null) {
			return;
		}
		postProcessor = pP;
	}

	public IntermediateModelElement(final JPAEdmNameBuilder nameBuilder, final String internalName) {
		super();
		this.nameBuilder = nameBuilder;
		this.internalName = internalName;
	}

	JPAEdmNameBuilder getNameBuilder() {
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.olingo.odata4.jpa.processor.core.edm.mapper.IntermediatModelItem#getInternalName()
	 */
	@Override
	public String getInternalName() {
		return internalName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.olingo.odata4.jpa.processor.core.edm.mapper.IntermediatModelItem#ignore()
	 */
	@Override
	public boolean ignore() {
		return toBeIgnored;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.olingo.odata4.jpa.processor.core.edm.mapper.IntermediatModelItem#setExternalName(java.lang.String)
	 */
	@Override
	public void setExternalName(final String externalName) {
		this.externalName = externalName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.olingo.odata4.jpa.processor.core.edm.mapper.IntermediatModelItem#setIgnore(boolean)
	 */
	@Override
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
				final IntermediateModelElement func = mappingBuffer.get(externalName);
				final CsdlAbstractEdmItem edmFunc = func.getEdmItem();
				if (!func.ignore()) {
					extractionTarget.add((T) edmFunc);
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

	abstract CsdlAbstractEdmItem getEdmItem() throws ODataJPAModelException;
}
