package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.util.IntegrationTestHelper;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

public class TestODataBasics extends TestBase {

	@Test
	public void testMetadata() throws IOException, ODataException {

		final IntegrationTestHelper helper = new IntegrationTestHelper(persistenceAdapter, "$metadata");
		helper.assertStatus(200);

		final String metadata = helper.getRawResult();
		assertNotNull(metadata);
		assertTrue(metadata.length() > 1);
	}

}
