package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlActionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAFunction;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;

/**
 * <a href=
 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398024"
 * >OData Version 4.0 Part 3 - 13 Entity Container</a>
 * @author Oliver Grande
 *
 */
class IntermediateEntityContainer extends IntermediateModelElement {
	private final IntermediateServiceDocument serviceDocument;
	final private Map<String, IntermediateEntitySet> entitySetListInternalKey;

	private CsdlEntityContainer edmContainer;

	IntermediateEntityContainer(final JPAEdmNameBuilder nameBuilder, final IntermediateServiceDocument serviceDocument)
			throws ODataJPAModelException {
		super(nameBuilder, nameBuilder.buildContainerName());
		this.serviceDocument = serviceDocument;
		this.setExternalName(nameBuilder.buildContainerName());
		this.entitySetListInternalKey = new HashMap<String, IntermediateEntitySet>();
	}

	@Override
	protected void lazyBuildEdmItem() throws ODataJPAModelException {
		if (edmContainer != null) {
			return;
		}
		edmContainer = new CsdlEntityContainer();
		edmContainer.setName(getExternalName());
		edmContainer.setEntitySets(buildEntitySets());
		edmContainer.setFunctionImports(buildFunctionImports());
		edmContainer.setActionImports(buildActionImports());

		// TODO Singleton
	}

	@Override
	CsdlEntityContainer getEdmItem() throws ODataJPAModelException {
		lazyBuildEdmItem();
		return edmContainer;
	}

	IntermediateEntitySet getEntitySet(final String edmEntitySetName) throws ODataJPAModelException {
		lazyBuildEdmItem();
		return (IntermediateEntitySet) findModelElementByEdmItem(edmEntitySetName,
				entitySetListInternalKey);
	}

	IntermediateEntitySet getEntitySet(final JPAEntityType entityType) throws ODataJPAModelException {
		lazyBuildEdmItem();
		for (final String internalName : entitySetListInternalKey.keySet()) {
			final IntermediateEntitySet modelElement = entitySetListInternalKey.get(internalName);
			if (modelElement.getEdmItem().getTypeFQN().equals(entityType.getExternalFQN())) {
				return modelElement;
			}
		}
		return null;
	}

	/**
	 * Entity Sets are described in <a href=
	 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398024"
	 * >OData Version 4.0 Part 3 - 13.2 Element edm:EntitySet</a>
	 * @param Entity Type
	 * @return Entity Set
	 */
	@SuppressWarnings("unchecked")
	private List<CsdlEntitySet> buildEntitySets() throws ODataJPAModelException {
		for (final AbstractJPASchema schema : serviceDocument.getJPASchemas()) {
			// Build Entity Sets
			for (final JPAEntityType et : schema.getEntityTypes()) {
				if (!et.ignore()) {
					final IntermediateEntitySet es = new IntermediateEntitySet(schema.getNameBuilder(), et);
					entitySetListInternalKey.put(es.internalName, es);
				}
			}
		}
		return (List<CsdlEntitySet>) extractEdmModelElements(entitySetListInternalKey);
	}

	/**
	 * Try to find the entity set matching the given name.
	 *
	 * @return The name of entity set or <code>null</code> if no one was found.
	 */
	private String findMatchingEntitySetName(final FullQualifiedName typeName) {
		for (final String internalName : entitySetListInternalKey.keySet()) {
			final IntermediateEntitySet entitySet = entitySetListInternalKey.get(internalName);
			if (entitySet.getEntityType().getExternalFQN().equals(typeName)) {
				return entitySet.getExternalName();
			}
		}
		return null;
	}

	/**
	 * Creates the FunctionImports. Function Imports have to be created for
	 * <i>unbound</i> functions. These are functions, which do not depend on an
	 * entity set. E.g. .../MyFunction().
	 * <p>
	 * Details are described in : <a href=
	 * "https://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398042"
	 * >OData Version 4.0 Part 3 - 13.6 Element edm:FunctionImport</a>
	 *
	 * @param CsdlFunction
	 *            edmFu
	 */
	private CsdlFunctionImport buildFunctionImport(final CsdlFunction edmFu) {
		final CsdlFunctionImport edmFuImport = new CsdlFunctionImport();
		edmFuImport.setName(edmFu.getName());
		edmFuImport.setFunction(nameBuilder.buildFQN(edmFu.getName()));
		edmFuImport.setIncludeInServiceDocument(true);
		if (edmFu.getReturnType() != null) {
			edmFuImport.setEntitySet(findMatchingEntitySetName(edmFu.getReturnType().getTypeFQN()));
		}
		return edmFuImport;
	}

	private List<CsdlFunctionImport> buildFunctionImports() throws ODataJPAModelException {
		final List<CsdlFunctionImport> edmFunctionImports = new LinkedList<CsdlFunctionImport>();
		for (final AbstractJPASchema schema : serviceDocument.getJPASchemas()) {
			// Build Entity Sets
			final List<JPAFunction> functions = schema.getFunctions();

			if (functions == null) {
				continue;
			}
			for (final JPAFunction jpaFu : functions) {
				if (!((IntermediateFunction) jpaFu).requiresFunctionImport()) {
					continue;
				}
				edmFunctionImports.add(buildFunctionImport(((IntermediateFunction) jpaFu).getEdmItem()));
			}
		}
		return edmFunctionImports;
	}

	private List<CsdlActionImport> buildActionImports() throws ODataJPAModelException {
		final List<CsdlActionImport> edmActionImports = new LinkedList<CsdlActionImport>();
		for (final AbstractJPASchema schema : serviceDocument.getJPASchemas()) {
			// Build Entity Sets
			final List<JPAAction> actions = schema.getActions();

			if (actions == null) {
				continue;
			}
			for (final JPAAction jpaAction : actions) {
				if (jpaAction.isBound()) {
					continue;
				}

				final IntermediateAction intermediateAction = (IntermediateAction) jpaAction;
				final CsdlAction emdAction = intermediateAction.getEdmItem();
				final CsdlActionImport edmActionImport = new CsdlActionImport();
				edmActionImport.setName(emdAction.getName());
				edmActionImport.setAction(intermediateAction.getExternalFQN());
				if (emdAction.getReturnType() != null) {
					edmActionImport.setEntitySet(findMatchingEntitySetName(emdAction.getReturnType().getTypeFQN()));
				}

				edmActionImports.add(edmActionImport);
			}
		}
		return edmActionImports;
	}

}
