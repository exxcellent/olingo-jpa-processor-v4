package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.test.TestDataConstants;
import org.apache.olingo.jpa.processor.core.util.EdmEntitySetDouble;
import org.apache.olingo.jpa.processor.core.util.EdmEntityTypeDouble;
import org.apache.olingo.jpa.processor.core.util.EdmPropertyDouble;
import org.apache.olingo.jpa.processor.core.util.ExpandItemDouble;
import org.apache.olingo.jpa.processor.core.util.ExpandOptionDouble;
import org.apache.olingo.jpa.processor.core.util.SelectOptionDouble;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestHelper;
import org.apache.olingo.jpa.processor.core.util.UriInfoDouble;
import org.apache.olingo.jpa.processor.core.util.UriResourceNavigationDouble;
import org.apache.olingo.jpa.processor.core.util.UriResourcePropertyDouble;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceValue;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TestJPAQuerySelectClause extends TestBase {

	private JPAAbstractEntityQuery cut;
	private JPAEntityType jpaEntityType;
	private HashMap<String, From<?, ?>> joinTables;
	private Root<?> root;
	private JPAODataSessionContextAccess context;

	@Before
	public void setup() throws ODataException {
		helper = new TestHelper(persistenceAdapter.getMetamodel(), PUNIT_NAME);
		jpaEntityType = helper.getJPAEntityType("BusinessPartners");
		createHeaders();
		context = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, persistenceAdapter.getMetamodel()),
				persistenceAdapter);
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "BusinessPartners"), context, null,
				persistenceAdapter.createEntityManager(), headers, null);
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables = new HashMap<String, From<?, ?>>();
		joinTables.put(jpaEntityType.getInternalName(), root);
	}

	@Test
	public void checkSelectAll() throws ODataApplicationException, ODataJPAModelException {
		fillJoinTable(root);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut
				.buildSelectionPathList(
						new UriInfoDouble(new SelectOptionDouble("*"))));
		assertEquals(jpaEntityType.getPathList().size(), selectClause.size());

	}

	@Test
	public void checkSelectAllWithSelectionNull() throws ODataApplicationException, ODataJPAModelException {
		fillJoinTable(root);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(new UriInfoDouble(
				null)));

		assertEquals(jpaEntityType.getPathList().size(), selectClause.size());
	}

	@Test
	public void checkSelectExpandViaIgnoredProperties() throws ODataApplicationException, ODataJPAModelException {
		// Organizations('3')/Address?$expand=AdministrativeDivision
		fillJoinTable(root);
		final List<ExpandItem> expItems = new ArrayList<ExpandItem>();
		final EdmEntityType startEntity = new EdmEntityTypeDouble(nameBuilder, "Organization");
		final EdmEntityType targetEntity = new EdmEntityTypeDouble(nameBuilder, "AdministrativeDivision");

		final ExpandOption expOps = new ExpandOptionDouble("AdministrativeDivision", expItems);
		expItems.add(new ExpandItemDouble(targetEntity));
		final List<UriResource> startResources = new ArrayList<UriResource>();
		final UriInfoDouble uriInfo = new UriInfoDouble(null);
		uriInfo.setExpandOpts(expOps);
		uriInfo.setUriResources(startResources);

		startResources.add(new UriResourceNavigationDouble(startEntity));
		startResources.add(new UriResourcePropertyDouble(new EdmPropertyDouble("Address")));

		final List<Selection<?>> selectClause = cut
				.createSelectClause(cut.buildSelectionPathList(uriInfo));

		assertContains(selectClause, "Address/RegionCodeID");
	}

	private void fillJoinTable(final Root<?> joinRoot) {
		Join<?, ?> join = joinRoot.join("locationName", JoinType.LEFT);
		joinTables.put("locationName", join);
		join = joinRoot.join("address", JoinType.LEFT);
		join = join.join("countryName", JoinType.LEFT);
		joinTables.put("countryName", join);
		join = joinRoot.join("address", JoinType.LEFT);
		join = join.join("regionName", JoinType.LEFT);
		joinTables.put("regionName", join);
	}

	@Test
	public void checkSelectOnePropertyCreatedAt() throws ODataApplicationException, ODataJPAModelException {
		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("CreationDateTime"))));
		assertEquals(2, selectClause.size());
		assertContains(selectClause, "CreationDateTime");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectOnePropertyID() throws ODataApplicationException, ODataJPAModelException {
		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("ID"))));
		assertEquals(1, selectClause.size());
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectOnePropertyPartKey() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("AdministrativeDivisionDescriptions");
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "AdministrativeDivisionDescriptions"), context, null,
				persistenceAdapter.createEntityManager(), headers, null);

		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble((new SelectOptionDouble("CodePublisher")))));
		assertEquals(4, selectClause.size());
		assertContains(selectClause, "CodePublisher");
		assertContains(selectClause, "CodeID");
		assertContains(selectClause, "DivisionCode");
		assertContains(selectClause, "Language");
	}

	@Test
	public void checkSelectPropertyTypeCreatedAt() throws ODataApplicationException, ODataJPAModelException {
		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Type,CreationDateTime"))));

		assertEquals(3, selectClause.size());
		assertContains(selectClause, "CreationDateTime");
		assertContains(selectClause, "Type");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectSupertypePropertyTypeName2() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);

		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Type,Name2"))));
		assertContains(selectClause, "Name2");
		assertContains(selectClause, "Type");
		assertContains(selectClause, "ID");
		assertEquals(3, selectClause.size());
	}

	@Test
	public void checkSelectCompleteComplexType() throws ODataApplicationException, ODataJPAModelException {
		// Organizations$select=Address
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);
		fillJoinTable(root);

		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Address"))));
		//    assertContains(selectClause, "Address");
		//    assertContains(selectClause, "ID");
		assertEquals(TestDataConstants.NO_ATTRIBUTES_POSTAL_ADDRESS + 1, selectClause.size());
	}

	@Test
	public void checkSelectCompleteNestedComplexTypeLowLevel() throws ODataApplicationException, ODataJPAModelException {
		// Organizations$select=Address
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);

		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("AdministrativeInformation/Created"))));
		assertEquals(3, selectClause.size());
		assertContains(selectClause, "AdministrativeInformation/Created/By");
		assertContains(selectClause, "AdministrativeInformation/Created/At");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectCompleteNestedComplexTypeHighLevel() throws ODataApplicationException, ODataJPAModelException {
		// Organizations$select=Address
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);

		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("AdministrativeInformation"))));
		assertEquals(5, selectClause.size());
		assertContains(selectClause, "AdministrativeInformation/Created/By");
		assertContains(selectClause, "AdministrativeInformation/Created/At");
		assertContains(selectClause, "AdministrativeInformation/Updated/By");
		assertContains(selectClause, "AdministrativeInformation/Updated/At");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectElementOfComplexType() throws ODataApplicationException, ODataJPAModelException {
		// Organizations$select=Address/Country
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		joinTables.put(jpaEntityType.getInternalName(), root);

		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		// SELECT c.address.geocode FROM Company c WHERE c.name = 'Random House'
		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Address/Country"))));
		assertContains(selectClause, "Address/Country");
		assertContains(selectClause, "ID");
		assertEquals(2, selectClause.size());
	}

	@Test
	public void checkSelectTextJoinSingleAttribute() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);
		joinTables.put(jpaEntityType.getInternalName(), root);
		fillJoinTable(root);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Address/CountryName"))));
		assertContains(selectClause, "Address/CountryName");
		assertContains(selectClause, "ID");
		assertEquals(2, selectClause.size());
	}

	@Test
	public void checkSelectTextJoinCompextType() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);
		joinTables.put(jpaEntityType.getInternalName(), root);
		fillJoinTable(root);

		final List<Selection<?>> selectClause = cut.createSelectClause(cut.buildSelectionPathList(
				new UriInfoDouble(new SelectOptionDouble("Address"))));
		assertEquals(TestDataConstants.NO_ATTRIBUTES_POSTAL_ADDRESS + 1, selectClause.size());
		assertContains(selectClause, "Address/CountryName");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectStreamValueStatic() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("PersonImages");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "PersonImages"), context, null, emf
				.createEntityManager(), headers, null);

		final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));
		final List<UriResource> uriResources = new ArrayList<UriResource>();
		uriInfo.setUriResources(uriResources);
		uriResources.add(new UriResourceEntitySetDouble());
		uriResources.add(new UriResourceValueDouble());

		final List<Selection<?>> selectClause = cut
				.createSelectClause(cut.buildSelectionPathList(uriInfo));
		assertNotNull(selectClause);
		assertContains(selectClause, "Image");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectStreamValueDynamic() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("OrganizationImages");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "OrganizationImages"), context, null, emf
				.createEntityManager(), headers, null);

		final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));
		final List<UriResource> uriResources = new ArrayList<UriResource>();
		uriInfo.setUriResources(uriResources);
		uriResources.add(new UriResourceEntitySetDouble());
		uriResources.add(new UriResourceValueDouble());

		final List<Selection<?>> selectClause = cut
				.createSelectClause(cut.buildSelectionPathList(uriInfo));
		assertNotNull(selectClause);
		assertContains(selectClause, "Image");
		assertContains(selectClause, "MimeType");
		assertContains(selectClause, "ID");
	}

	@Test
	public void checkSelectPropertyValue() throws ODataApplicationException, ODataJPAModelException {
		jpaEntityType = helper.getJPAEntityType("Organizations");
		root = persistenceAdapter.getEMF().getCriteriaBuilder().createTupleQuery().from(jpaEntityType.getTypeClass());
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);

		final UriInfoDouble uriInfo = new UriInfoDouble(new SelectOptionDouble("Address"));
		final List<UriResource> uriResources = new ArrayList<UriResource>();
		uriInfo.setUriResources(uriResources);
		// BusinessPartnerImages('99')/AdministrativeInformation/Created/By/$value
		uriResources.add(new UriResourceEntitySetDouble());
		uriResources.add(new UriResourceComplexPropertyDouble(new EdmPropertyDouble("AdministrativeInformation")));
		uriResources.add(new UriResourceComplexPropertyDouble(new EdmPropertyDouble("Created")));
		uriResources.add(new UriResourcePropertyDouble(new EdmPropertyDouble("By")));
		uriResources.add(new UriResourceValueDouble());

		final List<Selection<?>> selectClause = cut
				.createSelectClause(cut.buildSelectionPathList(uriInfo));
		assertNotNull(selectClause);
		assertContains(selectClause, "AdministrativeInformation/Created/By");
		assertContains(selectClause, "ID");
	}

	private void assertContains(final List<Selection<?>> selectClause, final String alias) {
		for (final Selection<?> selection : selectClause) {
			if (selection.getAlias().equals(alias)) {
				return;
			}
		}
		fail(alias + " not found");
	}

	private class UriResourceValueDouble implements UriResourceValue {

		@Override
		public UriResourceKind getKind() {
			return UriResourceKind.value;
		}

		@Override
		public String getSegmentValue() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private class UriResourceComplexPropertyDouble implements UriResourceComplexProperty {
		private final EdmProperty property;

		public UriResourceComplexPropertyDouble(final EdmProperty property) {
			super();
			this.property = property;
		}

		@Override
		public EdmProperty getProperty() {
			return property;
		}

		@Override
		public EdmType getType() {
			fail();
			return null;
		}

		@Override
		public boolean isCollection() {
			fail();
			return false;
		}

		@Override
		public String getSegmentValue(final boolean includeFilters) {
			fail();
			return null;
		}

		@Override
		public String toString(final boolean includeFilters) {
			fail();
			return null;
		}

		@Override
		public UriResourceKind getKind() {
			fail();
			return null;
		}

		@Override
		public String getSegmentValue() {
			fail();
			return null;
		}

		@Override
		public EdmComplexType getComplexType() {
			fail();
			return null;
		}

		@Override
		public EdmComplexType getComplexTypeFilter() {
			fail();
			return null;
		}

	}

	private class UriResourceEntitySetDouble implements UriResourceEntitySet {

		@Override
		public EdmType getType() {
			fail();
			return null;
		}

		@Override
		public boolean isCollection() {
			fail();
			return false;
		}

		@Override
		public String getSegmentValue(final boolean includeFilters) {
			fail();
			return null;
		}

		@Override
		public String toString(final boolean includeFilters) {
			fail();
			return null;
		}

		@Override
		public UriResourceKind getKind() {
			fail();
			return null;
		}

		@Override
		public String getSegmentValue() {
			fail();
			return null;
		}

		@Override
		public EdmEntitySet getEntitySet() {
			fail();
			return null;
		}

		@Override
		public EdmEntityType getEntityType() {
			fail();
			return null;
		}

		@Override
		public List<UriParameter> getKeyPredicates() {
			fail();
			return null;
		}

		@Override
		public EdmType getTypeFilterOnCollection() {
			fail();
			return null;
		}

		@Override
		public EdmType getTypeFilterOnEntry() {
			fail();
			return null;
		}

	}
}
