package org.apache.olingo.jpa.security;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import org.apache.olingo.commons.api.http.HttpMethod;

/**
 * Define the security constraints for a specific HTTP method as part of the
 * normal OData/REST CRUD operations.
 *
 * @author Ralf Zozmann
 *
 */
@Retention(RUNTIME)
@Documented
public @interface AccessDefinition {

	HttpMethod method();// nullable, so no default must be set

	/**
	 * An empty array is interpreted as 'permit to all' users (depending to
	 * {@link #authenticationRequired()} only for logged in users or also for
	 * anonymous users).<br/>
	 * An non empty array forces {@link #authenticationRequired() authentication}
	 * and allow access only to users having at least one the listed roles.
	 *
	 * @return The names of the authorized roles.
	 */
	String[] rolesAllowed() default {};

	/**
	 *
	 * @return TRUE to enforce an existing user principal to access entity.
	 */
	boolean authenticationRequired() default true;
}
