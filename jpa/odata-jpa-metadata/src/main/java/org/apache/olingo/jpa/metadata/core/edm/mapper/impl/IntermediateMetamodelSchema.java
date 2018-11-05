package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.PluralAttribute;

import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException.MessageKeys;

/**
 * <p>For details about Schema metadata see:
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397946"
 * >OData Version 4.0 Part 3 - 5 Schema </a>
 * @author Oliver Grande
 *
 */
class IntermediateMetamodelSchema extends AbstractJPASchema {
	final private IntermediateServiceDocument serviceDocument;
	final private Metamodel jpaMetamodel;
	final private Map<String, IntermediateComplexType> complexTypeListInternalKey;
	final private Map<String, IntermediateEntityType> entityTypeListInternalKey;
	final private Map<String, IntermediateFunction> functionListInternalKey;
	final private Map<String, IntermediateAction> actionListInternalKey;
	private CsdlSchema edmSchema = null;

	IntermediateMetamodelSchema(final IntermediateServiceDocument serviceDocument, final String namespace,
			final Metamodel jpaMetamodel) throws ODataJPAModelException {
		super(namespace);
		this.serviceDocument = serviceDocument;
		this.jpaMetamodel = jpaMetamodel;
		this.complexTypeListInternalKey = buildComplexTypeList();
		this.entityTypeListInternalKey = buildEntityTypeList();
		this.functionListInternalKey = buildFunctionList();
		this.actionListInternalKey = buildActionList();
	}

	@SuppressWarnings("unchecked")
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmSchema != null) {
			return;
		}
		edmSchema = new CsdlSchema();
		edmSchema.setNamespace(getNameBuilder().buildNamespace());
		edmSchema.setComplexTypes(
				(List<CsdlComplexType>) IntermediateModelElement.extractEdmModelElements(complexTypeListInternalKey));
		edmSchema.setEntityTypes(
				(List<CsdlEntityType>) IntermediateModelElement.extractEdmModelElements(entityTypeListInternalKey));
		edmSchema.setFunctions(
				(List<CsdlFunction>) IntermediateModelElement.extractEdmModelElements(functionListInternalKey));
		edmSchema
		.setActions((List<CsdlAction>) IntermediateModelElement.extractEdmModelElements(actionListInternalKey));

		//  edm:Annotations
		//  edm:Annotation
		// edm:EnumType --> Annotation @Enummerated (see IntermediateCustomSchema)
		//  edm:Term
		//  edm:TypeDefinition
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
	IntermediateStructuredType getStructuredType(final Attribute<?, ?> jpaAttribute) {
		Class<?> targetClass = null;
		if (jpaAttribute.isCollection()) {
			targetClass = ((PluralAttribute<?, ?, ?>) jpaAttribute).getElementType().getJavaType();
		} else {
			targetClass = jpaAttribute.getJavaType();
		}
		IntermediateStructuredType type = complexTypeListInternalKey
				.get(JPANameBuilder.buildStructuredTypeName(targetClass));
		if (type == null) {
			type = entityTypeListInternalKey.get(JPANameBuilder.buildStructuredTypeName(targetClass));
		}
		return type;
	}

	@Override
	JPAEntityType getEntityType(final Class<?> targetClass) {
		return entityTypeListInternalKey.get(JPANameBuilder.buildStructuredTypeName(targetClass));
	}

	@Override
	IntermediateComplexType getComplexType(final Class<?> targetClass) {
		return complexTypeListInternalKey.get(JPANameBuilder.buildStructuredTypeName(targetClass));
	}

	@Override
	JPAEntityType getEntityType(final String externalName) {
		for (final String internalName : entityTypeListInternalKey.keySet()) {
			if (entityTypeListInternalKey.get(internalName).getExternalName().equals(externalName)) {
				return entityTypeListInternalKey.get(internalName);
			}
		}
		return null;
	}

	@Override
	List<JPAEntityType> getEntityTypes() {
		final List<JPAEntityType> entityTypes = new ArrayList<JPAEntityType>(
				entityTypeListInternalKey.size());
		entityTypes.addAll(entityTypeListInternalKey.values());
		return entityTypes;
	}

	@Override
	JPAFunction getFunction(final String externalName) {
		for (final String internalName : functionListInternalKey.keySet()) {
			if (functionListInternalKey.get(internalName).getExternalName().equals(externalName)) {
				if (!functionListInternalKey.get(internalName).ignore()) {
					return functionListInternalKey.get(internalName);
				}
			}
		}
		return null;
	}

	@Override
	List<JPAFunction> getFunctions() {
		final ArrayList<JPAFunction> functions = new ArrayList<JPAFunction>(functionListInternalKey.size());
		for (final String internalName : functionListInternalKey.keySet()) {
			functions.add(functionListInternalKey.get(internalName));
		}
		return functions;
	}

	@Override
	JPAAction getAction(final String externalName) {
		for (final String internalName : actionListInternalKey.keySet()) {
			if (actionListInternalKey.get(internalName).getExternalName().equals(externalName)) {
				if (!actionListInternalKey.get(internalName).ignore()) {
					return actionListInternalKey.get(internalName);
				}
			}
		}
		return null;
	}

	@Override
	List<JPAAction> getActions() {
		return new ArrayList<JPAAction>(actionListInternalKey.values());
	}

	private Map<String, IntermediateComplexType> buildComplexTypeList() throws ODataJPAModelException {
		final HashMap<String, IntermediateComplexType> ctList = new HashMap<String, IntermediateComplexType>();

		for (final EmbeddableType<?> embeddable : this.jpaMetamodel.getEmbeddables()) {
			final IntermediateComplexType ct = new IntermediateComplexType(getNameBuilder(), embeddable,
					serviceDocument);
			ctList.put(ct.internalName, ct);
		}
		return ctList;
	}

	private Map<String, IntermediateEntityType> buildEntityTypeList() throws ODataJPAModelException {
		final HashMap<String, IntermediateEntityType> etList = new HashMap<String, IntermediateEntityType>();

		for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {
			final IntermediateEntityType et = new IntermediateEntityType(getNameBuilder(), entity, serviceDocument);
			etList.put(et.internalName, et);
		}
		return etList;
	}

	private Map<String, IntermediateFunction> buildFunctionList() throws ODataJPAModelException {
		final HashMap<String, IntermediateFunction> funcList = new HashMap<String, IntermediateFunction>();
		// 1. Option: Create Function from Entity Annotations
		final IntermediateFunctionFactory factory = new IntermediateFunctionFactory();
		for (final EntityType<?> entity : this.jpaMetamodel.getEntities()) {

			funcList.putAll(factory.create(getNameBuilder(), entity, this));
		}
		return funcList;
	}

	private Map<String, IntermediateAction> buildActionList() throws ODataJPAModelException {
		final HashMap<String, IntermediateAction> actionList = new HashMap<String, IntermediateAction>();
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
	IntermediateEnumType createEnumType(final Class<? extends Enum<?>> clazz) throws ODataJPAModelException {
		// not supported in JPA models
		throw new ODataJPAModelException(MessageKeys.INVALID_ENTITY_TYPE);
	}
}
