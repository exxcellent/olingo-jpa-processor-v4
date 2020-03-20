package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.processor.core.util.ServerCallSimulator;
import org.apache.olingo.jpa.processor.core.util.TestBase;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestODataBasics extends TestBase {

	@Test
	public void testMetadata() throws IOException, ODataException {

		final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "$metadata");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final String metadata = helper.getRawResult();
		assertNotNull(metadata);
		assertTrue(metadata.length() > 1);
	}

	@Test
	public void testService() throws IOException, ODataException {

		final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "");
		helper.execute(HttpStatusCode.OK.getStatusCode());

		final String servicedata = helper.getRawResult();
		assertNotNull(servicedata);
		assertTrue(servicedata.length() > 1);
	}

	@Test
	public void testAll() throws IOException, ODataException {

		final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "$all");
		helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
	}

	@Test
	public void testCrossjoin() throws IOException, ODataException {

		final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter,
				"$crossjoin(Persons,PersonImages)");
		helper.execute(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode());
	}

	@Test
	public void testEntityId() throws IOException, ODataException {

		final ServerCallSimulator helper = new ServerCallSimulator(persistenceAdapter, "$entity?$id=Persons('99')");
		helper.execute(HttpStatusCode.OK.getStatusCode());
		final ObjectNode person = helper.getJsonObjectValue();
		assertNotNull(person);
	}

}
