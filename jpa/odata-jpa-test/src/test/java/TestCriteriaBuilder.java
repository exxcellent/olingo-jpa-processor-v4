import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivision;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescription;
import org.apache.olingo.jpa.processor.core.testmodel.AdministrativeDivisionDescriptionKey;
import org.apache.olingo.jpa.processor.core.testmodel.BusinessPartnerRole;
import org.apache.olingo.jpa.processor.core.testmodel.DataSourceHelper;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TestCriteriaBuilder {
	private static final String ENTITY_MANAGER_DATA_SOURCE = "javax.persistence.nonJtaDataSource";
	private static EntityManagerFactory emf;
	private EntityManager em;
	private CriteriaBuilder cb;

	@BeforeClass
	public static void setupClass() {
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(ENTITY_MANAGER_DATA_SOURCE, DataSourceHelper.createDataSource(
				// doesn't work with HDBSQL
				DataSourceHelper.DB_DERBY));
		emf = Persistence.createEntityManagerFactory(org.apache.olingo.jpa.processor.core.test.Constant.PUNIT_NAME, properties);
	}

	@Before
	public void setup() {
		em = emf.createEntityManager();
		cb = em.getCriteriaBuilder();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSubstringWithExperession() {
		final CriteriaQuery<Tuple> adminQ = cb.createTupleQuery();
		final Root<AdministrativeDivisionDescription> adminRoot1 = adminQ.from(AdministrativeDivisionDescription.class);
		//    (Expression<T>) cb.sum(jpaOperator.getLeft(), jpaOperator.getRightAsNumber());
		//    cb.substring((Expression<String>) (jpaFunction.getParameter(0).get()), start, length);
		final Path<?> p = adminRoot1.get("name");

		final Expression<Integer> sum = cb.sum(cb.literal(1), cb.literal(4));

		adminQ.where(cb.equal(cb.substring((Expression<String>) (p), cb.literal(1), sum), "North"));
		adminQ.multiselect(adminRoot1.get("name"));
		final TypedQuery<Tuple> tq = em.createQuery(adminQ);
		tq.getResultList();
	}

	@Test
	public void testSubSelect() {
		// https://stackoverflow.com/questions/29719321/combining-conditional-expressions-with-and-and-or-predicates-using-the-jpa-c
		final CriteriaQuery<Tuple> adminQ1 = cb.createTupleQuery();
		final Subquery<Long> adminQ2 = adminQ1.subquery(Long.class);
		final Subquery<Long> adminQ3 = adminQ2.subquery(Long.class);
		final Subquery<Long> org = adminQ3.subquery(Long.class);

		final Root<AdministrativeDivision> adminRoot1 = adminQ1.from(AdministrativeDivision.class);
		final Root<AdministrativeDivision> adminRoot2 = adminQ2.from(AdministrativeDivision.class);
		final Root<AdministrativeDivision> adminRoot3 = adminQ3.from(AdministrativeDivision.class);
		final Root<Organization> org1 = org.from(Organization.class);

		org.where(cb.and(cb.equal(org1.get("ID"), "3")), createParentOrg(org1, adminRoot3));
		org.select(cb.literal(1L));

		adminQ3.where(cb.and(createParentAdmin(adminRoot3, adminRoot2), cb.exists(org)));
		adminQ3.select(cb.literal(1L));

		adminQ2.where(cb.and(createParentAdmin(adminRoot2, adminRoot1), cb.exists(adminQ3)));
		adminQ2.select(cb.literal(1L));

		adminQ1.where(cb.exists(adminQ2));
		adminQ1.multiselect(adminRoot1.get("divisionCode"));

		final TypedQuery<Tuple> tq = em.createQuery(adminQ1);
		tq.getResultList();
	}

	@Test
	public void TestExpandCount() {
		final CriteriaQuery<Tuple> count = cb.createTupleQuery();
		final Root<?> roles = count.from(BusinessPartnerRole.class);

		count.multiselect(roles.get("businessPartnerID"), cb.count(roles).alias("$count"));
		count.groupBy(roles.get("businessPartnerID"));
		count.orderBy(cb.desc(cb.count(roles)));
		final TypedQuery<Tuple> tq = em.createQuery(count);
		final List<Tuple> act = tq.getResultList();
		tq.getFirstResult();
	}

	@Test
	public void TestAnd() {
		final CriteriaQuery<Tuple> count = cb.createTupleQuery();
		final Root<?> adminDiv = count.from(AdministrativeDivision.class);

		count.multiselect(adminDiv);
		final Predicate[] restrictions = new Predicate[3];
		restrictions[0] = cb.equal(adminDiv.get("codeID"), "NUTS2");
		restrictions[1] = cb.equal(adminDiv.get("divisionCode"), "BE34");
		restrictions[2] = cb.equal(adminDiv.get("codePublisher"), "Eurostat");
		count.where(cb.and(restrictions));
		final TypedQuery<Tuple> tq = em.createQuery(count);
		final List<Tuple> act = tq.getResultList();
		tq.getFirstResult();
	}

	@Ignore
	@Test
	public void TestSearchEmbeddedId() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<AdministrativeDivisionDescription> adminDiv = cq.from(AdministrativeDivisionDescription.class);
		cq.multiselect(adminDiv);

		final Subquery<AdministrativeDivisionDescriptionKey> sq = cq.subquery(AdministrativeDivisionDescriptionKey.class);
		final Root<AdministrativeDivisionDescription> text = sq.from(AdministrativeDivisionDescription.class);
		sq.where(cb.function("CONTAINS", Boolean.class, text.get("name"), cb.literal("luettich")));
		final Expression<AdministrativeDivisionDescriptionKey> exp = text.get("key");
		sq.select(exp);

		//    cq.where(cb.and(cb.equal(adminDiv.get("key").get("codeID"), "NUTS2"),
		//        cb.in(adminDiv).value(sq)));
		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> act = tq.getResultList();
		System.out.println(act.size());
	}

	@Ignore
	@Test
	public void TestSearchNoSubquery() {
		final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		final Root<?> adminDiv = cq.from(AdministrativeDivisionDescription.class);
		cq.multiselect(adminDiv);

		// Predicate[] restrictions = new Predicate[2];
		cq.where(
				cb.and(cb.equal(cb.conjunction(),
						cb.function("CONTAINS", Boolean.class, adminDiv.get("name"), cb.literal("luettich"))),
						cb.equal(adminDiv.get("key").get("codeID"), "NUTS2")));

		final TypedQuery<Tuple> tq = em.createQuery(cq);
		final List<Tuple> act = tq.getResultList();
		System.out.println(act.size());
	}

	private Expression<Boolean> createParentAdmin(final Root<AdministrativeDivision> subQuery,
			final Root<AdministrativeDivision> query) {
		return cb.and(cb.equal(query.get("codePublisher"), subQuery.get("codePublisher")),
				cb.and(cb.equal(query.get("codeID"), subQuery.get("parentCodeID")),
						cb.equal(query.get("divisionCode"), subQuery.get("parentDivisionCode"))));
	}

	private Predicate createParentOrg(final Root<Organization> org1, final Root<AdministrativeDivision> adminRoot3) {
		return cb.and(cb.equal(adminRoot3.get("codePublisher"), org1.get("address").get("regionCodePublisher")),
				cb.and(cb.equal(adminRoot3.get("codeID"), org1.get("address").get("regionCodeID")),
						cb.equal(adminRoot3.get("divisionCode"), org1.get("address").get("region"))));
	}
}