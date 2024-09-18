package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;

/**
 * Persistence adapter assuming to work with a resource local transaction
 * context.
 *
 * @author Ralf Zozmann
 *
 */
public class ResourceLocalPersistenceAdapter extends AbstractJPAAdapter {

  /**
   * Create adapter creating a {@link Persistence#createEntityManagerFactory(String, Map) EMF} with empty
   * property map.
   *
   * @see #ResourceLocalPersistenceAdapter(String, Map, AbstractJPADatabaseProcessor)
   */
  public ResourceLocalPersistenceAdapter(final String pUnit, final AbstractJPADatabaseProcessor dbAccessor) {
    this(pUnit, Collections.emptyMap(), dbAccessor);
  }

  /**
   * @see AbstractJPAAdapter#AbstractJPAAdapter(String, Map, AbstractJPADatabaseProcessor)
   */
  public ResourceLocalPersistenceAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
      final AbstractJPADatabaseProcessor dbAccessor) {
    super(pUnit, mapEntityManagerProperties, dbAccessor);
  }

  /**
   * @see AbstractJPAAdapter#AbstractJPAAdapter(String, EntityManagerFactory, AbstractJPADatabaseProcessor)
   */
  public ResourceLocalPersistenceAdapter(final String pUnit, final EntityManagerFactory emf,
      final AbstractJPADatabaseProcessor dbAccessor) {
    super(pUnit, emf, dbAccessor);
  }

  /**
   * @see AbstractJPAAdapter#AbstractJPAAdapter(String, EntityManagerFactory)
   */
  public ResourceLocalPersistenceAdapter(final String pUnit, final EntityManagerFactory emf) {
    super(pUnit, emf);
  }

  @Override
  public void beginTransaction(final EntityManager em) throws RuntimeException {
    em.getTransaction().begin();
  }

  @Override
  public void commitTransaction(final EntityManager em) throws RuntimeException {
    em.getTransaction().commit();
    em.close();
  }

  @Override
  public void cancelTransaction(final EntityManager em) throws RuntimeException {
    if (em.getTransaction().isActive()) {
      em.getTransaction().rollback();
    }
    em.clear();
    em.close();
  }

}
