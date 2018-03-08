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
@Target({ ElementType.PARAMETER, ElementType.TYPE_PARAMETER, ElementType.ANNOTATION_TYPE })
public @interface EdmActionParameter {

  /**
   * Most Java compilers do not transfer the parameter name from source code into byte code,
   * so we need a fallback to get a valid name at runtime.
   * 
   * @return The name of the parameter on OData.
   */
  String name();
}
