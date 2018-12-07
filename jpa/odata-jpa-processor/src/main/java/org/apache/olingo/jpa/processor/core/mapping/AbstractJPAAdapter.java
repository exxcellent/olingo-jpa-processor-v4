package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;

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
	private final JPAODataDatabaseProcessor dbAccessor;
	private final EntityManagerFactory emf;
	private final Set<Class<?>> dtos = new HashSet<>();

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
			final JPAODataDatabaseProcessor dbAccessor) {
		this(pUnit, Persistence.createEntityManagerFactory(pUnit, mapEntityManagerProperties), dbAccessor);
	}

	/**
	 * Only for internal use; protect against usage outside of our package.
	 */
	AbstractJPAAdapter(final String pUnit, final EntityManagerFactory emf,
			final JPAODataDatabaseProcessor dbAccessor) throws IllegalArgumentException {
		this.namespace = pUnit;
		this.dbAccessor = dbAccessor;
		this.emf = emf;
		if (emf == null) {
			throw new IllegalArgumentException("EntityManagerFactory required");
		}
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
	 * @param dto
	 *            The class must have the annotation
	 *            {@link org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO}.
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
