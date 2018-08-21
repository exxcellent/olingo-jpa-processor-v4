package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Transient;

import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.jpa.cdi.Inject;
import org.apache.olingo.jpa.metadata.core.edm.annotation.EdmIgnore;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttributePath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASimpleAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * A DTO is mapped as OData entity!
 *
 * @author rzozmann
 *
 */
class IntermediateTypeDTO extends IntermediateModelElement implements JPAEntityType {
	private final Class<?> dtoType;
	private final IntermediateServiceDocument serviceDocument;
	private CsdlEntityType edmEntityType = null;
	private final Map<String, IntermediatePropertyDTOField> declaredPropertiesList;
	private final Map<String, JPAPathImpl> simpleAttributePathMap;
	private final Map<String, JPAPathImpl> complexAttributePathMap;

	public IntermediateTypeDTO(final JPAEdmNameBuilder nameBuilder, final Class<?> dtoType,
			final IntermediateServiceDocument serviceDocument) throws ODataJPAModelException {
		super(nameBuilder, dtoType.getName());

		// DTO must have marker annotation
		final ODataDTO annotation = dtoType.getAnnotation(ODataDTO.class);
		if (annotation == null) {
			throw new ODataJPAModelException(MessageKeys.TYPE_NOT_SUPPORTED, dtoType.getName(), null);
		}
		// super class not allowed for DTO
		if (dtoType.getSuperclass() != null && Object.class != dtoType.getSuperclass()) {
			throw new ODataJPAModelException(MessageKeys.TYPE_NOT_SUPPORTED, dtoType.getName(), null);
		}

		this.dtoType = dtoType;
		this.serviceDocument = serviceDocument;
		this.setExternalName(nameBuilder.buildDTOTypeName(dtoType));
		this.declaredPropertiesList = new HashMap<String, IntermediatePropertyDTOField>();
		this.simpleAttributePathMap = new HashMap<String, JPAPathImpl>();
		this.complexAttributePathMap = new HashMap<String, JPAPathImpl>();
	}

	boolean determineAbstract() {
		final int modifiers = dtoType.getModifiers();
		return Modifier.isAbstract(modifiers);
	}

	protected void buildPropertyList() throws ODataJPAModelException {

		IntermediatePropertyDTOField property;
		for (final Field field : dtoType.getDeclaredFields()) {
			if (field.isAnnotationPresent(EdmIgnore.class)) {
				continue;
			}
			if (field.isAnnotationPresent(Transient.class)) {
				continue;
			}
			if (field.isAnnotationPresent(Inject.class)) {
				continue;
			}
			// currently only primitive types are supported
			if (!JPATypeConvertor.isPrimitiveType(field)) {
				throw new ODataJPAModelException(ODataJPAModelException.MessageKeys.TYPE_NOT_SUPPORTED,
						field.getType().getName(), field.getName());
			}

			property = new IntermediatePropertyDTOField(nameBuilder, field, serviceDocument);
			declaredPropertiesList.put(property.internalName, property);
		}
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmEntityType != null) {
			return;
		}
		buildPropertyList();
		edmEntityType = new CsdlEntityType();
		edmEntityType.setName(getExternalName());
		edmEntityType.setProperties(extractEdmProperties(declaredPropertiesList));
		// edmEntityType.setNavigationProperties(
		// (List<CsdlNavigationProperty>)
		// extractEdmProperties(declaredNaviPropertiesList));
		edmEntityType.setKey(extractEdmKeyElements(declaredPropertiesList));
		edmEntityType.setAbstract(determineAbstract());
		// edmEntityType.setBaseType(determineBaseType());
		// edmEntityType.setHasStream(determineHasStream());
	}

	private List<CsdlPropertyRef> extractEdmKeyElements(final Map<String, IntermediatePropertyDTOField> propertyList)
			throws ODataJPAModelException {
		final List<CsdlPropertyRef> keyList = new ArrayList<CsdlPropertyRef>();
		for (final Entry<String, IntermediatePropertyDTOField> entry : propertyList.entrySet()) {
			if (entry.getValue().isKey()) {
				final CsdlPropertyRef key = new CsdlPropertyRef();
				key.setName(entry.getValue().getExternalName());
				keyList.add(key);
			}
		}
		return returnNullIfEmpty(keyList);
	}

	private List<CsdlProperty> extractEdmProperties(final Map<String, IntermediatePropertyDTOField> declaredPropertiesList)
			throws RuntimeException {
		return declaredPropertiesList.entrySet().stream().map(x -> {
			try {
				return x.getValue().getEdmItem();
			} catch (final ODataJPAModelException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}

	@Override
	public CsdlEntityType getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmEntityType;
	}

	@Override
	public boolean isAbstract() {
		final int modifiers = dtoType.getModifiers();
		return Modifier.isAbstract(modifiers);
	}

	@Override
	public List<JPAAssociationAttribute> getAssociations() throws ODataJPAModelException {
		return Collections.emptyList();
	}

	@Override
	public JPAAssociationAttribute getAssociation(final String internalName) throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPAAssociationPath getAssociationPath(final String externalName) throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPAAssociationPath> getAssociationPathList() throws ODataJPAModelException {
		return Collections.emptyList();
	}

	@Override
	public JPASimpleAttribute getAttribute(final String internalName) throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPASimpleAttribute> getAttributes() throws ODataJPAModelException {
		lazyBuildEdmItem();
		final List<JPASimpleAttribute> result = new ArrayList<JPASimpleAttribute>();
		for (final String propertyKey : declaredPropertiesList.keySet()) {
			final IntermediatePropertyDTOField attribute = declaredPropertiesList.get(propertyKey);
			if (!attribute.ignore()) {
				result.add(attribute);
			}
		}
		if (getBaseType() != null) {
			result.addAll(getBaseType().getAttributes());
		}
		return result;
	}

	protected JPAStructuredType getBaseType() throws ODataJPAModelException {
		final Class<?> baseType = dtoType.getSuperclass();
		if (baseType == null) {
			return null;
		}
		return serviceDocument.getEntityType(baseType);
	}

	@Override
	public String getContentType() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPASelector getContentTypeAttributePath() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPAAssociationPath getDeclaredAssociation(final JPAAssociationPath associationPath)
			throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPAAssociationPath getDeclaredAssociation(final String externalName) throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	private void lazyBuildCompletePathMap() throws ODataJPAModelException {
		ArrayList<JPAAttribute> pathList;

		lazyBuildEdmItem();
		if (simpleAttributePathMap.size() == 0) {
			String externalName;
			for (final IntermediatePropertyDTOField property : declaredPropertiesList.values()) {
				if (property.isComplex()) {
					complexAttributePathMap.put(property.getExternalName(),
							new JPAPathImpl(property.getExternalName(), null, property));
					final Map<String, JPAPathImpl> nestedComplexAttributePathMap = ((IntermediateStructuredType) property
							.getStructuredType()).getComplexAttributePathMap();
					for (final Entry<String, JPAPathImpl> entry : nestedComplexAttributePathMap.entrySet()) {
						externalName = entry.getKey();
						pathList = new ArrayList<JPAAttribute>(entry.getValue().getPathElements());
						pathList.add(0, property);
						complexAttributePathMap.put(nameBuilder.buildPath(property.getExternalName(), externalName),
								new JPAPathImpl(nameBuilder.buildPath(property.getExternalName(), externalName), null,
										pathList));
					}

					// add the (simple) properties of complex type as path to this type
					final Map<String, JPAPathImpl> nestedSimpleAttributePathMap = ((IntermediateStructuredType) property
							.getStructuredType()).getSimpleAttributePathMap();
					JPAPathImpl newPath;
					for (final Entry<String, JPAPathImpl> entry : nestedSimpleAttributePathMap.entrySet()) {
						externalName = entry.getKey();
						pathList = new ArrayList<JPAAttribute>(entry.getValue().getPathElements());
						pathList.add(0, property);
						if (property.isKey()) {
							newPath = new JPAPathImpl(externalName, determineDBFieldName(property, entry.getValue()),
									pathList);
						} else {
							newPath = new JPAPathImpl(nameBuilder.buildPath(property.getExternalName(), externalName),
									determineDBFieldName(property, entry.getValue()), pathList);
						}
						simpleAttributePathMap.put(newPath.getAlias(), newPath);
					}
				} else {
					simpleAttributePathMap.put(property.getExternalName(),
							new JPAPathImpl(property.getExternalName(), property.getDBFieldName(), property));
				}
			}
		}
		// TODO: base class must be a JPA type, so we can cast... but has a bad smell
		final IntermediateStructuredType baseType = (IntermediateStructuredType) getBaseType();
		if (baseType != null) {
			simpleAttributePathMap.putAll(baseType.getSimpleAttributePathMap());
			complexAttributePathMap.putAll(baseType.getComplexAttributePathMap());
		}

	}

	private String determineDBFieldName(final IntermediatePropertyDTOField property, final JPAAttributePath jpaPath) {
		final AttributeOverrides overwriteList = property.getAnnotation(AttributeOverrides.class);
		if (overwriteList != null) {
			for (final AttributeOverride overwrite : overwriteList.value()) {
				if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
					return overwrite.column().name();
				}
			}
		} else {
			final AttributeOverride overwrite = property.getAnnotation(AttributeOverride.class);
			if (overwrite != null) {
				if (overwrite.name().equals(jpaPath.getLeaf().getInternalName())) {
					return overwrite.column().name();
				}
			}
		}
		return jpaPath.getDBFieldName();
	}

	@Override
	public JPAAttributePath getPath(final String externalName) throws ODataJPAModelException {
		lazyBuildCompletePathMap();
		JPAAttributePath targetPath = simpleAttributePathMap.get(externalName);
		if (targetPath == null) {
			targetPath = complexAttributePathMap.get(externalName);
		}
		if (targetPath == null || targetPath.ignore()) {
			return null;
		}
		return targetPath;
	}

	@Override
	public List<JPASelector> getPathList() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getTypeClass() {
		return dtoType;
	}

	@Override
	public List<JPASimpleAttribute> getKeyAttributes() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPAAttributePath> getKeyPath() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getKeyType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPASelector> getSearchablePath() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public JPAAttributePath getStreamAttributePath() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTableName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasEtag() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasStream() throws ODataJPAModelException {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<JPAAttributePath> searchChildPath(final JPASelector selectItemPath) {
		throw new UnsupportedOperationException();
	}

}
