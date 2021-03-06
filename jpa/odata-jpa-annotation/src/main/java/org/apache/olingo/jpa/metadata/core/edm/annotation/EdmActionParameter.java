package org.apache.olingo.jpa.metadata.core.edm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation to configure an action (method) parameter manually.
 *
 * @author Ralf Zozmann
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface EdmActionParameter {

  /**
   * Most Java compilers do not transfer the parameter name from source code into byte code,
   * so we need a fallback to get a valid name at runtime.
   *
   * @return The name of the parameter on OData.
   */
  String name();

  /**
   * Optional value for floating point parameters like {@link java.math.BigDecimal BigDecimal} or
   * {@link java.lang.Double Double} or {@link java.lang.Float Float} to define the maximum number of digits.
   *
   * @see java.math.BigDecimal#precision()
   * @see org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal
   */
  int precision() default -1;

  /**
   * Optional value for floating point parameters like {@link java.math.BigDecimal BigDecimal} or
   * {@link java.lang.Double Double} or {@link java.lang.Float Float} to define the maximum decimal places.
   *
   * @see java.math.BigDecimal#scale()
   * @see org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal
   */
  int scale() default -1;
}
