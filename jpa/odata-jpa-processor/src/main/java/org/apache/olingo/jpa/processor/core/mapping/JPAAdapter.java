package org.apache.olingo.jpa.processor.core.mapping;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;

import org.apache.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;

/**
 * Implementations of this interface will map between OData entities and JPA
 * entities.<br/>
 * The concrete implementation of the transaction concept in this persistence
 * adapter allows the following scenarios:
 * <ul>
 * <li>Working on resource transaction</li>
 * <li>Working on JTA transaction (not recommended to avoid polluting of JTA
 * context with invalid entities)</li>
 * <li>Working on separate (user) transaction, a combination with 'join
 * transaction' scenario possible</li>
 * </ul>
 *
 * @author Ralf Zozmann
 *
 */
public interface JPAAdapter {

	/**
	 * After creating a new entity manager a call to
	 * {@link #beginTransaction(EntityManager)} will happen.
	 */
	public EntityManager createEntityManager() throws RuntimeException;

	/**
	 *
	 * @see javax.transaction.UserTransaction#begin()
	 */
	public void beginTransaction(EntityManager em) throws RuntimeException;

	/**
	 * Finish usage of entity manager (transaction).
	 *
	 * @see javax.transaction.UserTransaction#commit()
	 */
	public void commitTransaction(EntityManager em) throws RuntimeException;

	/**
	 * Cancel usage of entity manager (transaction).
	 *
	 * @see javax.transaction.UserTransaction#rollback()
	 */
	public void cancelTransaction(EntityManager em) throws RuntimeException;

	/**
	 *
	 * @return The name space used on OData for JPA entities.
	 */
	public String getNamespace();

	public Metamodel getMetamodel();

	/**
	 * @return An empty, non empty or <code>null</code> collection of non persistent
	 *         (non JPA) POJOs classes to handle as OData entities.
	 * @see org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO @ODataDTO
	 *      annotation
	 */
	public Collection<Class<?>> getDTOs();

	public JPAODataDatabaseProcessor getDatabaseAccessor();

	/**
	 * Called at end of lifecycle of adapter to release any allocated resources
	 * (close {@link javax.persistence.EntityManagerFactory#close()
	 * EntityManagerFactory} etc.).
	 */
	public void dispose();
}
