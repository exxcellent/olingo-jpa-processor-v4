package org.apache.olingo.jpa.metadata.core.edm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation to configure an action result manually.
 *
 * @author Ralf Zozmann
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface EdmActionResult {

  /**
   * Value for floating point results like {@link java.math.BigDecimal BigDecimal} or
   * {@link java.lang.Double Double} or {@link java.lang.Float Float} to define the maximum number of digits.
   *
   * @see java.math.BigDecimal#precision()
   * @see org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal
   */
  int precision();

  /**
   * Value for floating point results like {@link java.math.BigDecimal BigDecimal} or
   * {@link java.lang.Double Double} or {@link java.lang.Float Float} to define the maximum decimal places.
   *
   * @see java.math.BigDecimal#scale()
   * @see org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal
   */
  int scale();
}
