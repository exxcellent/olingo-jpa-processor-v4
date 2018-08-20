package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.IdentifiableType;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397974"
 * >OData Version 4.0 Part 3 - 8 Entity Type</a>
 * @author Oliver Grande
 *
 */
class IntermediateEntityType extends IntermediateStructuredType implements JPAEntityType {
	private CsdlEntityType edmEntityType;
	private boolean hasEtag = false;

	IntermediateEntityType(final JPAEdmNameBuilder nameBuilder, final EntityType<?> et,
			final IntermediateServiceDocument serviceDocument)
					throws ODataJPAModelException {
		super(nameBuilder, et, serviceDocument);
		this.setExternalName(nameBuilder.buildEntityTypeName(et));
		final EdmIgnore jpaIgnore = ((AnnotatedElement) this.jpaManagedType.getJavaType()).getAnnotation(
				EdmIgnore.class);
		if (jpaIgnore != null) {
			this.setIgnore(true);
		}
	}

	@Override
	public String getContentType() throws ODataJPAModelException {
		final IntermediateProperty stream = getStreamProperty();
		return stream.getContentType();
	}

	@Override
	public List<JPAAttributePath> getKeyPath() throws ODataJPAModelException {
		lazyBuildEdmItem();

		final List<JPAAttributePath> result = new ArrayList<JPAAttributePath>();
		for (final String internalName : this.declaredPropertiesList.keySet()) {
			final JPAAttribute attribute = this.declaredPropertiesList.get(internalName);
			if (attribute instanceof IntermediateEmbeddedIdProperty) {
				result.add(complexAttributePathMap.get(attribute.getExternalName()));
			} else if (attribute.isKey()) {
				result.add(simpleAttributePathMap.get(attribute.getExternalName()));
			}
		}
		final JPAStructuredType baseType = getBaseType();
		if (baseType != null) {
			result.addAll(((IntermediateEntityType) baseType).getKeyPath());
		}
		return result;
	}

	@Override
	public Class<?> getKeyType() {
		if (jpaManagedType instanceof IdentifiableType<?>) {
			return ((IdentifiableType<?>) jpaManagedType).getIdType().getJavaType();
		} else {
			return null;
		}
	}

	@Override
	public List<JPASelector> getSearchablePath() throws ODataJPAModelException {
		final List<JPASelector> allPath = getPathList();
		final List<JPASelector> searchablePath = new ArrayList<JPASelector>();
		for (final JPASelector p : allPath) {
			if (p.getLeaf().isSearchable()) {
				searchablePath.add(p);
			}
		}
		return searchablePath;
	}

	@Override
	public JPAAttributePath getStreamAttributePath() throws ODataJPAModelException {
		return (JPAAttributePath) getPath(getStreamProperty().getExternalName());
	}

	@Override
	public JPASelector getContentTypeAttributePath() throws ODataJPAModelException {
		final String propertyInternalName = getStreamProperty().getContentTypeProperty();
		if (propertyInternalName == null || propertyInternalName.isEmpty()) {
			return null;
		}
		// Ensure that Ignore is ignored
		return getPathByDBField(getProperty(propertyInternalName).getDBFieldName());
	}

	@Override
	public String getTableName() {
		final AnnotatedElement a = jpaManagedType.getJavaType();
		Table t = null;

		if (a != null) {
			t = a.getAnnotation(Table.class);
		}

		return (t == null) ? jpaManagedType.getJavaType().getName().toUpperCase(Locale.ENGLISH)
				: t.name();
	}

	@Override
	public boolean hasStream() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return this.determineHasStream();
	}

	@Override
	public boolean hasEtag() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return hasEtag;
	};

	@Override
	public List<JPAAttributePath> searchChildPath(final JPASelector selectItemPath) {
		final List<JPAAttributePath> result = new ArrayList<JPAAttributePath>();
		for (final String pathName : this.simpleAttributePathMap.keySet()) {
			final JPAAttributePath p = simpleAttributePathMap.get(pathName);
			if (!p.ignore() && p.getAlias().startsWith(selectItemPath.getAlias())) {
				result.add(p);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	protected <T> List<?> extractEdmProperties(final Map<String, ? extends IntermediateModelElement> mappingBuffer)
			throws ODataJPAModelException {
		final List<T> extractionTarget = new LinkedList<T>();
		for (final String externalName : mappingBuffer.keySet()) {
			if (!((IntermediateModelElement) mappingBuffer.get(externalName)).ignore()
					// Skip Streams
					&& !(mappingBuffer.get(externalName) instanceof IntermediateProperty &&
							((IntermediateProperty) mappingBuffer.get(externalName)).isStream())) {
				if (mappingBuffer.get(externalName) instanceof IntermediateEmbeddedIdProperty) {
					extractionTarget.addAll((Collection<? extends T>) resolveEmbeddedId(
							(IntermediateEmbeddedIdProperty) mappingBuffer.get(externalName)));
				} else {
					extractionTarget.add((T) ((IntermediateModelElement) mappingBuffer.get(externalName)).getEdmItem());
				}
			}
		}
		return extractionTarget;
		// return returnNullIfEmpty(extractionTarget);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmEntityType == null) {
			buildPropertyList();
			edmEntityType = new CsdlEntityType();
			edmEntityType.setName(getExternalName());
			edmEntityType.setProperties((List<CsdlProperty>) extractEdmProperties(declaredPropertiesList));
			edmEntityType.setNavigationProperties(
					(List<CsdlNavigationProperty>) extractEdmProperties(
							declaredNaviPropertiesList));
			edmEntityType.setKey(extractEdmKeyElements(declaredPropertiesList));
			edmEntityType.setAbstract(isAbstract());
			edmEntityType.setBaseType(determineBaseType());
			edmEntityType.setHasStream(determineHasStream());
			determineHasEtag();
			// TODO determine OpenType

		}
	}

	void determineHasEtag() {
		for (final String internalName : this.declaredPropertiesList.keySet()) {
			if (declaredPropertiesList.get(internalName).isEtag()) {
				hasEtag = true;
			}
		}
	}

	/**
	 * Creates the key of an entity. In case the POJO is declared with an embedded
	 * ID the key fields get resolved, so that they occur as separate properties
	 * within the metadata document
	 *
	 * @param propertyList
	 * @return
	 * @throws ODataJPAModelException
	 */
	List<CsdlPropertyRef> extractEdmKeyElements(final Map<String, IntermediateProperty> propertyList)
			throws ODataJPAModelException {
		// TODO setAlias
		final List<CsdlPropertyRef> keyList = new ArrayList<CsdlPropertyRef>();
		for (final String internalName : propertyList.keySet()) {
			if (propertyList.get(internalName).isKey()) {
				if (propertyList.get(internalName).isComplex()) {
					final List<JPASimpleAttribute> idAttributes = ((IntermediateComplexType) propertyList
							.get(internalName)
							.getStructuredType())
							.getAttributes();
					for (final JPAAttribute idAttribute : idAttributes) {
						final CsdlPropertyRef key = new CsdlPropertyRef();
						key.setName(idAttribute.getExternalName());
						keyList.add(key);
					}
				} else {
					final CsdlPropertyRef key = new CsdlPropertyRef();
					key.setName(propertyList.get(internalName).getExternalName());
					keyList.add(key);
				}
			}
		}
		return returnNullIfEmpty(keyList);
	}

	@Override
	public CsdlEntityType getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmEntityType;
	}

	private <T> List<?> resolveEmbeddedId(final IntermediateEmbeddedIdProperty embeddedId) throws ODataJPAModelException {
		return ((IntermediateComplexType) embeddedId.getStructuredType()).getEdmItem().getProperties();
	}
}
