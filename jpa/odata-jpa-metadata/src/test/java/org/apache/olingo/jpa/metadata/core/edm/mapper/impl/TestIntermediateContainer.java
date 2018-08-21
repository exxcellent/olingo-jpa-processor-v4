package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlFunctionImport;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.test.TestDataConstants;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestIntermediateContainer extends TestMappingRoot {
	private IntermediateServiceDocument serviceDocument;
	private AbstractJPASchema schema;

	@Before
	public void setup() throws ODataJPAModelException {
		IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
		serviceDocument = new IntermediateServiceDocument(PUNIT_NAME);
		schema = serviceDocument.createMetamodelSchema(PUNIT_NAME, emf.getMetamodel());
	}

	@Test
	public void checkContainerCanBeCreated() throws ODataJPAModelException {

		new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME), serviceDocument);
	}

	@Test
	public void checkGetName() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);
		assertEquals("OrgApacheOlingoJpaContainer", container.getExternalName());
	}

	@Test
	public void checkGetNoEntitySets() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);
		assertEquals(TestDataConstants.NO_ENTITY_TYPES, container.getEdmItem().getEntitySets().size());
	}

	@Test
	public void checkGetEntitySetName() throws ODataJPAModelException {

		final List<CsdlEntitySet> entitySets = serviceDocument.getEntityContainer().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				return;
			}
		}
		fail();
	}

	@Test
	public void checkGetEntitySetType() throws ODataJPAModelException {

		final List<CsdlEntitySet> entitySets = serviceDocument.getEntityContainer().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				assertEquals(new JPAEdmNameBuilder(PUNIT_NAME).buildFQN("BusinessPartner"), entitySet.getTypeFQN());
				return;
			}
		}
		fail();
	}

	@Ignore("Some attributes are currently commented out")
	@Test
	public void checkGetNoNavigationPropertyBindings() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				assertEquals(4, entitySet.getNavigationPropertyBindings().size());
				return;
			}
		}
		fail();
	}

	@Test
	public void checkGetNavigationPropertyBindingsPath() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				for (final CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
					if ("Roles".equals(binding.getPath())) {
						return;
					}
				}
			}
		}
		fail();
	}

	@Test
	public void checkGetNavigationPropertyBindingsTarget() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				for (final CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
					if ("Roles".equals(binding.getPath())) {
						assertEquals("BusinessPartnerRoles", binding.getTarget());
						return;
					}
				}
			}
		}
		fail();
	}

	@Ignore("Address/AdministrativeDivision is currently commented out")
	@Test
	public void checkGetNavigationPropertyBindingsPathComplexType() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				for (final CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
					if ("Address/AdministrativeDivision".equals(binding.getPath())) {
						return;
					}
				}
			}
		}
		fail();
	}

	@Ignore("Mapping has changed, so the requested navigaion binding may be invalid?!")
	@Test
	public void checkGetNavigationPropertyBindingsPathComplexTypeNested() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlEntitySet> entitySets = container.getEdmItem().getEntitySets();
		for (final CsdlEntitySet entitySet : entitySets) {
			if (entitySet.getName().equals("BusinessPartners")) {
				for (final CsdlNavigationPropertyBinding binding : entitySet.getNavigationPropertyBindings()) {
					if ("AdministrativeInformation/Created/User".equals(binding.getPath())) {
						return;
					}
				}
			}
		}
		fail();
	}

	@Test
	public void checkGetNoFunctionImportIfBound() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
		for (final CsdlFunctionImport funcImport : funcImports) {
			if (funcImport.getName().equals("CountRoles")) {
				fail("Bound function must not generate a function import");
			}
		}
	}

	@Test
	public void checkGetNoFunctionImportIfUnBoundHasImportFalse() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
		for (final CsdlFunctionImport funcImport : funcImports) {
			if (funcImport.getName().equals("max")) {
				fail("UnBound function must not generate a function import is not annotated");
			}
		}
	}

	@Test
	public void checkGetFunctionImportIfUnBoundHasImportTrue() throws ODataJPAModelException {

		final IntermediateEntityContainer container = new IntermediateEntityContainer(new JPAEdmNameBuilder(PUNIT_NAME),
				serviceDocument);

		final List<CsdlFunctionImport> funcImports = container.getEdmItem().getFunctionImports();
		for (final CsdlFunctionImport funcImport : funcImports) {
			if (funcImport.getName().equals("Olingo V4 ")) {
				fail("UnBound function must be generate a function import is annotated");
			}
		}
	}
}