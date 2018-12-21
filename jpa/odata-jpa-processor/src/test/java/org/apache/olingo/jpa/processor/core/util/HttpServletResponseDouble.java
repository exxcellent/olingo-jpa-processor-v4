package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpServletResponseDouble implements HttpServletResponse {

	private int setStatus;
	private final ServletOutputStream outputStream = new OutPutStream();

	@Override
	public String getCharacterEncoding() {
		fail();
		return null;
	}

	@Override
	public String getContentType() {
		fail();
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return this.outputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		fail();
		return null;
	}

	@Override
	public void setCharacterEncoding(final String charset) {
		fail();

	}

	@Override
	public void setContentLength(final int len) {
		fail();

	}

	@Override
	public void setContentType(final String type) {
		fail();

	}

	@Override
	public void setBufferSize(final int size) {
		fail();

	}

	@Override
	public int getBufferSize() {
		return ((OutPutStream) this.outputStream).getSize();
	}

	@Override
	public void flushBuffer() throws IOException {
		fail();

	}

	@Override
	public void resetBuffer() {
		fail();

	}

	@Override
	public boolean isCommitted() {
		fail();
		return false;
	}

	@Override
	public void reset() {
		fail();

	}

	@Override
	public void setLocale(final Locale loc) {
		fail();

	}

	@Override
	public Locale getLocale() {
		fail();
		return null;
	}

	@Override
	public void addCookie(final Cookie cookie) {
		fail();

	}

	@Override
	public boolean containsHeader(final String name) {
		fail();
		return false;
	}

	@Override
	public String encodeURL(final String url) {
		fail();
		return null;
	}

	@Override
	public String encodeRedirectURL(final String url) {
		fail();
		return null;
	}

	@Override
	public String encodeUrl(final String url) {
		fail();
		return null;
	}

	@Override
	public String encodeRedirectUrl(final String url) {
		fail();
		return null;
	}

	@Override
	public void sendError(final int sc, final String msg) throws IOException {
		// TODO do not ignore message?
		setStatus(sc);
	}

	@Override
	public void sendError(final int sc) throws IOException {
		sendError(sc, null);
	}

	@Override
	public void sendRedirect(final String location) throws IOException {
		fail();

	}

	@Override
	public void setDateHeader(final String name, final long date) {
		fail();

	}

	@Override
	public void addDateHeader(final String name, final long date) {
		fail();

	}

	@Override
	public void setHeader(final String name, final String value) {
		fail();

	}

	@Override
	public void addHeader(final String name, final String value) {
		// TODO
	}

	@Override
	public void setIntHeader(final String name, final int value) {
		fail();

	}

	@Override
	public void addIntHeader(final String name, final int value) {
		fail();

	}

	@Override
	public void setStatus(final int sc) {
		this.setStatus = sc;
	}

	@Override
	public void setStatus(final int sc, final String sm) {
		fail();

	}

	@Override
	public int getStatus() {
		return setStatus;
	}

	class OutPutStream extends ServletOutputStream {
		List<Integer> buffer = new ArrayList<Integer>();

		@Override
		public void write(final int b) throws IOException {
			buffer.add(new Integer(b));
		}

		public Iterator<Integer> getBuffer() {
			return buffer.iterator();
		}

		public int getSize() {
			return buffer.size();
		}

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(final WriteListener writeListener) {
			fail();
		}
	}

	//
	class ResultStream extends InputStream {
		private final Iterator<Integer> bufferExcess;

		public ResultStream(final OutPutStream buffer) {
			super();
			this.bufferExcess = buffer.getBuffer();
		}

		@Override
		public int read() throws IOException {
			if (bufferExcess.hasNext()) {
				return bufferExcess.next().intValue();
			}
			return -1;
		}

	}

	public InputStream getInputStream() {

		return new ResultStream((OutPutStream) this.outputStream);
	}

	@Override
	public void setContentLengthLong(final long len) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHeader(final String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaders(final String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}
}
