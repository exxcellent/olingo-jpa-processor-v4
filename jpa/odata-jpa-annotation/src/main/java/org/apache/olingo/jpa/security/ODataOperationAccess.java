package org.apache.olingo.jpa.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to limit the access to an OData action in combination
 * with
 * {@link org.apache.olingo.jpa.processor.core.security.AnnotationBasedSecurityInceptor
 * AnnotationBasedSecurityInceptor}. Default is: access for every authenticated
 * user.
 *
 * @author Ralf Zozmann
 *
 */
@Target({ METHOD })
@Retention(RUNTIME)
@Documented
public @interface ODataOperationAccess {

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
	 * @return TRUE to enforce an existing user principal to access operation.
	 */
	boolean authenticationRequired() default true;
}
