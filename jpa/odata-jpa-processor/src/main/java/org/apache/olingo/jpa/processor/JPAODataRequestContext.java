package org.apache.olingo.jpa.processor;

import java.util.Locale;

import javax.persistence.EntityManager;

import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.jpa.processor.core.api.JPAServiceDebugger;
import org.apache.olingo.jpa.processor.core.util.DependencyInjectorImpl;
import org.apache.olingo.jpa.processor.transformation.TransformingFactory;
import org.apache.olingo.server.api.debug.DebugSupport;

/**
 * The request context knows all about the global context and also the (currently processed) request specific things.
 *
 */
public interface JPAODataRequestContext extends JPAODataGlobalContext {

  /**
   *
   * @return The dependency injector with request scope including {@link JPAODataGlobalContext#getDependencyInjector()
   * global dpi} data.
   */
  @Override
  public DependencyInjector getDependencyInjector();

  public TransformingFactory getTransformerFactory();

  /**
   *
   * @see javax.servlet.http.HttpServletRequest#getHeader(String)
   * @return The (HTTP) request (header) value of given <i>name</i> or <code>null</code>.
   */
  //  public String getParameter(String name);

  /**
   *
   * @see javax.servlet.http.HttpServletRequest#getHeaders(String)
   * @return The (HTTP) request (header) values of given <i>name</i> or <code>null</code>.
   */
  //  public Enumeration<String> getParameters(String name);

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

  //  public DebugSupport getDebugSupport();

  public void setDebugSupport(final DebugSupport jpaDebugSupport);

  /**
   * Create a new instance of request context sharing the same values as the creator, but with a few differences:
   * <ul>
   * <li>A fresh instance of {@link #getDependencyInjector() dependency injector} is used that can have separate
   * dependency values. A possible registered {@link DependencyInjectorImpl#getDependencyValue(Class) dependency value}
   * for {@link JPAODataRequestContext} will be replaced the the new derived one.</li>
   * </ul>
   * @return A new derived instance of request context.
   * @throws ODataException
   */
  public JPAODataRequestContext createSubRequestContext() throws ODataException;
}
