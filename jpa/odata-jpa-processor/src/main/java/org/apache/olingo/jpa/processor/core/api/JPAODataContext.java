package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.server.api.debug.DebugSupport;

public interface JPAODataContext extends JPAODataSessionContextAccess {

	public void setDebugSupport(final DebugSupport jpaDebugSupport);

}
