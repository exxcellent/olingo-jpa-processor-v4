package org.apache.olingo.jpa.test.criteriaapi;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescription;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartner;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRoleKey;
import org.apache.olingo.jpa.processor.core.testmodel.Country;
import org.apache.olingo.jpa.test.util.AbstractTest;
import org.apache.olingo.jpa.test.util.DataSourceHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestAssociations extends AbstractTest {
	private static EntityManagerFactory emf;
	private EntityManager em;
	private CriteriaBuilder cb;

	@BeforeClass
	public static void setupClass() {
		emf = createEntityManagerFactory(DataSourceHelper.DatabaseType.HSQLDB);
	}

	@Before
	public void setup() {
		em = emf.createEntityManager();
		cb = em.getCriteriaBuilder();
	}

	@Test
	public void testIdClassHandling() throws IOException, ODataException {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<?> root = cq.from(BusinessPartner.class);
		cq.multiselect(root.get("ID").alias("ID"), root.get("country"));

		final Subquery<?> subQuery = cq.subquery(BusinessPartnerRoleKey.class);
		final Root<BusinessPartnerRole> subQueryRoot = subQuery.from(BusinessPartnerRole.class);
		subQuery.select(subQueryRoot.get("businessPartnerID"));
		final Predicate equalIds = cb.equal(root.get("ID"), subQueryRoot.get("businessPartnerID"));
		final Predicate equalRole = cb.equal(subQueryRoot.get("roleCategory"), cb.literal("A"));
		final Predicate equalID = cb.equal(subQueryRoot.get("businessPartnerID"), cb.literal("2"));
		Predicate and = cb.and(equalIds, equalRole);
		and = cb.and(and, equalID);
		subQuery.where(and);
		cq.where(cb.exists(subQuery));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		assertTrue(result.size() == 1);
		assertTrue(result.get(0).get("ID").equals("2"));
	}

	@Test
	public void getAdministrativeDivisionDescriptions() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartner> root = cq.from(BusinessPartner.class);

		final ParameterExpression<String> p = cb.parameter(String.class);
		cq.multiselect(root.get("locations").alias("locations")).where(cb.equal(root.get("ID"), p));

		final TypedQuery<Tuple> tq = em.createQuery(cq);
		tq.setParameter(p, "3");// BusinessPartner with that Id
		final List<Tuple> result = tq.getResultList();
		// 'US-CA' must bring 2 results
		assertEquals(2, result.size());
		final AdministrativeDivisionDescription add = (AdministrativeDivisionDescription) result.get(1)
				.get("locations");
		assertNotNull(add);
		assertNotNull(add.getKey());
		assertEquals("US-CA", add.getKey().getDivisonCode());
	}

	@Test
	public void getBuPaRoles() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartner> root = cq.from(BusinessPartner.class);

		cq.multiselect(root.get("roles").alias("roles"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final BusinessPartnerRole role = (BusinessPartnerRole) result.get(0).get("roles");
		assertNotNull(role);
	}

	@Test
	public void getBuPaLocation() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartner> root = cq.from(BusinessPartner.class);

		cq.multiselect(root.get("locations").alias("L"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final AdministrativeDivisionDescription act = (AdministrativeDivisionDescription) result.get(0).get("L");
		assertNotNull(act);
	}

	@Ignore("This is simply a JPA provider test, but not an test for us... and it will not work in Hibernate")
	@Test
	public void getRoleBuPa() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartnerRole> root = cq.from(BusinessPartnerRole.class);

		cq.multiselect(root.get("businessPartner").alias("BuPa"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final BusinessPartner bp = (BusinessPartner) result.get(0).get("BuPa");
		assertNotNull(bp);
	}

	@Ignore("'countryName' is temporary removed from datamodel to fix the O/R mapping")
	@Test
	public void getBuPaCountryName() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartner> root = cq.from(BusinessPartner.class);

		cq.multiselect(root.get("address").get("countryName").alias("CN"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final Country region = (Country) result.get(0).get("CN");
		assertNotNull(region);
	}

	@Ignore("'regionName' is temporary removed from datamodel to fix the O/R mapping")
	@Test
	public void getBuPaRegionName() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<BusinessPartner> root = cq.from(BusinessPartner.class);

		cq.multiselect(root.get("address").get("regionName").alias("RN"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final AdministrativeDivisionDescription region = (AdministrativeDivisionDescription) result.get(0).get("RN");
		assertNotNull(region);
	}

	@Test
	public void getAdministrativeDivisionParent() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);

		cq.multiselect(root.get("parent").alias("P"));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("P");
		assertNotNull(act);
	}

	@Test
	public void getAdministrativeDivisionOneParent() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);
		root.alias("Source");
		cq.multiselect(root.get("parent").alias("P"));
		// cq.select((Selection<? extends Tuple>) root);
		cq.where(cb.and(
				cb.equal(root.get("codePublisher"), "Eurostat"),
				cb.and(
						cb.equal(root.get("codeID"), "NUTS3"),
						cb.equal(root.get("divisionCode"), "BE251"))));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("P");
		assertNotNull(act);
		assertEquals("NUTS2", act.getCodeID());
		assertEquals("BE25", act.getDivisionCode());
	}

	@Test
	public void getAdministrativeDivisionChildrenOfOneParent() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<AdministrativeDivision> root = cq.from(AdministrativeDivision.class);
		root.alias("Source");
		cq.multiselect(root.get("children").alias("C"));
		cq.where(cb.and(
				cb.equal(root.get("codePublisher"), "Eurostat"),
				cb.and(
						cb.equal(root.get("codeID"), "NUTS2"),
						cb.equal(root.get("divisionCode"), "BE25"))));
		cq.orderBy(cb.desc(root.get("divisionCode")));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> result = tq.getResultList();
		final AdministrativeDivision act = (AdministrativeDivision) result.get(0).get("C");
		assertNotNull(act);
		assertEquals(8, result.size());
		assertEquals("NUTS3", act.getCodeID());
		assertEquals("BE251", act.getDivisionCode());
	}

}
