package org.apache.olingo.jpa.security;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to limit the access to an OData entity (JPA entity
 * with annotation {@link javax.persistence.Entity @Entity}) in combination with
 * {@link org.apache.olingo.jpa.processor.core.security.AnnotationBasedSecurityInceptor
 * AnnotationBasedSecurityInceptor}. Default is: reject access to the secured
 * entity for all (unconfigured)
 * {@link org.apache.olingo.commons.api.http.HttpMethod HTTP methods} and
 * users/roles. All HTTP methods that should be allowed must be configured or
 * access will be rejected.
 *
 * @author Ralf Zozmann
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
@Documented
public @interface ODataEntityAccess {

	AccessDefinition[] value() default {};

}
