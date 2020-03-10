package org.apache.olingo.jpa.processor.impl.deserializer;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.UploadContext;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.server.api.ODataRequest;

class ODataRequestContext implements UploadContext {

	private final String contentType;
	private final InputStream streamBody;

	ODataRequestContext(final ODataRequest request, final InputStream streamBody) {
		contentType = request.getHeader(HttpHeader.CONTENT_TYPE);
		this.streamBody = streamBody;
	}

	@Override
	public String getCharacterEncoding() {
		// force default Charset name to ByteArrayOutputStream.toString(...)
		return null;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public int getContentLength() {
		return (int) contentLength();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return streamBody;
	}

	@Override
	public long contentLength() {
		try {
			return streamBody.available();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}
