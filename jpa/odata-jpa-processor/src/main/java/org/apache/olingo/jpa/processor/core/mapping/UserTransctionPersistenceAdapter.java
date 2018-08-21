package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;

/**
 * Persistence adapter with separate {@link UserTransaction transaction}
 * behaviour. This adapter should be used for JTA controlled context.
 *
 * @author Ralf Zozmann
 *
 */
public class UserTransctionPersistenceAdapter extends AbstractJPAAdapter {

	private final static String DEFAULT_USERTRANSACTION_NAME = "java:comp/UserTransaction";

	private UserTransaction transactionAccessor = null;
	private final String jndiUserTxName;


	/**
	 * @see #UserTransctionPersistenceAdapter(String, Map,
	 *      JPAODataDatabaseProcessor)
	 */
	public UserTransctionPersistenceAdapter(final String namespace, final JPAODataDatabaseProcessor dbAccessor) {
		this(namespace, null, dbAccessor);
	}

	/**
	 * {@inheritDoc}
	 */
	public UserTransctionPersistenceAdapter(final String namespace, final Map<?, ?> mapEntityManagerProperties,
			final JPAODataDatabaseProcessor dbAccessor) {
		super(namespace, mapEntityManagerProperties, dbAccessor);
		this.jndiUserTxName = DEFAULT_USERTRANSACTION_NAME;
	}

	/**
	 * Use a user transaction got via JNDI lookup for
	 * {@link #DEFAULT_USERTRANSACTION_NAME}.
	 *
	 * @see #UserTransctionPersistenceAdapter(String, JPAODataDatabaseProcessor,
	 *      EntityManagerFactory, String)
	 */
	public UserTransctionPersistenceAdapter(final String namespace, final JPAODataDatabaseProcessor dbAccessor,
			final EntityManagerFactory entityManagerFactory) throws IllegalArgumentException {
		this(namespace, dbAccessor, entityManagerFactory, DEFAULT_USERTRANSACTION_NAME);
	}

	/**
	 *
	 * @param namespace
	 *            The namespace (persistence unit name from persistence.xml) used
	 *            for Odata entities.
	 * @param dbAccessor
	 *            The helper to access database with proper SQL dialect.
	 * @param entityManagerFactory
	 *            The entity manager factory from outside; normally injected via
	 *            {@link javax.persistence.PersistenceUnit @PersistenceUnit}.
	 * @param jndiUserTxName
	 *            The JNDI name used to lookup for user transaction.
	 */
	public UserTransctionPersistenceAdapter(final String namespace, final JPAODataDatabaseProcessor dbAccessor,
			final EntityManagerFactory entityManagerFactory, final String jndiUserTxName)
					throws IllegalArgumentException {
		super(namespace, entityManagerFactory, dbAccessor);
		this.jndiUserTxName = jndiUserTxName;
		if (jndiUserTxName == null) {
			throw new IllegalArgumentException("JNDI name for user transaction required");
		}
	}

	@Override
	public void beginTransaction(final EntityManager em) throws RuntimeException {
		if (transactionAccessor == null) {
			try {
				transactionAccessor = (UserTransaction) new InitialContext().lookup(jndiUserTxName);
			} catch (final NamingException e) {
				throw new RuntimeException(
						"Lookup for user transaction (" + jndiUserTxName + ") failed", e);
			}
		}
		try {
			transactionAccessor.begin();
		} catch (NotSupportedException | SystemException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void commitTransaction(final EntityManager em) throws RuntimeException {
		try {
			em.joinTransaction();
			em.flush();
			transactionAccessor.commit();
		} catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException e) {
			throw new RuntimeException(e);
		}
		em.close();
	}

	@Override
	public void cancelTransaction(final EntityManager em) throws RuntimeException {
		try {
			em.clear();
			transactionAccessor.rollback();
		} catch (final SystemException e) {
			throw new RuntimeException(e);
		}
		em.close();
	}

}
