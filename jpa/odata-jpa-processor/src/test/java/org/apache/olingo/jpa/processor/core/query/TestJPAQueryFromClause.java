package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAssociationAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.api.JPAODataContextAccessDouble;
import org.apache.olingo.jpa.processor.core.api.JPAODataSessionContextAccess;
import org.apache.olingo.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.jpa.processor.core.util.EdmEntitySetDouble;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.apache.olingo.jpa.processor.core.util.TestHelper;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Before;
import org.junit.Test;

public class TestJPAQueryFromClause extends TestBase {
	private JPAAbstractEntityQuery cut;
	private JPAEntityType jpaEntityType;

	@Before
	public void setup() throws ODataException {
		helper = new TestHelper(emf, PUNIT_NAME);
		jpaEntityType = helper.getJPAEntityType("Organizations");
		final JPAODataSessionContextAccess context = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, emf.getMetamodel()), persistenceAdapter);
		createHeaders();
		cut = new JPAQuery(null, new EdmEntitySetDouble(nameBuilder, "Organizations"), context, null, emf
				.createEntityManager(), headers, null);
	}

	@Test
	public void checkFromListContainsRoot() throws ODataApplicationException {
		final Map<String, From<?, ?>> act = cut.createFromClause(new ArrayList<JPAAssociationAttribute>());
		assertNotNull(act.get(jpaEntityType.getInternalName()));
	}

	@Test
	public void checkFromListOrderByContainsOne() throws ODataJPAModelException, ODataApplicationException {
		final List<JPAAssociationAttribute> orderBy = new ArrayList<JPAAssociationAttribute>();
		final JPAAttribute exp = helper.getJPAAssociation("Organizations", "roles");
		orderBy.add((JPAAssociationAttribute) exp);

		final Map<String, From<?, ?>> act = cut.createFromClause(orderBy);
		assertNotNull(act.get(exp.getInternalName()));
	}

	@Test
	public void checkFromListOrderByOuterJoinOne() throws ODataJPAModelException, ODataApplicationException {
		final List<JPAAssociationAttribute> orderBy = new ArrayList<JPAAssociationAttribute>();
		final JPAAttribute exp = helper.getJPAAssociation("Organizations", "roles");
		orderBy.add((JPAAssociationAttribute) exp);

		final Map<String, From<?, ?>> act = cut.createFromClause(orderBy);

		@SuppressWarnings("unchecked")
		final
		Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getInternalName());
		final Set<Join<Organization, ?>> joins = root.getJoins();
		assertEquals(1, joins.size());

		for (final Join<Organization, ?> join : joins) {
			assertEquals(JoinType.LEFT, join.getJoinType());
		}
	}

	@Test
	public void checkFromListOrderByOuterJoinOnConditionOne() throws ODataJPAModelException, ODataApplicationException {
		final List<JPAAssociationAttribute> orderBy = new ArrayList<JPAAssociationAttribute>();
		final JPAAttribute exp = helper.getJPAAssociation("Organizations", "roles");
		orderBy.add((JPAAssociationAttribute) exp);

		final Map<String, From<?, ?>> act = cut.createFromClause(orderBy);

		@SuppressWarnings("unchecked")
		final
		Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getInternalName());
		final Set<Join<Organization, ?>> joins = root.getJoins();
		assertEquals(1, joins.size());

		for (final Join<Organization, ?> join : joins) {
			assertNull(join.getOn());
		}
	}

}
