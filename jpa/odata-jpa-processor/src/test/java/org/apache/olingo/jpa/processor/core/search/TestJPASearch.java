package org.apache.olingo.jpa.processor.core.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;

public class TestJPASearch extends TestBase {

	@Test
	public void testAllAttributesSimpleCase() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"Organizations?$search = Org");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ArrayNode ents = helper.getValues();
		// Nameline1 should match for all Organizations
		assertEquals(10, ents.size());
	}

	@Test
	public void testMultipleAttributesSearchWithPhrase() throws IOException, ODataException {
		// skip test with Hibernate
		assumeTrue(
				"Hibernate has a stupid parameter binding check not accepting '%001%' as apptern for java.net.URL attribute",
				getJPAProvider() != JPAProvider.Hibernate);

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DatatypeConversionEntities?$search = \"001\"");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final ArrayNode ents = helper.getValues();
		// should have 1 result for matching UUID
		assertEquals(1, ents.size());
		assertTrue(ents.get(0).get("Uuid").asText().endsWith("001"));
	}

	@Test
	public void testMultipleAttributesSearchWithComplexExpression() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter,
				"DatatypeConversionEntities?$search = anywhere OR \"888\"");
		// not supported -> TODO
		helper.execute(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode());

	}

}
