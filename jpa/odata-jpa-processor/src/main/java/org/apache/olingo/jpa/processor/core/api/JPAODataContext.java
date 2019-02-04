package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.jpa.processor.core.database.JPAODataDatabaseOperations;
import org.apache.olingo.server.api.debug.DebugSupport;

public interface JPAODataContext extends JPAODataSessionContextAccess {

	@Deprecated
	public void setOperationConverter(final JPAODataDatabaseOperations jpaOperationConverter);

	public void setDatabaseProcessor(final JPAODataDatabaseProcessor databaseProcessor);

	public void setDebugSupport(final DebugSupport jpaDebugSupport);

	public void initDebugger(String debugFormat);
}
