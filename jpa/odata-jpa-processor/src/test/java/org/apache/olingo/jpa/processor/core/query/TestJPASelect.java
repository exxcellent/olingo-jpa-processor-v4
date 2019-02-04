package org.apache.olingo.jpa.processor.core.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestJPASelect extends TestBase {

	@Test
	public void testSimpleGet() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "Persons('99')");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ObjectNode p = helper.getValue();
		assertEquals(99, p.get("ID").asLong());
	}

	/**
	 * Test working datatype conversion between JPA and OData entity.
	 */
	@Test
	public void testDatatypeConversionEntities() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "DatatypeConversionEntities(1)");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ObjectNode p = helper.getValue();
		assertEquals(1, p.get("ID").asLong());
	}

	@Test
	public void testSelectEmbeddedId() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"AdministrativeDivisionDescriptions?$select=CodePublisher,DivisionCode&$filter=CodeID eq 'NUTS3'");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode adds = helper.getValues();
		assertEquals(88, adds.size());
		// Not selected non key attributes must not be set
		assertNull(adds.get(0).get("Name"));

	}

	@Test
	public void testSelectRelationshipTargets() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipSourceEntities(1)/Targets");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(2, targets.size());
	}

	@Test
	public void testSelectRelationshipSource() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipTargetEntities(3)/Source");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ObjectNode source = helper.getValue();
		assertNotNull(source);
		assertEquals(1, source.get("ID").asInt());
	}

	@Test
	public void testSelectRelationshipM2NLeftNavigation() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipSourceEntities(1)/LeftM2Ns");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(1, targets.size());
	}

	@Test
	public void testSelectRelationshipM2NRightNavigation() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipTargetEntities(5)/RightM2Ns");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(1, targets.size());
	}

	@Test
	public void testSelectRelationshipOne2Many() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipTargetEntities(5)/One2ManyTest");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(1, targets.size());
	}

	@Test
	public void testSelectRelationshipSecondM2NLeftNavigation() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipEntities(2)/SecondLeftM2Ns");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(1, targets.size());
	}

	@Test
	public void testSelectRelationshipSecondM2NRightNavigation() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"RelationshipEntities(4)/SecondRightM2Ns");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(3, targets.size());
	}

	@Test
	public void testSelectRelationshipM2NBusinessPartnerRoles() throws IOException, ODataException {
		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"BusinessPartners('5')/Locations");

		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ArrayNode targets = helper.getValues();
		assertEquals(2, targets.size());
	}

}
