package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.metamodel.Metamodel;

import org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType;
import org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO;
import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.AbstractJPADatabaseProcessor;
import org.apache.olingo.jpa.processor.core.database.JPA_DefaultDatabaseProcessor;

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
  private final Set<Class<?>> cts = new LinkedHashSet<>();

  /**
   * Convenience constructor to use the {@link JPA_DefaultDatabaseProcessor} for database access.
   */
  protected AbstractJPAAdapter(final String pUnit, final EntityManagerFactory emf) throws IllegalArgumentException {
    this(pUnit, emf, new JPA_DefaultDatabaseProcessor());
  }

  /**
   *
   * @param pUnit
   * The name of the persistence unit is used also as namespace.
   *
   * @param mapEntityManagerProperties
   * Maybe <code>null</code>
   * @param dbAccessor
   */
  protected AbstractJPAAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
      final AbstractJPADatabaseProcessor dbAccessor) throws IllegalArgumentException {
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
  public final Collection<Class<?>> getDTOEntityTypes() {
    return Collections.unmodifiableCollection(dtos);
  }

  @Override
  public final Collection<Class<?>> getDTOComplexTypes() {
    return Collections.unmodifiableCollection(cts);
  }
  
  /**
   * @param dto The class must have the annotation
   * {@link org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO @ODataDTO}.
   */
  public final void registerDTOEntityType(final Class<?> dto) {
    if (dto == null || !dto.isAnnotationPresent(ODataDTO.class)) {
      throw new IllegalArgumentException("DTO class required");
    }
    if(dto.isAnnotationPresent(Entity.class) || dto.isAnnotationPresent(Embeddable.class)) {
      throw new IllegalArgumentException("DTO must not be an @Entity or @Embeddable");
    }
    dtos.add(dto);
  }

  /**
   * @param ct The class must have the annotation
   * {@link org.apache.olingo.jpa.metadata.core.edm.complextype.ODataComplexType @ODataComplexType}.
   */
  public final void registerDTOComplexType(final Class<?> ct) {
    if (ct == null || !ct.isAnnotationPresent(ODataComplexType.class)) {
      throw new IllegalArgumentException("ComplexType class required");
    }
    if(ct.isAnnotationPresent(Entity.class) || ct.isAnnotationPresent(Embeddable.class)) {
      throw new IllegalArgumentException("ComplexType must not be an @Entity or @Embeddable");
    }
    cts.add(ct);
  }
  
  @Override
  public void dispose() {
    emf.close();
  }
}
