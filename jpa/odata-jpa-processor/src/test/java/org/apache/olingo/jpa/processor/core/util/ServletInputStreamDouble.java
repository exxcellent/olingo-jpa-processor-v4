package org.apache.olingo.jpa.processor.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.junit.Assert;

public class ServletInputStreamDouble extends ServletInputStream {
	private final InputStream stream;

	public ServletInputStreamDouble(final ServletInputStream stream) {
		super();
		this.stream = stream;
	}

	public ServletInputStreamDouble(final StringBuffer stream) {
		super();
		if (stream != null) {
			this.stream = new ByteArrayInputStream(stream.toString().getBytes());
		} else {
			this.stream = null;
		}
	}

	@Override
	public int read() throws IOException {
		if (stream == null) {
			throw new IOException("Not available");
		}
		return stream.read();
	}

	@Override
	public int available() throws IOException {
		if (stream == null) {
			return 0;
		}
		return stream.available();
	}

	@Override
	public boolean isFinished() {
		return isReady();
	}

	@Override
	public boolean isReady() {
		try {
			return stream.available() > 0;
		} catch (final IOException e) {
			return false;
		}
	}

	@Override
	public void setReadListener(final ReadListener readListener) {
		Assert.fail();

	}

}
