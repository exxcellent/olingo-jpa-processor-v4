package org.apache.olingo.jpa.metadata.core.edm.entity;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.apache.olingo.server.api.ODataApplicationException;

/**
 * Optional annotation to configure an entity class in a more specific way. This annotation <b>cannot</b> be used for
 * {@link org.apache.olingo.jpa.metadata.core.edm.dto.ODataDTO DTO}'s.
 *
 * @author Ralf Zozmann
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ODataEntity {

	/**
	 * Dummy used as default/marker to make handler optional.
	 *
	 */
	static final class DEFAULT implements DataAccessConditioner<Object> {

		@Override
		public Expression<Boolean> buildSelectCondition(final EntityManager em, final Root<Object> from) throws ODataApplicationException {
			throw new UnsupportedOperationException();
		}
	};

	/**
	 *
	 * @return The class implementing the logic to handle custom logic for JPA query generation.
	 */
	Class<? extends DataAccessConditioner<?>> handlerDataAccessConditioner() default DEFAULT.class;
}
