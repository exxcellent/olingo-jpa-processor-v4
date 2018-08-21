package org.apache.olingo.jpa.processor.core.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPAEnumFilter extends TestBase {

	@Test
	public void testEnumStringBasedEq() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DatatypeConversionEntities?$filter=AStringMappedEnum eq java.time.chrono.IsoEra'CE'");
		helper.assertStatus(200);

		final ArrayNode dces = helper.getValues();
		assertEquals(1, dces.size());
		assertEquals("CE", dces.get(0).get("AStringMappedEnum").asText());
	}

	@Test
	public void testEnumStringBasedNe() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DatatypeConversionEntities?$filter=AStringMappedEnum ne java.time.chrono.IsoEra'BCE'");
		helper.assertStatus(200);

		final ArrayNode dces = helper.getValues();
		assertTrue(dces.size() > 0);
	}

	@Test
	public void testEnumOrdinalBased() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DatatypeConversionEntities?$filter=AOrdinalMappedEnum eq java.time.temporal.ChronoUnit'NANOS'");
		helper.assertStatus(200);

		final ArrayNode dces = helper.getValues();
		assertEquals(1, dces.size());
		assertEquals("NANOS", dces.get(0).get("AOrdinalMappedEnum").asText());
	}

}
