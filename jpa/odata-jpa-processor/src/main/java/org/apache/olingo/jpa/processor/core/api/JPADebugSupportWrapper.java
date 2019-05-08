package org.apache.olingo.jpa.processor.core.api;

import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.debug.DebugInformation;
import org.apache.olingo.server.api.debug.DebugSupport;

class JPADebugSupportWrapper implements DebugSupport {

	final private DebugSupport debugSupport;
	private JPAServiceDebugger debugger;

	public JPADebugSupportWrapper(final DebugSupport debugSupport) {
		super();
		this.debugSupport = debugSupport;
	}

	@Override
	public void init(final OData odata) {
		debugSupport.init(odata);
	}

	@Override
	public boolean isUserAuthorized() {
		return debugSupport.isUserAuthorized();
	}

	@Override
	public ODataResponse createDebugResponse(final String debugFormat, final DebugInformation debugInfo) {
		debugInfo.getRuntimeInformation().addAll(debugger.getRuntimeInformation());
		return debugSupport.createDebugResponse(debugFormat, debugInfo);
	}

	void setDebugger(final JPAServiceDebugger debugger) {
		this.debugger = debugger;
	}
}