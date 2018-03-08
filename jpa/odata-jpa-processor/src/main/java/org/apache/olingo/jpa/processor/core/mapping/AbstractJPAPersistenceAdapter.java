package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Map;

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
public abstract class AbstractJPAPersistenceAdapter implements JPAPersistenceAdapter {

	private final String namespace;
	private final JPAODataDatabaseProcessor dbAccessor;
	private final EntityManagerFactory emf;

	/**
	 *
	 * @param pUnit
	 *            The name of the persistence unit is used also as namespace.
	 *
	 * @param mapEntityManagerProperties
	 *            Maybe <code>null</code>
	 * @param dbAccessor
	 */
	public AbstractJPAPersistenceAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
			final JPAODataDatabaseProcessor dbAccessor) {
		this(pUnit, Persistence.createEntityManagerFactory(pUnit, mapEntityManagerProperties), dbAccessor);
	}

	/**
	 * Only for internal use; protect against usage outside of our package.
	 */
	AbstractJPAPersistenceAdapter(final String pUnit, final EntityManagerFactory emf,
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

}
