package org.apache.olingo.jpa.processor.api;

import java.util.Enumeration;
import java.util.Locale;

import javax.persistence.EntityManager;

import org.apache.olingo.jpa.processor.conversion.TransformingFactory;
import org.apache.olingo.jpa.processor.core.api.JPAServiceDebugger;
import org.apache.olingo.server.api.debug.DebugSupport;

/**
 * The request context knows all about the global context and also the request specific things.
 *
 */
public interface JPAODataSessionContextAccess extends JPAODataGlobalContext {

  /**
   *
   * @return The dpi with request scope including {@link JPAODataGlobalContext#getDependencyInjector() global dpi} data.
   */
  @Override
  public DependencyInjector getDependencyInjector();

  public TransformingFactory getTransformerFactory();

  /**
   *
   * @see javax.servlet.http.HttpServletRequest#getHeader(String)
   * @return The (HTTP) request (header) value of given <i>name</i> or <code>null</code>.
   */
  public String getParameter(String name);

  /**
   *
   * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
   * @return The (HTTP) request (header) values of given <i>name</i> or <code>null</code>.
   */
  public Enumeration<String> getParameters(String name);

  /**
   *
   * @return The JPA session to access database related aspects.
   */
  public EntityManager getEntityManager();

  /**
   *
   * @return The preferred locale, never <code>null</code>.
   */
  public Locale getLocale();

  public JPAServiceDebugger getServiceDebugger();

  public DebugSupport getDebugSupport();

  public void setDebugSupport(final DebugSupport jpaDebugSupport);

}
