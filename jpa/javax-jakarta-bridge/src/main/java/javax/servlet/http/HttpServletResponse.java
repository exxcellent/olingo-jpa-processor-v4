package javax.servlet.http;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * @deprecated This fake class exists only to make Olingo runable in a Jakarta environment. If Olingo supports Jakarta
 * remove the complete javax-jakarta-bridge module.
 */
@Deprecated
public interface HttpServletResponse extends jakarta.servlet.http.HttpServletResponse {
  @Override
  public ServletOutputStream getOutputStream() throws IOException;

}
