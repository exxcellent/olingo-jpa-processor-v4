package org.apache.olingo.jpa.processor.core.util;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpServletResponseDouble implements HttpServletResponse {

  private int setStatus;
  private final TestingOutputStream outputStream = new TestingOutputStream();
  private final HashMap<String, List<String>> headers = new HashMap<String, List<String>>();

  @Override
  public String getCharacterEncoding() {
    fail();
    return null;
  }

  @Override
  public String getContentType() {
    if (headers.containsKey(HttpRequestHeaderDouble.HEADER_CONTENT_TYPE)) {
      return headers.get(HttpRequestHeaderDouble.HEADER_CONTENT_TYPE).get(0);
    }
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
    return this.outputStream.getSize();
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
    List<String> headerValues = headers.get(name);
    if (headerValues == null) {
      headerValues = new ArrayList<String>(1);
      headers.put(name, headerValues);
    }
    headerValues.add(value);
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

  class TestingOutputStream extends ServletOutputStream {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024 * 1024);

    @Override
    public void write(final int b) throws IOException {
      buffer.write(b);
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

  public InputStream getInputStream() {
    return new ByteArrayInputStream(this.outputStream.buffer.toByteArray());
  }

  @Override
  public void setContentLengthLong(final long len) {
    fail();
  }

  @Override
  public String getHeader(final String name) {
    fail();
    return null;
  }

  @Override
  public Collection<String> getHeaders(final String name) {
    fail();
    return null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    fail();
    return null;
  }
}
