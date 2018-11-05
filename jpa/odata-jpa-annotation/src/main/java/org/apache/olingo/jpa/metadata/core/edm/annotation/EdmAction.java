package org.apache.olingo.jpa.metadata.core.edm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata of a action, see <a href =
 * "http://docs.oasis-open.org/odata/odata/v4.0/errata02/os/complete/part3-csdl/odata-v4.0-errata02-os-part3-csdl-complete.html#_Toc406398005">
 * edm:Action.</a><br/>
 * By default bound actions are treated as Entity methods, whereas unbound actions are called without context.
 * 
 * @author Ralf Zozmann
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EdmAction {

	/**
	 * Defines the name of the action in the service document
	 */
	String name() default "";
}
