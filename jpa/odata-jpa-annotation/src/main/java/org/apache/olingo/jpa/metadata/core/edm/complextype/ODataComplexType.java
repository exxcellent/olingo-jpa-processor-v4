package org.apache.olingo.jpa.metadata.core.edm.complextype;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.apache.olingo.jpa.metadata.core.edm.NamingStrategy;

/**
 * Optional annotation to configure an {@link javax.persistence.Embeddable @Embeddable} class in a more specific way.
 *
 * @author Ralf Zozmann
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ODataComplexType {


  /**
   * @see NamingStrategy#UpperCamelCase
   */
  NamingStrategy attributeNaming() default NamingStrategy.UpperCamelCase;
}
