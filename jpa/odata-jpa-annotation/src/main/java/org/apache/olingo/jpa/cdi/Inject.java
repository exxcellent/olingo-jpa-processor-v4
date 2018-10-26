package org.apache.olingo.jpa.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used as complete replacement (in context of
 * olingo-jpa-processor) for <i>javax.inject.Inject</i> with additional support
 * for method parameter injection.<br/>
 * Injected fields or parameters are pure server side. So injected method
 * parameters of an action are hidden to OData meta data.
 *
 * @author Ralf Zozmann
 *
 */
@Target({ PARAMETER, FIELD })
@Retention(RUNTIME)
@Documented
public @interface Inject {
	// marker annotation
}
