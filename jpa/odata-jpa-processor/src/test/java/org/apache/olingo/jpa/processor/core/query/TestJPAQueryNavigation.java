package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPAQueryNavigation extends TestBase {

	@Test
	public void testNavigationOneHop() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Organizations('3')/Roles");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
	}

	@Test
	public void testNavigationOneHopWithoutReferencedColumn() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')/Image2");
		helper.assertStatus(200);

		final ObjectNode img = helper.getValue();
		assertEquals(99, img.get("PID").asLong());
	}

	@Test
	public void testNavigationOneHopWithoutJoinColumnButMappedBy() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('98')/Image3");
		helper.assertStatus(200);

		final ObjectNode img = helper.getValue();
		assertEquals(99, img.get("PID").asLong());
	}

	@Test
	public void testNavigationTwoHopUsingDefaultIdMappingHibernateStyle() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Persons('98')/Image3/PersonWithDefaultIdMapping");
		helper.assertStatus(200);

		final ObjectNode img = helper.getValue();
		assertEquals(97, img.get("ID").asLong());
	}

	@Test
	public void testNoNavigationOneEntity() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Organizations('3')");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("Third Org.", org.get("Name1").asText());
	}

	@Test
	public void testNavigationOneHopAndOrderBy() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations('3')/Roles?$orderby=RoleCategory desc");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(3, orgs.size());
		assertEquals("C", orgs.get(0).get("RoleCategory").asText());
		assertEquals("A", orgs.get(2).get("RoleCategory").asText());
	}

	@Test
	public void testNavigationOneHopReverse() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"BusinessPartnerRoles(BusinessPartnerID='2',RoleCategory='A')/BusinessPartner");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("2", org.get("ID").asText());
	}

	@Test
	public void testNavigationViaComplexType() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations('3')/AdministrativeInformation/Created/By");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("99", org.get("value").asText());
	}

	@Ignore("Requested navigation path currenlty not exisitng in model")
	@Test
	public void testNavigationViaComplexTypeTwoHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations('3')/AdministrativeInformation/Created/User/Address/AdministrativeDivision");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("3166-1", org.get("ParentCodeID").asText());
	}

	@Test
	public void testNavigationSelfToOneOneHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions(DivisionCode='BE352',CodeID='NUTS3',CodePublisher='Eurostat')/Parent");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("NUTS2", org.get("CodeID").asText());
		assertEquals("BE35", org.get("DivisionCode").asText());
	}

	@Test
	public void testNavigationSelfToOneTwoHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions(DivisionCode='BE352',CodeID='NUTS3',CodePublisher='Eurostat')/Parent/Parent");
		helper.assertStatus(200);

		final ObjectNode org = helper.getValue();
		assertEquals("NUTS1", org.get("CodeID").asText());
		assertEquals("BE3", org.get("DivisionCode").asText());
	}

	@Ignore("TODO")
	@Test
	public void testNavigationSelfToManyOneHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')/Children?$orderby=DivisionCode desc");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(5, orgs.size());
		assertEquals("NUTS2", orgs.get(0).get("CodeID").asText());
		assertEquals("BE25", orgs.get(0).get("DivisionCode").asText());
	}

	@Ignore("TODO")
	@Test
	public void testNavigationSelfToManyTwoHops() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisions(DivisionCode='BE2',CodeID='NUTS1',CodePublisher='Eurostat')/Children(DivisionCode='BE25',CodeID='NUTS2',CodePublisher='Eurostat')/Children?$orderby=DivisionCode desc");
		helper.assertStatus(200);

		final ArrayNode orgs = helper.getValues();
		assertEquals(8, orgs.size());
		assertEquals("NUTS3", orgs.get(0).get("CodeID").asText());
		assertEquals("BE258", orgs.get(0).get("DivisionCode").asText());
	}

	@Ignore("Currently not available")
	@Test
	public void testNavigationSelfToOneThreeHopsNoResult() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations('3')/Address/AdministrativeDivision/Parent/Parent");
		helper.assertStatus(204);
	}

	@Ignore("Currently not available")
	@Test
	public void testNavigationSelfToManyOneHopsNoResult() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations('3')/Address/AdministrativeDivision/Children");
		helper.assertStatus(204);
	}
}
