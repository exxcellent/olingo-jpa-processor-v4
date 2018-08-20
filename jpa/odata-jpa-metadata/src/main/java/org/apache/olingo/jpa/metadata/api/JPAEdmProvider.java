package org.apache.olingo.jpa.metadata.api;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlAction;
import org.apache.olingo.commons.api.edm.provider.CsdlActionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlFunction;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.AbstractJPASchema;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.IntermediateServiceDocument;

public final class JPAEdmProvider extends CsdlAbstractEdmProvider {

	final private IntermediateServiceDocument serviceDocument;
	@SuppressWarnings("unused")
	final private AbstractJPASchema defaultSchema;

	// TODO edmx: Reference -> Support by Olingo?
	// http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406397930
	public JPAEdmProvider(final String namespace, final Metamodel jpaMetamodel) throws ODataException {
		super();
		serviceDocument = new IntermediateServiceDocument(namespace);
		// initial schema for persistence model of JPA
		defaultSchema = serviceDocument.createMetamodelSchema(namespace, jpaMetamodel);
	}

	@Override
	public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) throws ODataException {
		for (final CsdlSchema schema : serviceDocument.getEdmSchemas()) {
			if (schema.getNamespace().equals(complexTypeName.getNamespace())) {
				return schema.getComplexType(complexTypeName.getName());
			}
		}
		return null;
	}

	@Override
	public CsdlEnumType getEnumType(final FullQualifiedName enumTypeName) throws ODataException {
		for (final CsdlSchema schema : serviceDocument.getEdmSchemas()) {
			if (schema.getNamespace().equals(enumTypeName.getNamespace())) {
				return schema.getEnumType(enumTypeName.getName());
			}
		}
		return null;
	}

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		// we have to use the only/global entity container...derived from primary schema
		return serviceDocument.getEntityContainer();
		// return defaultSchema.getEdmItem().getEntityContainer();
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName)
			throws ODataException {
		// This method is invoked when displaying the Service Document at e.g.
		// .../DemoService.svc
		if(entityContainerName == null) {
			return serviceDocument.getEntityContainerInfo();
		}
		return null;
	}

	@Override
	public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainerFQN, final String entitySetName)
			throws ODataException {
		// the name space (of entity container) is ignored, because Odata v4 does only
		// support one entity container
		return serviceDocument.getEntityContainer().getEntitySet(entitySetName);
	}

	@Override
	public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {

		for (final CsdlSchema schema : serviceDocument.getEdmSchemas()) {
			if (schema.getNamespace().equals(entityTypeName.getNamespace())) {
				return schema.getEntityType(entityTypeName.getName());
			}
		}
		return null;
	}

	@Override
	public CsdlFunctionImport getFunctionImport(final FullQualifiedName entityContainerFQN,
			final String functionImportName)
					throws ODataException {
		// the name space (of entity container) is ignored, because Odata v4 does only
		// support one entity container
		return serviceDocument.getEntityContainer().getFunctionImport(functionImportName);
	}

	@Override
	public List<CsdlFunction> getFunctions(final FullQualifiedName functionName) throws ODataException {
		for (final CsdlSchema schema : serviceDocument.getEdmSchemas()) {
			if (schema.getNamespace().equals(functionName.getNamespace())) {
				return schema.getFunctions(functionName.getName());
			}
		}
		return null;
	}

	@Override
	public List<CsdlAction> getActions(final FullQualifiedName actionName) throws ODataException {
		for (final CsdlSchema schema : serviceDocument.getEdmSchemas()) {
			if (schema.getNamespace().equals(actionName.getNamespace())) {
				return schema.getActions(actionName.getName());
			}
		}
		return null;
	}

	@Override
	public CsdlActionImport getActionImport(final FullQualifiedName entityContainerFQN, final String actionImportName)
			throws ODataException {
		// the name space (of entity container) is ignored, because Odata v4 does only
		// support one entity container
		return serviceDocument.getEntityContainer().getActionImport(actionImportName);
	}

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		return serviceDocument.getEdmSchemas();
	}

	public final IntermediateServiceDocument getServiceDocument() {
		return serviceDocument;
	}

	public void setRequestLocales(final Enumeration<Locale> locales) {
		ODataJPAException.setLocales(locales);
	}
}
