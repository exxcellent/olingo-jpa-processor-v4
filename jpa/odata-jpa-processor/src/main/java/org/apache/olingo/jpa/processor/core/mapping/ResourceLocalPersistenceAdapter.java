package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;

/**
 * Persistence adapter assuming to work with a resource local transaction
 * context.
 *
 * @author Ralf Zozmann
 *
 */
public class ResourceLocalPersistenceAdapter extends AbstractJPAPersistenceAdapter {

	public ResourceLocalPersistenceAdapter(final String pUnit, final JPAODataDatabaseProcessor dbAccessor) {
		this(pUnit, null, dbAccessor);
	}

	public ResourceLocalPersistenceAdapter(final String pUnit, final Map<?, ?> mapEntityManagerProperties,
			final JPAODataDatabaseProcessor dbAccessor) {
		super(pUnit, mapEntityManagerProperties, dbAccessor);
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
