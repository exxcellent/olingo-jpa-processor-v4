package org.apache.olingo.jpa.processor.core.mapping;

import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;

/**
 * Generic implementation to map OData entities 1:1 to JPA entities.
 *
 * @see https://en.wikibooks.org/wiki/Java_Persistence/Transactions
 *
 * @author Ralf Zozmann
 *
 */
public abstract class AbstractJPAAdapter implements JPAAdapter {

  private final String namespace;
  private final AbstractJPADatabaseProcessor dbAccessor;
  private final EntityManagerFactory emf;
  private final Set<Class<?>> dtos = new LinkedHashSet<>();

  /**
   *
   * @param pUnit
   *            The name of the persistence unit is used also as namespace.
   *
   * @param mapEntityManagerProperties
   *            Maybe <code>null</code>
   * @param dbAccessor
   */
  public AbstractJPAAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
      final AbstractJPADatabaseProcessor dbAccessor) {
    this(pUnit, Persistence.createEntityManagerFactory(pUnit, mapEntityManagerProperties), dbAccessor);
  }

  /**
   * Only for internal use; protect against usage outside of our package.
   */
  AbstractJPAAdapter(final String pUnit, final EntityManagerFactory emf,
      final AbstractJPADatabaseProcessor dbAccessor) throws IllegalArgumentException {
    this.namespace = pUnit;
    this.dbAccessor = dbAccessor;
    this.emf = emf;
    if (dbAccessor == null) {
      throw new IllegalArgumentException("DB processor required");
    }
    if (emf == null) {
      throw new IllegalArgumentException("EntityManagerFactory required");
    }
    dbAccessor.initialize(emf.getCriteriaBuilder());
  }

  protected final EntityManagerFactory getEntityManagerFactory() {
    return emf;
  }

  @Override
  public EntityManager createEntityManager() throws RuntimeException {
    return getEntityManagerFactory().createEntityManager();
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public Metamodel getMetamodel() {
    return getEntityManagerFactory().getMetamodel();
  }

  @Override
  public JPAODataDatabaseProcessor getDatabaseAccessor() {
    return dbAccessor;
  }

  @Override
  public Collection<Class<?>> getDTOs() {
    return Collections.unmodifiableCollection(dtos);
  }

	/**
   *
   * @param dto The class must have the annotation
   * {@link org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO @ODataDTO}.
   */
  public void registerDTO(final Class<?> dto) {
    if (dto == null) {
      throw new IllegalArgumentException("DTO class required");
    }
    dtos.add(dto);
  }

  @Override
  public void dispose() {
    emf.close();
  }
}
