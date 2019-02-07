package org.apache.olingo.jpa.processor.core.api;

import static org.junit.Assert.fail;

import java.util.List;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.jpa.metadata.api.JPAEdmProvider;
import org.apache.olingo.jpa.processor.core.mapping.JPAAdapter;
import org.apache.olingo.jpa.processor.core.util.DependencyInjector;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.debug.DebugSupport;

public class JPAODataContextAccessDouble implements JPAODataSessionContextAccess {
	private final JPAEdmProvider edmProvider;
	private final JPAAdapter persistenceAdapter;
	private final DependencyInjector dpi = new DependencyInjector();

	public JPAODataContextAccessDouble(final JPAEdmProvider edmProvider,
			final JPAAdapter persistenceAdapter) {
		super();
		this.edmProvider = edmProvider;
		this.persistenceAdapter = persistenceAdapter;
	}

	@Override
	public List<EdmxReference> getReferences() {
		fail();
		return null;
	}

	@Override
	public DebugSupport getDebugSupport() {
		fail();
		return null;
	}

	@Override
	public JPAEdmProvider getEdmProvider() {
		return edmProvider;
	}

	@Override
	public JPAODataDatabaseProcessor getDatabaseProcessor() {
		return persistenceAdapter.getDatabaseAccessor();
	}

	@Override
	public JPAServiceDebugger getDebugger() {
		return new JPAEmptyDebugger();
	}

	@Override
	public OData getOdata() {
		return null;
	}

	@Override
	public DependencyInjector getDependencyInjector() {
		return dpi;
	}

}
