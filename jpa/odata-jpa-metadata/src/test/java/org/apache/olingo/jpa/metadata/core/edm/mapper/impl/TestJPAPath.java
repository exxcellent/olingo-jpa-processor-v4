package org.apache.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;

import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPASelector;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.junit.Before;
import org.junit.Test;

public class TestJPAPath extends TestMappingRoot {
	private JPAEntityType organization;
	// private JPAStructuredType postalAddress;
	private TestHelper helper;

	@Before
	public void setup() throws ODataJPAModelException {
		helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
		organization = new IntermediateEntityType(new JPAEdmNameBuilder(PUNIT_NAME), helper.getEntityType(
				"Organization"), helper.serviceDocument);
	}

	@Test
	public void checkOnePathElementAlias() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Name1");
		assertEquals("Name1", cut.getAlias());
	}

	@Test
	public void checkOnePathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Name1");
		assertEquals(1, cut.getPathElements().size());
	}

	@Test
	public void checkOnePathElementElement() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Name1");
		assertEquals("name1", cut.getPathElements().get(0).getInternalName());
	}

	@Test
	public void checkOnePathElementFromSuperTypeAlias() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Type");
		assertEquals("Type", cut.getAlias());
	}

	@Test
	public void checkTwoPathElementAlias() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Address/Country");
		assertEquals("Address/Country", cut.getAlias());
	}

	@Test
	public void checkTwoPathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Address/Country");
		assertEquals(2, cut.getPathElements().size());
	}

	@Test
	public void checkTwoPathElementPathElements() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("Address/Country");
		assertEquals("address", cut.getPathElements().get(0).getInternalName());
		assertEquals("country", cut.getPathElements().get(1).getInternalName());
	}

	@Test
	public void checkThreePathElementAlias() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("AdministrativeInformation/Created/By");
		assertEquals("AdministrativeInformation/Created/By", cut.getAlias());
	}

	@Test
	public void checkThreePathElementPathSize() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("AdministrativeInformation/Created/By");
		assertEquals(3, cut.getPathElements().size());
	}

	@Test
	public void checkThreePathElementPathElements() throws ODataApplicationException, ODataJPAModelException {
		final JPASelector cut = organization.getPath("AdministrativeInformation/Created/By");
		assertEquals("administrativeInformation", cut.getPathElements().get(0).getInternalName());
		assertEquals("created", cut.getPathElements().get(1).getInternalName());
		assertEquals("by", cut.getPathElements().get(2).getInternalName());
	}

}
