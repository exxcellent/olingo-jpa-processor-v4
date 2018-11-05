package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.metamodel.Attribute;

import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
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
	final private IntermediateServiceDocument serviceDocument;

	IntermediateCustomSchema(final IntermediateServiceDocument serviceDocument, final String namespace)
			throws ODataJPAModelException {
		super(namespace);
		this.serviceDocument = serviceDocument;
	}

	@Override
	IntermediateStructuredType getStructuredType(final Attribute<?, ?> jpaAttribute) {
		// currently not supported
		return null;
	}

	@Override
	JPAEntityType getEntityType(final Class<?> targetClass) {
		return dtoTypes.get(JPANameBuilder.buildStructuredTypeName(targetClass));
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

	IntermediateTypeDTO getDTOType(final Class<?> targetClass) {
		return dtoTypes.get(targetClass.getSimpleName());
	}

	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmSchema != null) {
			return;
		}
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

	private List<CsdlAction> buildActionList() throws ODataJPAModelException {
		return actions.entrySet().stream().map(x -> {
			try {
				return x.getValue().getEdmItem();
			} catch (final ODataJPAModelException e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
	}

	@Override
	IntermediateEnumType createEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException {
		final String namespace = clazz.getPackage().getName();
		if (!namespace.equalsIgnoreCase(getInternalName())) {
			throw new ODataJPAModelException(MessageKeys.GENERAL);
		}
		IntermediateEnumType enumType = getEnumType(clazz);
		if (enumType == null) {
			enumType = new IntermediateEnumType(getNameBuilder(), clazz, serviceDocument);
			enumTypes.put(clazz.getSimpleName(), enumType);
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
			dtoTypes.put(clazz.getSimpleName(), dtoType);
			// build actions for DTO
			final IntermediateActionFactory factory = new IntermediateActionFactory();
			actions.putAll(factory.create(getNameBuilder(), dtoType.getTypeClass(), serviceDocument));
		}
		return dtoType;

	}

	@Override
	public CsdlSchema getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmSchema;
	}

	@Override
	JPAAction getAction(final String externalName) {
		for (final Entry<String, IntermediateAction> entry : actions.entrySet()) {
			if (entry.getValue().getExternalName().equals(externalName)) {
				if (!entry.getValue().ignore()) {
					return entry.getValue();
				}
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
		// currently not supported
		return null;
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
		final List<JPAEntityType> entityTypes = new ArrayList<JPAEntityType>(dtoTypes.size());
		entityTypes.addAll(dtoTypes.values());
		return entityTypes;
	}
}
