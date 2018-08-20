package org.apache.olingo.jpa.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used as complete replacement (in context of
 * olingo-jpa-processor) for {@link javax.inject.Inject} with additional support
 * for method parameter injection.
 *
 * @author Ralf Zozmann
 *
 */
@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
@Documented
public @interface Inject {
}
