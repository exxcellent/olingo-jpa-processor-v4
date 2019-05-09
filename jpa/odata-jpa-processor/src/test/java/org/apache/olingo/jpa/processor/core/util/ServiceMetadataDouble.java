package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmAction;
import org.apache.olingo.commons.api.edm.EdmAnnotations;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntityContainer;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmFunction;
import org.apache.olingo.commons.api.edm.EdmSchema;
import org.apache.olingo.commons.api.edm.EdmTerm;
import org.apache.olingo.commons.api.edm.EdmTypeDefinition;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.jpa.metadata.core.edm.mapper.impl.JPAEdmNameBuilder;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.etag.ServiceMetadataETagSupport;

public class ServiceMetadataDouble implements ServiceMetadata {

	private final Edm edm;

	public ServiceMetadataDouble() {
		super();
		edm = new EdmDouble();
	}

	public ServiceMetadataDouble(final JPAEdmNameBuilder nameBuilder, final String typeName) {
		super();
		this.nameBuilder = nameBuilder;
		this.edm = new EdmDouble(typeName);
	}

	private JPAEdmNameBuilder nameBuilder;

	@Override
	public Edm getEdm() {
		return edm;
	}

	@Override
	public ODataServiceVersion getDataServiceVersion() {
		fail();
		return null;
	}

	@Override
	public List<EdmxReference> getReferences() {
		fail();
		return null;
	}

	@Override
	public ServiceMetadataETagSupport getServiceMetadataETagSupport() {
		fail();
		return null;
	}

	class EdmDouble implements Edm {

		private final Map<FullQualifiedName, EdmEntityType> typeMap;

		public EdmDouble() {
			super();
			typeMap = new HashMap<FullQualifiedName, EdmEntityType>();
		}

		public EdmDouble(final String name) {
			super();
			typeMap = new HashMap<FullQualifiedName, EdmEntityType>();
			final EdmEntityType edmType = new EdmEntityTypeDouble(nameBuilder, name);
			typeMap.put(edmType.getFullQualifiedName(), edmType);
		}

		@Override
		public List<EdmSchema> getSchemas() {
			fail();
			return null;
		}

		@Override
		public EdmSchema getSchema(final String namespace) {
			fail();
			return null;
		}

		@Override
		public EdmEntityContainer getEntityContainer() {
			fail();
			return null;
		}

		@Override
		public EdmEntityContainer getEntityContainer(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmEnumType getEnumType(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmEntityType getEntityTypeWithAnnotations(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmTypeDefinition getTypeDefinition(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmEntityType getEntityType(final FullQualifiedName name) {
			return typeMap.get(name);
		}

		@Override
		public EdmComplexType getComplexType(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmComplexType getComplexTypeWithAnnotations(final FullQualifiedName name) {
			fail();
			return null;
		}

		@Override
		public EdmAction getUnboundAction(final FullQualifiedName actionName) {
			fail();
			return null;
		}

		@Override
		public EdmAction getBoundAction(final FullQualifiedName actionName, final FullQualifiedName bindingParameterTypeName,
		        final Boolean isBindingParameterCollection) {
			fail();
			return null;
		}

		@Override
		public List<EdmFunction> getUnboundFunctions(final FullQualifiedName functionName) {
			fail();
			return null;
		}

		@Override
		public EdmFunction getUnboundFunction(final FullQualifiedName functionName, final List<String> parameterNames) {
			fail();
			return null;
		}

		@Override
		public EdmFunction getBoundFunction(final FullQualifiedName functionName, final FullQualifiedName bindingParameterTypeName,
		        final Boolean isBindingParameterCollection, final List<String> parameterNames) {
			fail();
			return null;
		}

		@Override
		public EdmTerm getTerm(final FullQualifiedName termName) {
			fail();
			return null;
		}

		@Override
		public EdmAnnotations getAnnotationGroup(final FullQualifiedName targetName, final String qualifier) {
			fail();
			return null;
		}

		@Override
		public EdmAction getBoundActionWithBindingType(final FullQualifiedName bindingParameterTypeName,
		        final Boolean isBindingParameterCollection) {
			return null;
		}

		@Override
		public List<EdmFunction> getBoundFunctionsWithBindingType(final FullQualifiedName bindingParameterTypeName,
		        final Boolean isBindingParameterCollection) {
			// TODO Check what this is used for
			return null;
		}

	}

}
