package org.apache.olingo.jpa.servlet.example;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.cud.ODataDeleteRequest;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityCreateRequest;
import org.apache.olingo.client.api.communication.request.cud.ODataEntityUpdateRequest;
import org.apache.olingo.client.api.communication.request.cud.UpdateType;
import org.apache.olingo.client.api.communication.request.invoke.ODataInvokeRequest;
import org.apache.olingo.client.api.communication.request.retrieve.EdmMetadataRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataValueRequest;
import org.apache.olingo.client.api.communication.response.ODataDeleteResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityCreateResponse;
import org.apache.olingo.client.api.communication.response.ODataEntityUpdateResponse;
import org.apache.olingo.client.api.communication.response.ODataInvokeResponse;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientInvokeResult;
import org.apache.olingo.client.api.domain.ClientObjectFactory;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientValue;
import org.apache.olingo.client.api.uri.URIBuilder;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to define a connection to an servlet container hosting the example servlet as oData endpoint.
 *
 */
class ODataEndpointTestDefinition {

	private static Logger LOG = LoggerFactory.getLogger(ODataEndpointTestDefinition.class);

	/**
	 * Select the type of connection protocol.
	 *
	 */
	public static enum ConnectionSetting {
		HTTP;
	}

	private final static String SERVER_HOST = System.getProperty("service.target.server.hostname", "localhost");
	protected final static String TEST_USER_NAME = "user1";
	protected final static String TEST_USER_PASSWORD = "secret123";

	/**
	 * The default port for HTTPS on local/QA servlet container.
	 */
	@SuppressWarnings("unused")
	private final static int SERVER_PORT_HTTPS = 8443;
	private final static int SERVER_PORT_HTTP = 8080;

	/**
	 * @see org.apache.olingo.jpa.servlet.example.ODataServlet
	 */
	private final static String WEB_XML_WEBAPP_BASEPATH = "odata";

	private String protocolString = null;
	private String portString = null;
	private String targetUri = null;
	private final ODataClient client;

	public ODataEndpointTestDefinition() {
		client = ODataClientFactory.getClient();
		switchConnectionSetting(ConnectionSetting.HTTP);
	}

	/**
	 * Change the used port the new one.
	 */
	protected void switchConnectionSetting(final ConnectionSetting protocol) {
		if (protocol == null) {
			throw new IllegalArgumentException("Protocol must be given");
		}
		switch (protocol) {
		case HTTP:
			this.portString = System.getProperty("service.target.server.http_port", Integer.toString(SERVER_PORT_HTTP));
			protocolString = "http";
			break;
		default:
			throw new UnsupportedOperationException("not implemented yet");
		}
		refreshBaseUri();
	}

	private void refreshBaseUri() {
		if (portString == null) {
			return;
		}
		targetUri = protocolString + "://" + SERVER_HOST + ":" + this.portString + "/" + WEB_XML_WEBAPP_BASEPATH;
	}

	/**
	 * @return The current URI targeting the service host+path...
	 */
	private String getTargetUri() {
		return targetUri;
	}

	private String determineAuthorization() throws RuntimeException {
		try {
			return "Basic "
			        + new String(Base64.encodeBase64(new String(TEST_USER_NAME + ":" + TEST_USER_PASSWORD).getBytes("UTF-8")), "US-ASCII");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public URIBuilder newUri() {
		return client.newURIBuilder(getTargetUri());
	}

	public ClientObjectFactory getObjectFactory() {
		return client.getObjectFactory();
	}

	public ODataRetrieveResponse<Edm> retrieveMetadata() {
		LOG.info("Call metadata uri {}...", newUri().appendMetadataSegment().build().toString());
		final EdmMetadataRequest req = client.getRetrieveRequestFactory().getMetadataRequest(getTargetUri());
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public ODataRetrieveResponse<ClientEntity> retrieveEntity(final URIBuilder uriBuilder, final String description) {
		final URI uri = uriBuilder.build();
		LOG.info((description != null ? description : "") + "\n{}...", uri);
		final ODataEntityRequest<ClientEntity> req = client.getRetrieveRequestFactory().getEntityRequest(uri);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public ODataRetrieveResponse<ClientEntitySet> retrieveEntityCollection(final URIBuilder uriBuilder, final String description) {
		final URI uri = uriBuilder.build();
		LOG.info((description != null ? description : "") + "\n{}...", uri);
		final ODataEntitySetRequest<ClientEntitySet> req = client.getRetrieveRequestFactory().getEntitySetRequest(uri);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public <E extends ClientEntity> ODataEntityCreateResponse<E> createEntity(final URIBuilder uriBuilder, final E entity) {
		final URI uri = uriBuilder.build();
		LOG.info("Create entity\n{}...", uri);
		final ODataEntityCreateRequest<E> req = client.getCUDRequestFactory().getEntityCreateRequest(uri, entity);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public <E extends ClientEntity> ODataEntityUpdateResponse<E> updateEntity(final URIBuilder uriBuilder, final E entity) {
		final URI uri = uriBuilder.build();
		LOG.info("Update entity\n{}...", uri);
		final ODataEntityUpdateRequest<E> req = client.getCUDRequestFactory().getEntityUpdateRequest(uri, UpdateType.PATCH, entity);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public ODataRetrieveResponse<ClientPrimitiveValue> retrieveValue(final URIBuilder uriBuilder, final String description) {
		final URI uri = uriBuilder.build();
		LOG.info((description != null ? description : "") + "\n{}...", uri);
		final ODataValueRequest req = client.getRetrieveRequestFactory().getValueRequest(uri);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public ODataDeleteResponse deleteEntity(final URIBuilder uriBuilder, final String description) {
		final URI uri = uriBuilder.build();
		LOG.info((description != null ? description : "") + "\n{}...", uri);
		final ODataDeleteRequest req = client.getCUDRequestFactory().getDeleteRequest(uri);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public <T extends ClientInvokeResult> ODataInvokeResponse<T> callFunction(final URIBuilder uriBuilder, final Class<T> resultType,
	        final Map<String, ClientValue> functionParameters) {
		final URI uri = uriBuilder.build();
		final StringBuilder paramBuffer = new StringBuilder();
		if (functionParameters == null || functionParameters.isEmpty()) {
			paramBuffer.append("-");
		} else {
			int index = 0;
			for (final Map.Entry<String, ClientValue> entry : functionParameters.entrySet()) {
				if (index > 0) {
					paramBuffer.append(", ");
				}
				paramBuffer.append(entry.getKey()).append("=").append(entry.getValue().toString());
				index++;
			}
		}
		LOG.info("Call function with parameters: {}" + "\n{}...", paramBuffer, uri);
		final ODataInvokeRequest<T> req = client.getInvokeRequestFactory().getFunctionInvokeRequest(uri, resultType, functionParameters);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

	public <T extends ClientInvokeResult> ODataInvokeResponse<T> callAction(final URIBuilder uriBuilder, final Class<T> resultType,
	        final Map<String, ClientValue> actionParameters) {
		URI uri = uriBuilder.build();
		// the uri builder is buggy, because OLingo does not accept tailing '()' for operations without parameters
		if (uri.toString().endsWith("()")) {
			final String uriString = uri.toString();
			uri = URI.create(uriString.substring(0, uriString.length() - 2));
		}
		final StringBuilder paramBuffer = new StringBuilder();
		if (actionParameters == null || actionParameters.isEmpty()) {
			paramBuffer.append("-");
		} else {
			int index = 0;
			for (final Map.Entry<String, ClientValue> entry : actionParameters.entrySet()) {
				if (index > 0) {
					paramBuffer.append(", ");
				}
				paramBuffer.append(entry.getKey()).append("=").append(entry.getValue().toString());
				index++;
			}
		}
		LOG.info("Call action with parameters: {}" + "\n{}...", paramBuffer, uri);
		final ODataInvokeRequest<T> req = client.getInvokeRequestFactory().getActionInvokeRequest(uri, resultType, actionParameters);
		req.addCustomHeader("Authorization", determineAuthorization());
		return req.execute();
	}

}
